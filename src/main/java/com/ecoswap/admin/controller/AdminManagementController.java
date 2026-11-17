package com.ecoswap.admin.controller;

import com.ecoswap.admin.dto.AdminCategoryView;
import com.ecoswap.admin.dto.AdminDashboardView;
import com.ecoswap.admin.dto.CategoryForm;
import com.ecoswap.admin.dto.ReasonRequest;
import com.ecoswap.admin.service.AdminCategoryService;
import com.ecoswap.admin.service.AdminDashboardService;
import com.ecoswap.admin.service.AdminReportService;
import com.ecoswap.audit.AuditAction;
import com.ecoswap.audit.AuditLogView;
import com.ecoswap.audit.AuditService;
import com.ecoswap.audit.AuditTargetType;
import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.response.MessageResponse;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.common.util.Texts;
import com.ecoswap.exchange.dto.ExchangeRequestView;
import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import com.ecoswap.exchange.repository.ExchangeRequestRepository;
import com.ecoswap.exchange.service.ExchangeService;
import com.ecoswap.report.ReportStatus;
import com.ecoswap.report.ReportView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Admin dashboard, exchange monitoring, reports, categories and audit logs. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagementController {

    public record ResolveBody(@Size(max = 500, message = "Use 500 characters or fewer.") String notes,
                              boolean suspendListing, boolean suspendUser) {}

    private final AdminDashboardService dashboard;
    private final AdminReportService reports;
    private final AdminCategoryService categories;
    private final AuditService audit;
    private final ExchangeService exchanges;
    private final ExchangeRequestRepository requests;

    public AdminManagementController(AdminDashboardService dashboard, AdminReportService reports, AdminCategoryService categories,
                                     AuditService audit, ExchangeService exchanges, ExchangeRequestRepository requests) {
        this.dashboard = dashboard;
        this.reports = reports;
        this.categories = categories;
        this.audit = audit;
        this.exchanges = exchanges;
        this.requests = requests;
    }

    @GetMapping("/dashboard")
    public AdminDashboardView dashboard() {
        return dashboard.dashboard();
    }

    // ---- exchange monitoring (read-only: student exchanges don't need admin approval)

    @GetMapping("/exchanges")
    @Transactional(readOnly = true)
    public PageResponse<ExchangeRequestView> exchanges(@RequestParam(required = false) ExchangeRequestStatus status,
                                                       @RequestParam(required = false) String q,
                                                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        String like = q == null || q.isBlank() ? null : Texts.likePattern(q);
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
        return exchanges.adminPage(requests.searchAll(status, like, pageable));
    }

    @GetMapping("/exchanges/{id}")
    @Transactional(readOnly = true)
    public ExchangeRequestView exchange(@PathVariable Long id) {
        return exchanges.adminView(requests.findDetailed(id).orElseThrow(() -> new NotFoundException("Request")));
    }

    // ---- reports

    @GetMapping("/reports")
    public PageResponse<ReportView> reports(@RequestParam(required = false) ReportStatus status,
                                            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return reports.search(status, page, size);
    }

    @PatchMapping("/reports/{id}/review")
    public ReportView review(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        return reports.startReview(admin.getId(), id);
    }

    @PatchMapping("/reports/{id}/resolve")
    public ReportView resolve(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id, @Valid @RequestBody ResolveBody body) {
        return reports.resolve(admin.getId(), id, body.notes(), body.suspendListing(), body.suspendUser());
    }

    @PatchMapping("/reports/{id}/dismiss")
    public ReportView dismiss(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id, @Valid @RequestBody ReasonRequest body) {
        return reports.dismiss(admin.getId(), id, body.reason());
    }

    // ---- categories

    @GetMapping("/categories")
    public List<AdminCategoryView> categories() {
        return categories.all();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCategoryView createCategory(@AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody CategoryForm form) {
        return categories.create(admin.getId(), form);
    }

    @PutMapping("/categories/{id}")
    public AdminCategoryView updateCategory(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id, @Valid @RequestBody CategoryForm form) {
        return categories.update(admin.getId(), id, form);
    }

    @PatchMapping("/categories/{id}/deactivate")
    public AdminCategoryView deactivate(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        return categories.setActive(admin.getId(), id, false);
    }

    @PatchMapping("/categories/{id}/activate")
    public AdminCategoryView activate(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        return categories.setActive(admin.getId(), id, true);
    }

    @DeleteMapping("/categories/{id}")
    public MessageResponse deleteCategory(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        categories.delete(admin.getId(), id);
        return new MessageResponse("Category deleted.");
    }

    // ---- audit log (read-only)

    @GetMapping("/audit-logs")
    public PageResponse<AuditLogView> auditLogs(@RequestParam(required = false) AuditAction action,
                                                @RequestParam(required = false) AuditTargetType targetType,
                                                @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return audit.search(action, targetType, page, size);
    }
}
