package com.ecoswap.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** API calls get JSON errors; page requests get a redirect (login) or the 403 page. */
public final class SecurityErrorHandlers {

    private SecurityErrorHandlers() {}

    static boolean isApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    static void writeJson(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }

    public static AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) -> {
            if (isApi(request)) {
                writeJson(response, 401, "UNAUTHENTICATED", "Please log in to continue.");
                return;
            }
            String target = request.getRequestURI().substring(request.getContextPath().length());
            if (request.getQueryString() != null) {
                target += "?" + request.getQueryString();
            }
            response.sendRedirect(request.getContextPath() + "/login?next=" + URLEncoder.encode(target, StandardCharsets.UTF_8));
        };
    }

    public static AccessDeniedHandler accessDenied() {
        return (request, response, ex) -> {
            if (isApi(request)) {
                // A missing/expired CSRF token is the usual cause for an otherwise valid user.
                boolean csrf = ex instanceof org.springframework.security.web.csrf.CsrfException;
                writeJson(response, 403, csrf ? "CSRF_INVALID" : "FORBIDDEN",
                        csrf ? "Your session has expired. Please refresh the page and try again."
                             : "You do not have permission to do that.");
                return;
            }
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String path = request.getRequestURI().substring(request.getContextPath().length());
            // Admins who open a student page are sent to their own dashboard instead of an error.
            if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser u && u.isAdmin() && !path.startsWith("/admin")) {
                response.sendRedirect(request.getContextPath() + "/admin");
                return;
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        };
    }
}
