package com.ecoswap.auth.dto;

import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.entity.UserRole;

public record MeResponse(Long id, String fullName, String email, String university, UserRole role,
                         AccountStatus accountStatus, boolean emailVerified, String avatarUrl) {

    public static MeResponse of(User u) {
        String avatar = u.getProfileImagePath() == null ? null : "/media/profiles/" + u.getId();
        return new MeResponse(u.getId(), u.getFullName(), u.getEmail(), u.getUniversity(), u.getRole(),
                u.getAccountStatus(), u.isEmailVerified(), avatar);
    }
}
