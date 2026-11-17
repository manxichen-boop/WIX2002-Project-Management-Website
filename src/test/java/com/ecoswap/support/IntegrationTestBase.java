package com.ecoswap.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.ecoswap.auth.security.AuthenticatedUser;
import com.ecoswap.category.CategoryRepository;
import com.ecoswap.item.entity.Item;
import com.ecoswap.item.entity.ItemCondition;
import com.ecoswap.item.entity.ItemImage;
import com.ecoswap.item.entity.ListingApprovalStatus;
import com.ecoswap.item.entity.ListingType;
import com.ecoswap.item.repository.ItemRepository;
import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Integration tests run the full application against a real MySQL 8 database (profile "test",
 * default database ecoswap_test). Each test starts from empty tables; reference categories stay.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    private static final List<String> TABLES = List.of("admin_audit_logs", "approval_history", "support_replies", "support_tickets",
            "reports", "notifications", "favorites", "chat_messages", "stored_files", "exchange_requests", "item_images", "items",
            "email_verification_tokens", "password_reset_tokens", "users");

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected UserRepository users;
    @Autowired protected ItemRepository items;
    @Autowired protected CategoryRepository categories;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected JdbcTemplate jdbc;

    protected static final String PASSWORD = "Swap2026x";

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        TABLES.forEach(t -> jdbc.execute("TRUNCATE TABLE " + t));
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    protected User student(String name, AccountStatus status) {
        User u = User.newStudent(name, name.toLowerCase().replace(' ', '.') + "@siswa.um.edu.my", encoder.encode(PASSWORD), "Universiti Malaya");
        u.changeStatus(status, null);
        return users.save(u);
    }

    protected User admin() {
        return users.save(User.newAdministrator("Test Admin", "admin." + UUID.randomUUID() + "@ecoswap.test", encoder.encode(PASSWORD)));
    }

    /** An approved, available listing (with one image row, no file needed for these tests). */
    protected Item approvedItem(User owner, ListingType type, User admin) {
        Item item = new Item(owner, categories.findBySlug("books").orElseThrow(), "Item " + UUID.randomUUID().toString().substring(0, 6),
                "A useful item in good condition.", ItemCondition.GOOD, type, "Main Library", new BigDecimal("1.50"));
        item.addImage(new ItemImage("items/test/" + UUID.randomUUID() + ".jpg", "photo.jpg", "image/jpeg", 100, 0));
        item.moderate(ListingApprovalStatus.APPROVED, admin, null);
        return items.save(item);
    }

    protected RequestPostProcessor as(User user) {
        AuthenticatedUser principal = AuthenticatedUser.from(user);
        return authentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    protected String body(Object value) throws Exception {
        return json.writeValueAsString(value);
    }
}
