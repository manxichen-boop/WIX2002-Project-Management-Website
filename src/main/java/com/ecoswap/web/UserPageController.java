package com.ecoswap.web;

import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.category.CategoryService;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.item.dto.ItemDetailView;
import com.ecoswap.item.entity.ItemCondition;
import com.ecoswap.item.entity.ListingType;
import com.ecoswap.item.service.ItemQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Student application pages (role USER, enforced by SecurityConfig). Data is loaded through /api. */
@Controller
public class UserPageController {

    private final CategoryService categories;
    private final ItemQueryService itemQueries;

    public UserPageController(CategoryService categories, ItemQueryService itemQueries) {
        this.categories = categories;
        this.itemQueries = itemQueries;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return page(model, "dashboard", "pages/user/dashboard");
    }

    @GetMapping("/items/new")
    public String postItem(Model model) {
        formOptions(model);
        model.addAttribute("item", null);
        return page(model, "post", "pages/user/post-item");
    }

    @GetMapping("/items/{id:\\d+}/edit")
    public String editItem(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser me, Model model) {
        ItemDetailView item = itemQueries.details(id, me);
        if (!item.viewer().owner()) {
            throw new NotFoundException("Listing");   // never reveal other students' listings for editing
        }
        formOptions(model);
        model.addAttribute("item", item);
        return page(model, "listings", "pages/user/post-item");
    }

    @GetMapping("/my/listings")
    public String myListings(Model model) {
        return page(model, "listings", "pages/user/my-listings");
    }

    @GetMapping("/my/requests")
    public String myRequests(Model model) {
        return page(model, "requests", "pages/user/my-requests");
    }

    @GetMapping("/my/received-requests")
    public String receivedRequests(Model model) {
        return page(model, "received", "pages/user/received-requests");
    }

    @GetMapping("/my/saved")
    public String savedItems(Model model) {
        return page(model, "saved", "pages/user/saved-items");
    }

    @GetMapping("/my/history")
    public String history(Model model) {
        return page(model, "history", "pages/user/exchange-history");
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal AuthenticatedUser me, Model model) {
        model.addAttribute("shell", me.isAdmin() ? "admin" : "user");
        return page(model, "notifications", "pages/user/notifications");
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal AuthenticatedUser me, Model model) {
        model.addAttribute("shell", me.isAdmin() ? "admin" : "user");
        return page(model, "profile", "pages/user/profile");
    }

    /** Chats with the other student of each accepted exchange. */
    @GetMapping({"/messages", "/messages/{requestId:\\d+}"})
    public String messages(@PathVariable(required = false) Long requestId, Model model) {
        model.addAttribute("openRequestId", requestId);
        return page(model, "messages", "pages/user/messages");
    }

    @GetMapping("/support")
    public String support(Model model) {
        return page(model, "support", "pages/user/support");
    }

    private void formOptions(Model model) {
        model.addAttribute("categories", categories.activeCategories());
        model.addAttribute("conditions", ItemCondition.values());
        model.addAttribute("listingTypes", ListingType.values());
    }

    private static String page(Model model, String section, String view) {
        model.addAttribute("activeSection", section);
        return view;
    }
}
