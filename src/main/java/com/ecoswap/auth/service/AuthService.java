package com.ecoswap.auth.service;

import com.ecoswap.auth.dto.ForgotPasswordRequest;
import com.ecoswap.auth.dto.LoginRequest;
import com.ecoswap.auth.dto.RegisterRequest;
import com.ecoswap.auth.dto.RegisterResponse;
import com.ecoswap.auth.dto.ResetPasswordRequest;
import com.ecoswap.auth.entity.EmailVerificationToken;
import com.ecoswap.auth.entity.PasswordResetToken;
import com.ecoswap.auth.repository.EmailVerificationTokenRepository;
import com.ecoswap.auth.repository.PasswordResetTokenRepository;
import com.ecoswap.common.exception.ApiException;
import com.ecoswap.common.exception.InvalidInputException;
import com.ecoswap.common.util.Texts;
import com.ecoswap.common.util.TokenGenerator;
import com.ecoswap.config.AppProperties;
import com.ecoswap.notification.NotificationService;
import com.ecoswap.notification.NotificationType;
import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, credential checks, e-mail verification and password reset.
 * HTTP/session concerns (creating the security context, remember-me) stay in AuthController.
 */
@Service
public class AuthService {

    static final Duration VERIFICATION_VALIDITY = Duration.ofHours(48);
    static final Duration RESET_VALIDITY = Duration.ofMinutes(30);

    /** Verification result shown on the /verify-email page. */
    public enum VerificationResult { VERIFIED, ALREADY_VERIFIED, EXPIRED, INVALID }

    private final UserRepository users;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder encoder;
    private final MailService mail;
    private final NotificationService notifications;
    private final LoginAttemptService attempts;
    private final AppProperties props;
    /** Compared against when the e-mail is unknown, so response time does not reveal which e-mails exist. */
    private final String dummyHash;

    public AuthService(UserRepository users, EmailVerificationTokenRepository verificationTokens,
                       PasswordResetTokenRepository resetTokens, PasswordEncoder encoder, MailService mail,
                       NotificationService notifications, LoginAttemptService attempts, AppProperties props) {
        this.users = users;
        this.verificationTokens = verificationTokens;
        this.resetTokens = resetTokens;
        this.encoder = encoder;
        this.mail = mail;
        this.notifications = notifications;
        this.attempts = attempts;
        this.props = props;
        this.dummyHash = encoder.encode(TokenGenerator.newToken());
    }

    // ---------------------------------------------------------------- registration

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        String email = Texts.normaliseEmail(req.email());
        if (!req.password().equals(req.confirmPassword())) {
            throw new InvalidInputException("confirmPassword", "Passwords do not match.");
        }
        List<String> domains = props.auth().allowedDomains();
        String domain = email.substring(email.indexOf('@') + 1);
        if (!domains.isEmpty() && domains.stream().noneMatch(d -> domain.equals(d) || domain.endsWith("." + d))) {
            throw new InvalidInputException("email", "Please use your university e-mail (" + String.join(", ", domains) + ").");
        }
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "An account with this e-mail already exists.");
        }

        // Public registration can only create USER accounts awaiting approval.
        User user = users.save(User.newStudent(req.fullName().strip(), email, encoder.encode(req.password()), req.university().strip()));
        boolean sent = sendVerificationEmail(user);

        notifications.notifyAdmins(NotificationType.REGISTRATION_SUBMITTED, "New registration to review",
                user.getFullName() + " (" + user.getUniversity() + ") has registered and is waiting for approval.",
                "/admin/pending-users");
        return new RegisterResponse("Thanks for registering! An administrator will review your account shortly.", email, sent);
    }

    private boolean sendVerificationEmail(User user) {
        verificationTokens.invalidateAllForUser(user.getId());
        String token = TokenGenerator.newToken();
        verificationTokens.save(new EmailVerificationToken(user, TokenGenerator.sha256(token),
                LocalDateTime.now().plus(VERIFICATION_VALIDITY)));
        String link = mail.link("/verify-email?token=" + token);
        return mail.send(user.getEmail(), "Confirm your Eco Swape e-mail",
                "Hi " + user.getFullName() + ",\n\nThanks for joining Eco Swape. Please confirm your e-mail address:\n\n"
                        + link + "\n\nThis link is valid for 48 hours. Your account will be reviewed by an administrator; "
                        + "we'll let you know as soon as it is approved.\n\nEco Swape");
    }

    @Transactional
    public VerificationResult verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 100) {
            return VerificationResult.INVALID;
        }
        return verificationTokens.findByTokenHash(TokenGenerator.sha256(rawToken)).map(t -> {
            if (t.getUser().isEmailVerified()) {
                t.markUsed();
                return VerificationResult.ALREADY_VERIFIED;
            }
            if (!t.isUsable()) {
                return t.isUsed() ? VerificationResult.INVALID : VerificationResult.EXPIRED;
            }
            t.markUsed();
            t.getUser().markEmailVerified();
            return VerificationResult.VERIFIED;
        }).orElse(VerificationResult.INVALID);
    }

    /** Re-sends the verification link; silent for unknown or already verified e-mails. */
    @Transactional
    public void resendVerification(String rawEmail) {
        String email = Texts.normaliseEmail(rawEmail);
        String key = "verify:" + email;
        if (attempts.isBlocked(key)) {
            return;
        }
        attempts.recordFailure(key);
        users.findByEmail(email).filter(u -> !u.isEmailVerified() && u.getAccountStatus() != AccountStatus.REJECTED)
                .ifPresent(this::sendVerificationEmail);
    }

    // ---------------------------------------------------------------- login

    /**
     * Verifies credentials first and only then reveals the account status, so someone guessing
     * passwords learns nothing about an account. Returns the user when they may sign in.
     */
    @Transactional(readOnly = true)
    public User authenticate(LoginRequest req) {
        String email = Texts.normaliseEmail(req.email());
        String key = "login:" + email;
        if (attempts.isBlocked(key)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS",
                    "Too many failed attempts. Please wait " + attempts.minutesLocked() + " minutes and try again.");
        }
        User user = users.findByEmail(email).orElse(null);
        boolean matches = encoder.matches(req.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !matches) {
            attempts.recordFailure(key);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "The e-mail or password is incorrect.");
        }
        attempts.reset(key);

        switch (user.getAccountStatus()) {
            case PENDING_APPROVAL -> throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_PENDING",
                    "Your registration is still waiting for administrator approval.");
            case REJECTED -> throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_REJECTED",
                    "Your registration was not approved." + reasonSuffix(user));
            case SUSPENDED -> throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED",
                    "Your account is suspended." + reasonSuffix(user) + " Contact the Eco Swape team if you think this is a mistake.");
            case ACTIVE -> { /* allowed */ }
        }
        if (props.auth().requireEmailVerification() && !user.isEmailVerified() && !user.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
                    "Please confirm your e-mail address using the link we sent you.");
        }
        return user;
    }

    private static String reasonSuffix(User user) {
        return user.getStatusReason() == null ? "" : " Reason: " + user.getStatusReason();
    }

    // ---------------------------------------------------------------- password reset

    /** Always succeeds from the caller's point of view, so it cannot be used to discover accounts. */
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest req) {
        String email = Texts.normaliseEmail(req.email());
        String key = "reset:" + email;
        if (attempts.isBlocked(key)) {
            return;
        }
        attempts.recordFailure(key);   // counts requests, limiting reset e-mails per address
        users.findByEmail(email).filter(u -> u.getAccountStatus() != AccountStatus.REJECTED).ifPresent(user -> {
            resetTokens.invalidateAllForUser(user.getId());
            String token = TokenGenerator.newToken();
            resetTokens.save(new PasswordResetToken(user, TokenGenerator.sha256(token), LocalDateTime.now().plus(RESET_VALIDITY)));
            mail.send(user.getEmail(), "Reset your Eco Swape password",
                    "Hi " + user.getFullName() + ",\n\nWe received a request to reset your password. Choose a new one here:\n\n"
                            + mail.link("/reset-password?token=" + token)
                            + "\n\nThe link is valid for 30 minutes. If you didn't ask for this, you can ignore this e-mail.\n\nEco Swape");
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (!req.password().equals(req.confirmPassword())) {
            throw new InvalidInputException("confirmPassword", "Passwords do not match.");
        }
        PasswordResetToken token = resetTokens.findByTokenHash(TokenGenerator.sha256(req.token()))
                .filter(PasswordResetToken::isUsable)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "TOKEN_INVALID",
                        "This reset link is invalid or has expired. Please request a new one."));
        token.markUsed();
        User user = token.getUser();
        // Changing the hash also invalidates any remember-me cookies (they embed the hash).
        user.changePassword(encoder.encode(req.password()));
        resetTokens.invalidateAllForUser(user.getId());
        attempts.reset("login:" + user.getEmail());
    }
}
