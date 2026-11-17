package com.ecoswap.admin.service;

import com.ecoswap.admin.dto.AdminCategoryView;
import com.ecoswap.admin.dto.CategoryForm;
import com.ecoswap.audit.AuditAction;
import com.ecoswap.audit.AuditService;
import com.ecoswap.audit.AuditTargetType;
import com.ecoswap.category.Category;
import com.ecoswap.category.CategoryRepository;
import com.ecoswap.common.exception.BusinessRuleException;
import com.ecoswap.common.exception.InvalidInputException;
import com.ecoswap.common.exception.NotFoundException;
import com.ecoswap.common.util.Slugs;
import com.ecoswap.common.util.Texts;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Category management. Categories that listings use are never deleted (they are deactivated
 * instead), so existing listings keep a valid category.
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryService {

    private final CategoryRepository categories;
    private final UserRepository users;
    private final AuditService audit;

    public AdminCategoryService(CategoryRepository categories, UserRepository users, AuditService audit) {
        this.categories = categories;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AdminCategoryView> all() {
        return categories.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(c -> AdminCategoryView.of(c, categories.countItemsUsing(c.getId()))).toList();
    }

    @Transactional
    public AdminCategoryView create(Long adminId, CategoryForm form) {
        String name = form.name().strip();
        if (categories.existsByNameIgnoreCase(name)) {
            throw new InvalidInputException("name", "A category with this name already exists.");
        }
        String slug = uniqueSlug(name, null);
        Category c = categories.save(new Category(name, slug, Texts.clean(form.description()),
                form.iconKey() == null ? "more" : form.iconKey(), form.displayOrder() == null ? 100 : form.displayOrder()));
        audit.log(admin(adminId), AuditAction.CATEGORY_CREATED, AuditTargetType.CATEGORY, c.getId(), name);
        return AdminCategoryView.of(c, 0);
    }

    @Transactional
    public AdminCategoryView update(Long adminId, Long id, CategoryForm form) {
        Category c = find(id);
        String name = form.name().strip();
        if (categories.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new InvalidInputException("name", "A category with this name already exists.");
        }
        // The slug only changes with the name, so existing /explore?category= links keep working otherwise.
        String slug = name.equalsIgnoreCase(c.getName()) ? c.getSlug() : uniqueSlug(name, id);
        c.update(name, slug, Texts.clean(form.description()), form.iconKey() == null ? c.getIconKey() : form.iconKey(),
                form.displayOrder() == null ? c.getDisplayOrder() : form.displayOrder());
        audit.log(admin(adminId), AuditAction.CATEGORY_UPDATED, AuditTargetType.CATEGORY, c.getId(), name);
        return AdminCategoryView.of(c, categories.countItemsUsing(id));
    }

    @Transactional
    public AdminCategoryView setActive(Long adminId, Long id, boolean active) {
        Category c = find(id);
        if (!active && categories.findByActiveTrueOrderByDisplayOrderAscNameAsc().size() <= 1 && c.isActive()) {
            throw new BusinessRuleException("LAST_CATEGORY", "At least one category must stay active.");
        }
        c.setActive(active);
        audit.log(admin(adminId), active ? AuditAction.CATEGORY_ACTIVATED : AuditAction.CATEGORY_DEACTIVATED,
                AuditTargetType.CATEGORY, c.getId(), c.getName());
        return AdminCategoryView.of(c, categories.countItemsUsing(id));
    }

    /** Hard delete is only allowed for a category no listing has ever used. */
    @Transactional
    public void delete(Long adminId, Long id) {
        Category c = find(id);
        long used = categories.countItemsUsing(id);
        if (used > 0) {
            throw new BusinessRuleException("CATEGORY_IN_USE",
                    used + " listing(s) use this category, so it can't be deleted. Deactivate it instead.");
        }
        audit.log(admin(adminId), AuditAction.CATEGORY_DEACTIVATED, AuditTargetType.CATEGORY, c.getId(), "Deleted unused category " + c.getName());
        categories.delete(c);
    }

    private String uniqueSlug(String name, Long exceptId) {
        String base = Slugs.of(name);
        String slug = base;
        int n = 2;
        while (exceptId == null ? categories.existsBySlug(slug) : categories.existsBySlugAndIdNot(slug, exceptId)) {
            slug = base + "-" + n++;
        }
        return slug;
    }

    private Category find(Long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Category"));
    }

    private User admin(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("Administrator"));
    }
}
