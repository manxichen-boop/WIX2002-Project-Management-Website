package com.ecoswap.auth.security;

import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.entity.UserRole;
import java.io.Serial;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Security principal stored in the session. It holds only identifiers and the role;
 * the account status is re-checked on every request by {@link AccountStatusFilter}.
 */
public final class AuthenticatedUser implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final AccountStatus status;

    private AuthenticatedUser(Long id, String email, String passwordHash, UserRole role, AccountStatus status) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole(), user.getAccountStatus());
    }

    public Long getId() { return id; }
    public UserRole getRole() { return role; }
    public boolean isAdmin() { return role == UserRole.ADMIN; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }

    /** Only ACTIVE accounts can be authenticated (also applies to remember-me logins). */
    @Override public boolean isEnabled() { return status == AccountStatus.ACTIVE; }
}
