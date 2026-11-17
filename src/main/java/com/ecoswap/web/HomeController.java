package com.ecoswap.web;

import com.ecoswap.category.CategoryService;
import com.ecoswap.impact.ImpactService;
import com.ecoswap.item.service.ItemQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ImpactService impactService;
    private final ItemQueryService itemQueries;
    private final CategoryService categoryService;

    public HomeController(ImpactService impactService, ItemQueryService itemQueries, CategoryService categoryService) {
        this.impactService = impactService;
        this.itemQueries = itemQueries;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("impact", impactService.summary());
        model.addAttribute("featuredItems", itemQueries.featured());
        model.addAttribute("categoryCounts", categoryService.availableCounts());
        model.addAttribute("recentExchanges", impactService.recentExchanges());
        return "pages/home";
    }
}
