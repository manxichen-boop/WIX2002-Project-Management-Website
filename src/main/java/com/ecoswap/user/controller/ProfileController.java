package com.ecoswap.user.controller;

import com.ecoswap.auth.dto.MeResponse;
import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.common.response.MessageResponse;
import com.ecoswap.user.dto.PasswordChangeRequest;
import com.ecoswap.user.dto.ProfileUpdateRequest;
import com.ecoswap.user.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping
    public MeResponse get(@AuthenticationPrincipal AuthenticatedUser me) {
        return service.get(me.getId());
    }

    @PutMapping
    public MeResponse update(@AuthenticationPrincipal AuthenticatedUser me, @Valid @RequestBody ProfileUpdateRequest body) {
        return service.update(me.getId(), body);
    }

    @PostMapping("/photo")
    public MeResponse photo(@AuthenticationPrincipal AuthenticatedUser me, @RequestParam("photo") MultipartFile photo) {
        return service.changePhoto(me.getId(), photo);
    }

    @DeleteMapping("/photo")
    public MeResponse removePhoto(@AuthenticationPrincipal AuthenticatedUser me) {
        return service.removePhoto(me.getId());
    }

    @PatchMapping("/password")
    public MessageResponse password(@AuthenticationPrincipal AuthenticatedUser me, @Valid @RequestBody PasswordChangeRequest body) {
        service.changePassword(me.getId(), body);
        return new MessageResponse("Password changed.");
    }
}
