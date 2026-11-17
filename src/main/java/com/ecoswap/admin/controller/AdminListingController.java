package com.ecoswap.admin.controller;

import com.ecoswap.admin.dto.AdminItemView;
import com.ecoswap.admin.dto.ReasonRequest;
import com.ecoswap.admin.service.AdminListingService;
import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.item.entity.ItemAvailabilityStatus;
import com.ecoswap.item.entity.ListingApprovalStatus;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/items")
@PreAuthorize("hasRole('ADMIN')")
public class AdminListingController {

    private final AdminListingService service;

    public AdminListingController(AdminListingService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AdminItemView> all(@RequestParam(required = false) ListingApprovalStatus approval,
                                           @RequestParam(required = false) ItemAvailabilityStatus availability,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.search(approval, availability, q, page, size);
    }

    @GetMapping("/pending")
    public PageResponse<AdminItemView> pending(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.search(ListingApprovalStatus.PENDING_APPROVAL, null, null, page, size);
    }

    @GetMapping("/{id}")
    public AdminItemView detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PatchMapping("/{id}/approve")
    public AdminItemView approve(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        return service.approve(admin.getId(), id);
    }

    @PatchMapping("/{id}/reject")
    public AdminItemView reject(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id, @Valid @RequestBody ReasonRequest body) {
        return service.reject(admin.getId(), id, body.reason());
    }

    @PatchMapping("/{id}/request-changes")
    public AdminItemView requestChanges(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id, @Valid @RequestBody ReasonRequest body) {
        return service.requestChanges(admin.getId(), id, body.reason());
    }

    @PatchMapping("/{id}/suspend")
    public AdminItemView suspend(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id, @Valid @RequestBody ReasonRequest body) {
        return service.suspend(admin.getId(), id, body.reason());
    }

    @PatchMapping("/{id}/archive")
    public AdminItemView archive(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id, @Valid @RequestBody ReasonRequest body) {
        return service.archive(admin.getId(), id, body.reason());
    }
}
