package com.ecoswap.impact;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, privacy-safe platform statistics. */
@RestController
@RequestMapping("/api")
public class ImpactController {

    private final ImpactService service;

    public ImpactController(ImpactService service) {
        this.service = service;
    }

    @GetMapping("/impact/summary")
    public ImpactSummary summary() {
        return service.summary();
    }

    @GetMapping("/community/activity")
    public List<CommunityActivity> activity() {
        return service.recentExchanges();
    }
}
