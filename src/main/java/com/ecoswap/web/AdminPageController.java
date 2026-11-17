package com.ecoswap.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Administrator pages. Data comes from /api/admin/** (also ADMIN-only). */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPageController {

    @GetMapping
    public String dashboard(Model model) { return page(model, "overview", "dashboard"); }

    @GetMapping("/pending-users")
    public String pendingUsers(Model model) { return page(model, "pending-users", "pending-users"); }

    @GetMapping("/users")
    public String users(Model model) { return page(model, "users", "all-users"); }

    @GetMapping("/pending-listings")
    public String pendingListings(Model model) { return page(model, "pending-listings", "pending-listings"); }

    @GetMapping("/listings")
    public String listings(Model model) { return page(model, "listings", "all-listings"); }

    @GetMapping("/exchanges")
    public String exchanges(Model model) { return page(model, "exchanges", "exchanges"); }

    @GetMapping("/reports")
    public String reports(Model model) { return page(model, "reports", "reports"); }

    @GetMapping("/support")
    public String support(Model model) { return page(model, "support", "support"); }

    @GetMapping("/categories")
    public String categories(Model model) { return page(model, "categories", "categories"); }

    @GetMapping("/audit-logs")
    public String auditLogs(Model model) { return page(model, "audit", "audit-logs"); }

    private static String page(Model model, String section, String view) {
        model.addAttribute("activeSection", section);
        return "pages/admin/" + view;
    }
}
