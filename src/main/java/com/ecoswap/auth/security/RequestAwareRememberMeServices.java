package com.ecoswap.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

/**
 * Login is a JSON request, so the usual "remember-me" form parameter does not exist.
 * The login service sets a request attribute instead, which this class reads.
 */
public class RequestAwareRememberMeServices extends TokenBasedRememberMeServices {

    public static final String REMEMBER_ME_ATTRIBUTE = RequestAwareRememberMeServices.class.getName() + ".REQUESTED";

    public RequestAwareRememberMeServices(String key, UserDetailsService userDetailsService) {
        super(key, userDetailsService);
    }

    @Override
    protected boolean rememberMeRequested(HttpServletRequest request, String parameter) {
        return Boolean.TRUE.equals(request.getAttribute(REMEMBER_ME_ATTRIBUTE));
    }
}
