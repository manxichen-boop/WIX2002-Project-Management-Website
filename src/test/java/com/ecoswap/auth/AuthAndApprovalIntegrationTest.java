package com.ecoswap.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ecoswap.support.IntegrationTestBase;
import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.User;
import com.ecoswap.user.entity.UserRole;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Registration → approval / rejection → login → suspension → reactivation, against MySQL. */
class AuthAndApprovalIntegrationTest extends IntegrationTestBase {

    private Map<String, Object> registration(String email) {
        return Map.of("fullName", "Aisha Karim", "email", email, "password", PASSWORD, "confirmPassword", PASSWORD,
                "university", "Universiti Malaya", "acceptTerms", true);
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("email", email, "password", password, "rememberMe", false))));
    }

    @Test
    void registrationCreatesPendingStudentAndNotifiesAdmins() throws Exception {
        User admin = admin();
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(registration("Aisha@Siswa.um.edu.my"))))
                .andExpect(status().isCreated());

        User saved = users.findByEmail("aisha@siswa.um.edu.my").orElseThrow();   // e-mail normalised
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        assertThat(saved.getPasswordHash()).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(jdbc.queryForObject("select count(*) from notifications where user_id = ?", Long.class, admin.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from email_verification_tokens where user_id = ?", Long.class, saved.getId())).isEqualTo(1);
    }

    @Test
    void duplicateEmailAndWeakPasswordAreRejected() throws Exception {
        student("Aisha Karim", AccountStatus.ACTIVE);
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(registration("aisha.karim@siswa.um.edu.my"))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
        var weak = new java.util.HashMap<>(registration("new@siswa.um.edu.my"));
        weak.put("password", "short");
        weak.put("confirmPassword", "short");
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(weak)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }

    @Test
    void publicRegistrationCannotEscalateRole() throws Exception {
        var attempt = new java.util.HashMap<>(registration("sneaky@siswa.um.edu.my"));
        attempt.put("role", "ADMIN");
        attempt.put("accountStatus", "ACTIVE");
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(attempt)))
                .andExpect(status().isCreated());
        User u = users.findByEmail("sneaky@siswa.um.edu.my").orElseThrow();
        assertThat(u.getRole()).isEqualTo(UserRole.USER);
        assertThat(u.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
    }

    @Test
    void approvalLifecycleControlsLogin() throws Exception {
        User admin = admin();
        User student = student("Ben Tan", AccountStatus.PENDING_APPROVAL);

        login(student.getEmail(), PASSWORD).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_PENDING"));
        login(student.getEmail(), "wrong-pass1").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mvc.perform(patch("/api/admin/users/{id}/approve", student.getId()).with(as(admin)).with(csrf())).andExpect(status().isOk());
        login(student.getEmail(), PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.redirectUrl").value("/dashboard"));

        // Suspension takes effect immediately, even for an existing session
        mvc.perform(patch("/api/admin/users/{id}/suspend", student.getId()).with(as(admin)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("reason", "Repeated no-shows")))).andExpect(status().isOk());
        mvc.perform(get("/api/dashboard/summary").with(as(student))).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));
        login(student.getEmail(), PASSWORD).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"));

        mvc.perform(patch("/api/admin/users/{id}/activate", student.getId()).with(as(admin)).with(csrf())).andExpect(status().isOk());
        login(student.getEmail(), PASSWORD).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select count(*) from admin_audit_logs", Long.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from approval_history where entity_type = 'USER'", Long.class)).isEqualTo(3);
    }

    @Test
    void rejectedStudentSeesReasonAndCannotUseApp() throws Exception {
        User admin = admin();
        User student = student("Chen Wei", AccountStatus.PENDING_APPROVAL);
        mvc.perform(patch("/api/admin/users/{id}/reject", student.getId()).with(as(admin)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("reason", "Not a university e-mail")))).andExpect(status().isOk());
        login(student.getEmail(), PASSWORD).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_REJECTED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Not a university e-mail")));
        mvc.perform(get("/api/items/mine").with(as(users.findById(student.getId()).orElseThrow()))).andExpect(status().isUnauthorized());
    }
}
