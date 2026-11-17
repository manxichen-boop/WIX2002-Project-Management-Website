package com.ecoswap.support;

import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.common.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/support/tickets")
public class SupportController {

    private final SupportService service;

    public SupportController(SupportService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupportTicketView create(@AuthenticationPrincipal AuthenticatedUser me, @Valid @ModelAttribute CreateTicketForm form,
                                    @RequestParam(name = "attachment", required = false) MultipartFile attachment) {
        return service.create(me.getId(), form, attachment);
    }

    @GetMapping("/mine")
    public PageResponse<SupportTicketView> mine(@AuthenticationPrincipal AuthenticatedUser me,
                                                @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.mine(me.getId(), page, size);
    }

    @GetMapping("/{id}")
    public SupportTicketView detail(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return service.detail(me.getId(), id);
    }

    @PostMapping("/{id}/reply")
    public SupportTicketView reply(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id, @Valid @RequestBody ReplyBody body) {
        return service.reply(me.getId(), id, body.message());
    }
}
