package com.ecoswap.admin.controller;

import com.ecoswap.admin.service.AdminSupportService;
import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.support.ReplyBody;
import com.ecoswap.support.SupportTicketStatus;
import com.ecoswap.support.SupportTicketView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/support/tickets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportController {

    public record StatusBody(@NotNull(message = "Choose a status.") SupportTicketStatus status) {}

    private final AdminSupportService service;

    public AdminSupportController(AdminSupportService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<SupportTicketView> search(@RequestParam(required = false) SupportTicketStatus status,
                                                  @RequestParam(required = false) String q,
                                                  @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.search(status, q, page, size);
    }

    @GetMapping("/{id}")
    public SupportTicketView detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping("/{id}/reply")
    public SupportTicketView reply(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id, @Valid @RequestBody ReplyBody body) {
        return service.reply(admin.getId(), id, body.message());
    }

    @PatchMapping("/{id}/status")
    public SupportTicketView status(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id, @Valid @RequestBody StatusBody body) {
        return service.changeStatus(admin.getId(), id, body.status());
    }
}
