package com.ecoswap.web;

import com.ecoswap.admin.service.AdminCountsService;
import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.category.CategoryService;
import com.ecoswap.chat.ChatMessageRepository;
import com.ecoswap.category.CategoryView;
import com.ecoswap.exchange.repository.ExchangeRequestRepository;
import com.ecoswap.exchange.entity.ExchangeRequestStatus;
import com.ecoswap.notification.NotificationRepository;
import com.ecoswap.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Year;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Model attributes every page layout needs: the signed-in user, category menu, live badge
 * counts and the active navigation item. Applies to page controllers and the error pages only
 * (REST controllers are excluded so API calls stay cheap).
 */
@ControllerAdvice(basePackages = {"com.ecoswap.web", "org.springframework.boot.autoconfigure.web.servlet.error"})
public class GlobalModelAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalModelAdvice.class);

    private final UserRepository users;
    private final CategoryService categories;
    private final NotificationRepository notifications;
    private final ExchangeRequestRepository requests;
    private final AdminCountsService adminCounts;
    private final ChatMessageRepository chatMessages;

    @org.springframework.beans.factory.annotation.Value("${ecoswap.map.center:3.1209,101.6538}")
    private String mapCenter;

    @org.springframework.beans.factory.annotation.Value("${ecoswap.map.zoom:15}")
    private int mapZoom;

    public GlobalModelAdvice(UserRepository users, CategoryService categories, NotificationRepository notifications,
                             ExchangeRequestRepository requests, AdminCountsService adminCounts,
                             ChatMessageRepository chatMessages) {
        this.chatMessages = chatMessages;
        this.users = users;
        this.categories = categories;
        this.notifications = notifications;
        this.requests = requests;
        this.adminCounts = adminCounts;
    }

    @ModelAttribute
    void layoutAttributes(@AuthenticationPrincipal AuthenticatedUser principal, HttpServletRequest request, Model model) {
        String path = request.getRequestURI();
        model.addAttribute("currentPath", path);
        model.addAttribute("activeNav", activeNav(path));
        model.addAttribute("currentYear", Year.now().getValue());
        model.addAttribute("navCategories", safeCategories());
        model.addAttribute("mapCenter", mapCenter);
        model.addAttribute("mapZoom", mapZoom);

        CurrentUserView current = null;
        long unread = 0;
        long pendingReceived = 0;
        long unreadMessages = 0;
        if (principal != null) {
            try {
                current = users.findById(principal.getId()).map(CurrentUserView::of).orElse(null);
                unread = notifications.countByUserIdAndReadFalse(principal.getId());
                if (!principal.isAdmin()) {
                    pendingReceived = requests.countByItemOwnerIdAndStatus(principal.getId(), ExchangeRequestStatus.PENDING);
                    unreadMessages = chatMessages.countUnreadFor(principal.getId());
                } else {
                    // Admins see their sidebar (with queue counts) on every app page, incl. /notifications and /profile.
                    model.addAttribute("adminCounts", adminCounts.sidebarCounts());
                }
            } catch (RuntimeException e) {
                // The layout must still render (e.g. on an error page while the database is down).
                log.debug("Could not load layout data for user {}", principal.getId(), e);
            }
        }
        model.addAttribute("currentUser", current);
        model.addAttribute("unreadCount", unread);
        model.addAttribute("pendingRequestCount", pendingReceived);
        model.addAttribute("unreadMessageCount", unreadMessages);
    }

    private List<CategoryView> safeCategories() {
        try {
            return categories.activeCategories();
        } catch (RuntimeException e) {
            log.debug("Could not load categories for the navigation", e);
            return List.of();
        }
    }

    private static String activeNav(String path) {
        if (path.equals("/")) return "home";
        if (path.startsWith("/explore") || path.startsWith("/pickup-map") || path.matches("/items/\\d+")) return "explore";
        if (path.startsWith("/how-it-works")) return "how";
        if (path.startsWith("/community")) return "community";
        if (path.startsWith("/impact")) return "impact";
        return "";
    }
}
