package com.ecoswap.admin.controller;

import com.ecoswap.admin.dto.AdminUserView;
import com.ecoswap.admin.dto.ReasonRequest;
import com.ecoswap.admin.service.AdminUserService;
import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.user.entity.AccountStatus;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AdminUserView> all(@RequestParam(required = false) AccountStatus status,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.search(status, q, page, size);
    }

    @GetMapping("/pending")
    public PageResponse<AdminUserView> pending(@RequestParam(required = false) String q,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return service.search(AccountStatus.PENDING_APPROVAL, q, page, size);
    }

    @PatchMapping("/{id}/approve")
    public AdminUserView approve(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        return service.approve(admin.getId(), id);
    }

    @PatchMapping("/{id}/reject")
    public AdminUserView reject(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id, @Valid @RequestBody ReasonRequest body) {
        return service.reject(admin.getId(), id, body.reason());
    }

    @PatchMapping("/{id}/suspend")
    public AdminUserView suspend(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id, @Valid @RequestBody ReasonRequest body) {
        return service.suspend(admin.getId(), id, body.reason());
    }

    @PatchMapping("/{id}/activate")
    public AdminUserView activate(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        return service.activate(admin.getId(), id);
    }
}
