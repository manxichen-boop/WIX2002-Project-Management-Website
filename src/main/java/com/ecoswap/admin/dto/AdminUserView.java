package com.ecoswap.admin.dto;

import com.ecoswap.common.util.Times;
import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.User;
import java.time.Instant;

public record AdminUserView(Long id, String fullName, String email, String university, AccountStatus accountStatus,
                            String statusReason, boolean emailVerified, Instant createdAt) {

    public static AdminUserView of(User u) {
        return new AdminUserView(u.getId(), u.getFullName(), u.getEmail(), u.getUniversity(), u.getAccountStatus(),
                u.getStatusReason(), u.isEmailVerified(), Times.utc(u.getCreatedAt()));
    }
}
