package com.ecoswap.exchange.controller;

import com.ecoswap.admin.dto.ReasonRequest;
import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.exchange.dto.CreateRequestBody;
import com.ecoswap.exchange.dto.ExchangeRequestView;
import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import com.ecoswap.exchange.service.ExchangeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ExchangeController {

    private final ExchangeService service;

    public ExchangeController(ExchangeService service) {
        this.service = service;
    }

    @PostMapping("/items/{itemId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeRequestView create(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long itemId,
                                      @Valid @RequestBody CreateRequestBody body) {
        return service.create(me.getId(), itemId, body.offeredItemId(), body.message());
    }

    @GetMapping("/requests/sent")
    public PageResponse<ExchangeRequestView> sent(@AuthenticationPrincipal AuthenticatedUser me,
                                                  @RequestParam(required = false) ExchangeRequestStatus status,
                                                  @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.sent(me.getId(), status, page, size);
    }

    @GetMapping("/requests/received")
    public PageResponse<ExchangeRequestView> received(@AuthenticationPrincipal AuthenticatedUser me,
                                                      @RequestParam(required = false) ExchangeRequestStatus status,
                                                      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.received(me.getId(), status, page, size);
    }

    @GetMapping("/requests/history")
    public PageResponse<ExchangeRequestView> history(@AuthenticationPrincipal AuthenticatedUser me,
                                                     @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.history(me.getId(), page, size);
    }

    @GetMapping("/requests/{id}")
    public ExchangeRequestView get(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return service.get(me.getId(), id);
    }

    @PatchMapping("/requests/{id}/accept")
    public ExchangeRequestView accept(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return service.accept(me.getId(), id);
    }

    @PatchMapping("/requests/{id}/reject")
    public ExchangeRequestView reject(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                                      @Valid @RequestBody(required = false) ReasonRequest body) {
        return service.reject(me.getId(), id, body == null ? null : body.reason());
    }

    /** Cancels a pending request (requester) or withdraws from an accepted exchange (either side, reason required). */
    @PatchMapping("/requests/{id}/cancel")
    public ExchangeRequestView cancel(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                                      @Valid @RequestBody(required = false) ReasonRequest body) {
        return service.cancel(me.getId(), id, body == null ? null : body.reason());
    }

    @PatchMapping("/requests/{id}/confirm-completion")
    public ExchangeRequestView confirm(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return service.confirmCompletion(me.getId(), id);
    }
}
