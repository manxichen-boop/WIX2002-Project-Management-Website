package com.ecoswap.auth.controller;

import com.ecoswap.auth.dto.*;
import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.auth.security.RequestAwareRememberMeServices;
import com.ecoswap.auth.service.AuthService;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.response.MessageResponse;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

/** Authentication API. Logout (POST /api/auth/logout) is handled by Spring Security's logout filter. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;
    private final SecurityContextRepository contextRepository;
    private final RequestAwareRememberMeServices rememberMe;
    private final CsrfTokenRepository csrfTokens = new HttpSessionCsrfTokenRepository();

    public AuthController(AuthService authService, UserRepository users, SecurityContextRepository contextRepository,
                          RequestAwareRememberMeServices rememberMe) {
        this.authService = authService;
        this.users = users;
        this.contextRepository = contextRepository;
        this.rememberMe = rememberMe;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        User user = authService.authenticate(body);
        AuthenticatedUser principal = AuthenticatedUser.from(user);
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());

        // New session id on login (prevents session fixation) and a fresh CSRF token.
        request.getSession(true);
        request.changeSessionId();
        csrfTokens.saveToken(null, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);

        if (body.rememberMe()) {
            request.setAttribute(RequestAwareRememberMeServices.REMEMBER_ME_ATTRIBUTE, Boolean.TRUE);
            rememberMe.loginSuccess(request, response, auth);
        }
        return new LoginResponse(user.isAdmin() ? "/admin" : "/dashboard", MeResponse.of(user));
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser me) {
        return users.findById(me.getId()).map(MeResponse::of).orElseThrow(() -> new NotFoundException("Account"));
    }

    @GetMapping("/verify-email")
    public Map<String, String> verifyEmail(@RequestParam String token) {
        AuthService.VerificationResult result = authService.verifyEmail(token);
        return Map.of("result", result.name());
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@Valid @RequestBody ForgotPasswordRequest body) {
        authService.resendVerification(body.email());
        return new MessageResponse("If that account still needs confirming, a new link is on its way.");
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest body) {
        authService.requestPasswordReset(body);
        return new MessageResponse("If an account exists for that e-mail, a reset link is on its way. It is valid for 30 minutes.");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        authService.resetPassword(body);
        return new MessageResponse("Your password has been changed. You can now log in.");
    }
}
