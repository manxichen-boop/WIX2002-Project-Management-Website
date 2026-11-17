package com.ecoswap.user.service;

import com.ecoswap.auth.dto.MeResponse;
import com.ecoswap.common.exception.InvalidInputException;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.storage.FileCleanup;
import com.ecoswap.storage.StorageFolder;
import com.ecoswap.storage.StorageService;
import com.ecoswap.storage.UploadValidator;
import com.ecoswap.user.dto.PasswordChangeRequest;
import com.ecoswap.user.dto.ProfileUpdateRequest;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** The signed-in user's own profile: name, university, photo and password. E-mail and role are not editable. */
@Service
public class ProfileService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final UploadValidator uploads;
    private final StorageService storage;
    private final FileCleanup cleanup;

    public ProfileService(UserRepository users, PasswordEncoder encoder, UploadValidator uploads, StorageService storage, FileCleanup cleanup) {
        this.users = users;
        this.encoder = encoder;
        this.uploads = uploads;
        this.storage = storage;
        this.cleanup = cleanup;
    }

    @Transactional(readOnly = true)
    public MeResponse get(Long userId) {
        return MeResponse.of(user(userId));
    }

    @Transactional
    public MeResponse update(Long userId, ProfileUpdateRequest req) {
        User user = user(userId);
        user.rename(req.fullName().strip());
        if (!user.isAdmin()) {
            user.changeUniversity(req.university().strip());
        }
        return MeResponse.of(user);
    }

    @Transactional
    public MeResponse changePhoto(Long userId, MultipartFile file) {
        User user = user(userId);
        var upload = uploads.image(file, "photo");
        var stored = storage.store(StorageFolder.PROFILES, upload.content(), upload.type(), upload.originalFilename());
        List<String> newKey = new ArrayList<>(List.of(stored.key()));
        cleanup.deleteOnRollback(newKey);
        if (user.getProfileImagePath() != null) {
            cleanup.deleteAfterCommit(List.of(user.getProfileImagePath()));
        }
        user.changeProfileImage(stored.key());
        return MeResponse.of(user);
    }

    @Transactional
    public MeResponse removePhoto(Long userId) {
        User user = user(userId);
        if (user.getProfileImagePath() != null) {
            cleanup.deleteAfterCommit(List.of(user.getProfileImagePath()));
            user.changeProfileImage(null);
        }
        return MeResponse.of(user);
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest req) {
        User user = user(userId);
        if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new InvalidInputException("currentPassword", "Your current password is incorrect.");
        }
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new InvalidInputException("confirmPassword", "Passwords do not match.");
        }
        if (encoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new InvalidInputException("newPassword", "Choose a password you haven't used here before.");
        }
        user.changePassword(encoder.encode(req.newPassword()));   // also invalidates remember-me cookies
    }

    private User user(Long userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("Account"));
    }
}
