package com.ecoswap.category;

import com.ecoswap.common.exception.InvalidInputException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public category lookups. Admin create/update lives in admin.service.AdminCategoryService. */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    public List<CategoryView> activeCategories() {
        return categories.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream().map(CategoryView::of).toList();
    }

    /** Active category by id, for posting/editing listings. */
    public Category requireActive(Long id) {
        return categories.findById(id).filter(Category::isActive)
                .orElseThrow(() -> new InvalidInputException("categoryId", "Choose a valid category."));
    }

    /** Number of open (approved + available) listings per category id. */
    public Map<Long, Long> availableCounts() {
        return categories.countAvailableItemsPerCategory().stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }
}
