package com.ecoswap.report;

import com.ecoswap.auth.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportView create(@AuthenticationPrincipal AuthenticatedUser me, @Valid @RequestBody CreateReportBody body) {
        return service.create(me.getId(), body);
    }
}
