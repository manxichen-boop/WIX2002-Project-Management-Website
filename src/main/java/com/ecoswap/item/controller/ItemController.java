package com.ecoswap.item.controller;

import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.common.response.MessageResponse;
import com.ecoswap.common.response.PageResponse;
import com.ecoswap.item.dto.ItemCardView;
import com.ecoswap.item.dto.ItemDetailView;
import com.ecoswap.item.dto.ItemForm;
import com.ecoswap.item.dto.ItemSearchCriteria;
import com.ecoswap.item.dto.MyListingView;
import com.ecoswap.item.entity.ItemCondition;
import com.ecoswap.item.entity.ListingType;
import com.ecoswap.item.service.ItemQueryService;
import com.ecoswap.item.service.ItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    private final ItemQueryService queries;

    public ItemController(ItemService itemService, ItemQueryService queries) {
        this.itemService = itemService;
        this.queries = queries;
    }

    /** Public Explore search: approved and available items only. */
    @GetMapping
    public PageResponse<ItemCardView> search(@AuthenticationPrincipal AuthenticatedUser me,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) ItemCondition condition,
                                             @RequestParam(required = false) ListingType type,
                                             @RequestParam(required = false) String location,
                                             @RequestParam(defaultValue = "newest") String sort,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "12") int size) {
        return queries.search(new ItemSearchCriteria(trim(q, 80), category, condition, type, trim(location, 80), sort, page, size),
                me == null ? null : me.getId());
    }

    @GetMapping("/map")
    public List<ItemCardView> mapPins() {
        return queries.mapPins();
    }

    @GetMapping("/{id:\\d+}")
    public ItemDetailView details(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return queries.details(id, me);
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@AuthenticationPrincipal AuthenticatedUser me, @Valid @ModelAttribute ItemForm form,
                                      @RequestParam(name = "photos", required = false) List<MultipartFile> photos) {
        Long id = itemService.create(me.getId(), form, photos);
        return Map.of("id", id, "message", "Listing submitted. It will appear in Explore once an administrator approves it.");
    }

    @PutMapping(path = "/{id:\\d+}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public MessageResponse update(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                                  @Valid @ModelAttribute ItemForm form,
                                  @RequestParam(name = "photos", required = false) List<MultipartFile> photos,
                                  @RequestParam(name = "removeImageIds", required = false) List<Long> removeImageIds) {
        itemService.update(me.getId(), id, form, photos, removeImageIds);
        return new MessageResponse("Listing updated.");
    }

    /** Archives the listing (soft delete). */
    @DeleteMapping("/{id:\\d+}")
    public MessageResponse archive(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        itemService.archive(me.getId(), id);
        return new MessageResponse("Listing archived.");
    }

    @GetMapping("/mine")
    public PageResponse<MyListingView> mine(@AuthenticationPrincipal AuthenticatedUser me,
                                            @RequestParam(defaultValue = "active") String tab,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return itemService.myListings(me.getId(), tab, page, size);
    }

    @GetMapping("/mine/available")
    public List<ItemCardView> mineAvailable(@AuthenticationPrincipal AuthenticatedUser me) {
        return itemService.availableForSwap(me.getId());
    }

    private static String trim(String value, int max) {
        if (value == null) return null;
        String v = value.strip();
        return v.length() > max ? v.substring(0, max) : v;
    }
}
