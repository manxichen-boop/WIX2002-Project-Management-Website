package com.ecoswap.web;

import com.ecoswap.user.entity.User;
import java.util.Arrays;
import java.util.stream.Collectors;

/** What templates need to know about the signed-in user (never the entity itself). */
public record CurrentUserView(Long id, String fullName, String email, String university, boolean admin, String avatarUrl) {

    public static CurrentUserView of(User user) {
        String avatar = user.getProfileImagePath() == null ? null
                : "/media/profiles/" + user.getId() + "?v=" + user.getUpdatedAt().hashCode();
        return new CurrentUserView(user.getId(), user.getFullName(), user.getEmail(), user.getUniversity(), user.isAdmin(), avatar);
    }

    public String firstName() {
        return fullName.strip().split("\\s+")[0];
    }

    public String initials() {
        return Arrays.stream(fullName.strip().split("\\s+")).limit(2)
                .map(p -> p.substring(0, 1).toUpperCase()).collect(Collectors.joining());
    }
}
