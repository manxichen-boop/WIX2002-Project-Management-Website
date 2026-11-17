package com.ecoswap.auth.security;

import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Re-checks the account status of the signed-in user on every request, so a suspension
 * takes effect immediately rather than when the session happens to expire.
 */
public class AccountStatusFilter extends OncePerRequestFilter {

    private final UserRepository users;
    private final RememberMeServices rememberMeServices;

    public AccountStatusFilter(UserRepository users, RememberMeServices rememberMeServices) {
        this.users = users;
        this.rememberMeServices = rememberMeServices;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/img/") || path.startsWith("/fonts/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser principal) {
            AccountStatus status = users.findById(principal.getId()).map(u -> u.getAccountStatus()).orElse(null);
            if (status != AccountStatus.ACTIVE) {
                signOut(request, response, auth);
                if (request.getRequestURI().startsWith("/api/")) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":401,\"code\":\"ACCOUNT_INACTIVE\",\"message\":\"Your account is no longer active.\"}");
                } else {
                    response.sendRedirect(request.getContextPath() + "/login?inactive");
                }
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void signOut(HttpServletRequest request, HttpServletResponse response, Authentication auth) {
        if (rememberMeServices instanceof LogoutHandler handler) {
            handler.logout(request, response, auth);
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
