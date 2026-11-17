package com.ecoswap.config;

import com.ecoswap.auth.security.AccountStatusFilter;
import com.ecoswap.auth.security.AppUserDetailsService;
import com.ecoswap.auth.security.RequestAwareRememberMeServices;
import com.ecoswap.auth.security.SecurityErrorHandlers;
import com.ecoswap.common.util.TokenGenerator;
import com.ecoswap.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Two roles: USER (students) and ADMIN. URL rules below are the first layer; admin services
 * also carry @PreAuthorize and every service checks ownership, so the menu is never the only guard.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data: blob: https://tile.openstreetmap.org; font-src 'self'; "
            + "connect-src 'self' https://nominatim.openstreetmap.org; object-src 'none'; "
            + "base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    RequestAwareRememberMeServices rememberMeServices(AppUserDetailsService userDetailsService, AppProperties props) {
        String key = props.auth().rememberMeKey();
        if (key == null || key.isBlank()) {
            key = TokenGenerator.newToken();
            log.warn("ECOSWAP_REMEMBER_ME_KEY is not set: using a random key, so 'remember me' logins end when the application restarts.");
        }
        RequestAwareRememberMeServices services = new RequestAwareRememberMeServices(key, userDetailsService);
        services.setCookieName("ECOSWAP_REMEMBER");
        services.setTokenValiditySeconds(props.auth().rememberMeDays() * 24 * 3600);
        return services;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository users,
                                            RequestAwareRememberMeServices rememberMeServices,
                                            SecurityContextRepository contextRepository) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // static assets and error pages
                .requestMatchers("/css/**", "/js/**", "/img/**", "/fonts/**", "/vendor/**", "/favicon.ico", "/error").permitAll()
                // public pages
                .requestMatchers(HttpMethod.GET, "/", "/explore", "/pickup-map", "/items/{id:\\d+}", "/how-it-works", "/community", "/impact").permitAll()
                .requestMatchers(HttpMethod.GET, "/login", "/register", "/register/pending", "/forgot-password",
                        "/reset-password", "/verify-email").permitAll()
                // public API
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/forgot-password",
                        "/api/auth/reset-password", "/api/auth/resend-verification").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/verify-email", "/api/items", "/api/items/map", "/api/items/{id:\\d+}",
                        "/api/categories", "/api/impact/summary", "/api/community/activity").permitAll()
                // item photos: the media controller decides per image whether the caller may see it
                .requestMatchers(HttpMethod.GET, "/media/items/**").permitAll()
                // administrator area
                .requestMatchers("/admin", "/admin/**", "/api/admin/**").hasRole("ADMIN")
                // shared by both roles
                .requestMatchers("/notifications", "/profile", "/api/auth/me", "/api/notifications/**",
                        "/api/profile/**", "/media/**").authenticated()
                // everything else is the student application
                .anyRequest().hasRole("USER"))
            .securityContext(ctx -> ctx.securityContextRepository(contextRepository))
            .formLogin(form -> form.disable())     // login is POST /api/auth/login (JSON)
            .httpBasic(basic -> basic.disable())
            .rememberMe(rm -> rm.rememberMeServices(rememberMeServices))
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                .deleteCookies("ECOSWAP_SESSION", "ECOSWAP_REMEMBER")
                .invalidateHttpSession(true))
            .sessionManagement(session -> session.sessionFixation(fix -> fix.changeSessionId()))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(SecurityErrorHandlers.entryPoint())
                .accessDeniedHandler(SecurityErrorHandlers.accessDenied()))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .frameOptions(frame -> frame.deny()))
            .addFilterAfter(new AccountStatusFilter(users, rememberMeServices), RememberMeAuthenticationFilter.class);
        return http.build();
    }
}
