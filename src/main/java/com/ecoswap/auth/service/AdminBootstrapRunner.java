package com.ecoswap.auth.service;

import com.ecoswap.common.util.Texts;
import com.ecoswap.common.validation.StrongPasswordValidator;
import com.ecoswap.config.AppProperties;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.entity.UserRole;
import com.ecoswap.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secure first-admin setup. There is no admin registration page: the first administrator is
 * created from ECOSWAP_ADMIN_EMAIL / ECOSWAP_ADMIN_PASSWORD, once, if that e-mail is unused.
 * Existing accounts are never modified or promoted by this runner.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public AdminBootstrapRunner(UserRepository users, PasswordEncoder encoder, AppProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = Texts.normaliseEmail(Texts.clean(props.admin().bootstrapEmail()));
        String password = props.admin().bootstrapPassword();
        if (email == null || password == null || password.isBlank()) {
            if (users.countByRole(UserRole.ADMIN) == 0) {
                log.warn("No administrator exists. Set ECOSWAP_ADMIN_EMAIL and ECOSWAP_ADMIN_PASSWORD and restart to create one.");
            }
            return;
        }
        var existing = users.findByEmail(email);
        if (existing.isPresent()) {
            if (!existing.get().isAdmin()) {
                log.warn("Admin bootstrap skipped: {} already belongs to a student account and will not be promoted.", email);
            }
            return;
        }
        if (!StrongPasswordValidator.isStrong(password)) {
            log.error("Admin bootstrap skipped: ECOSWAP_ADMIN_PASSWORD must be 8–72 characters with a letter and a number.");
            return;
        }
        String name = Texts.clean(props.admin().bootstrapName());
        users.save(User.newAdministrator(name == null ? "EcoSwape Administrator" : name, email, encoder.encode(password)));
        log.info("Created the initial administrator account {}. You can now remove ECOSWAP_ADMIN_PASSWORD from the environment.", email);
    }
}
