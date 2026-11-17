package com.ecoswap.favorite;

import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.common.response.MessageResponse;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.item.dto.ItemCardView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService service;

    public FavoriteController(FavoriteService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ItemCardView> list(@AuthenticationPrincipal AuthenticatedUser me,
                                           @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
        return service.list(me.getId(), page, size);
    }

    @PostMapping("/{itemId}")
    public MessageResponse save(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long itemId) {
        service.save(me.getId(), itemId);
        return new MessageResponse("Saved.");
    }

    @DeleteMapping("/{itemId}")
    public MessageResponse remove(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long itemId) {
        service.remove(me.getId(), itemId);
        return new MessageResponse("Removed from saved items.");
    }
}
