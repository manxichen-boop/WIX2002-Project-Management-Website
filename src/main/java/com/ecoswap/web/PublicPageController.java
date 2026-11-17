package com.ecoswap.web;

import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.impact.ImpactService;
import com.ecoswap.item.entity.ItemCondition;
import com.ecoswap.item.entity.ListingType;
import com.ecoswap.item.service.ItemQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Public pages. Explore results are loaded by explore.js from /api/items. */
@Controller
public class PublicPageController {

    private final ItemQueryService itemQueries;
    private final ImpactService impact;

    public PublicPageController(ItemQueryService itemQueries, ImpactService impact) {
        this.itemQueries = itemQueries;
        this.impact = impact;
    }

    @GetMapping("/explore")
    public String explore(Model model) {
        model.addAttribute("conditions", ItemCondition.values());
        model.addAttribute("listingTypes", ListingType.values());
        return "pages/explore";
    }

    /** Rendered server-side for a fast first paint and shareable links; actions go through the API. */
    @GetMapping("/items/{id:\\d+}")
    public String itemDetails(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser me, Model model) {
        model.addAttribute("item", itemQueries.details(id, me));
        return "pages/item-details";
    }

    /** Map of pickup points for approved, available listings (data from /api/items/map). */
    @GetMapping("/pickup-map")
    public String pickupMap(Model model) {
        return "pages/pickup-map";
    }

    @GetMapping("/how-it-works")
    public String howItWorks() {
        return "pages/how-it-works";
    }

    @GetMapping("/community")
    public String community(Model model) {
        model.addAttribute("impact", impact.summary());
        model.addAttribute("recentExchanges", impact.recentExchanges());
        model.addAttribute("newListings", itemQueries.featured());
        return "pages/community";
    }

    @GetMapping("/impact")
    public String impact(Model model) {
        model.addAttribute("impact", impact.summary());
        return "pages/impact";
    }
}
