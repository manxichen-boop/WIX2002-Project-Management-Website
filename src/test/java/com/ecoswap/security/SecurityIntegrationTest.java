package com.ecoswap.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ecoswap.item.entity.Item;
import com.ecoswap.item.entity.ListingType;
import com.ecoswap.support.IntegrationTestBase;
import com.ecoswap.user.entity.AccountStatus;
import com.ecoswap.user.entity.User;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/** Security boundaries against the running application: roles, CSRF, ownership, uploads and visibility. */
class SecurityIntegrationTest extends IntegrationTestBase {

    @Test
    void anonymousUsersAreBlockedFromProtectedApisAndPages() throws Exception {
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/requests/sent")).andExpect(status().isUnauthorized());
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("/login?next=*"));
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/api/items")).andExpect(status().isOk());
    }

    @Test
    void studentsCannotUseAdminEndpoints() throws Exception {
        User student = student("Ben Tan", AccountStatus.ACTIVE);
        mvc.perform(get("/api/admin/users").with(as(student))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/users/{id}/approve", student.getId()).with(as(student)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(as(student))).andExpect(status().isForbidden());
    }

    @Test
    void stateChangingRequestsRequireCsrfToken() throws Exception {
        User student = student("Ben Tan", AccountStatus.ACTIVE);
        Item item = approvedItem(student("Aisha Karim", AccountStatus.ACTIVE), ListingType.GIVEAWAY, admin());
        mvc.perform(post("/api/favorites/{id}", item.getId()).with(as(student))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        mvc.perform(post("/api/favorites/{id}", item.getId()).with(as(student)).with(csrf())).andExpect(status().isOk());
    }

    @Test
    void studentsCannotModifyOtherStudentsListings() throws Exception {
        User owner = student("Aisha Karim", AccountStatus.ACTIVE);
        User other = student("Ben Tan", AccountStatus.ACTIVE);
        Item item = approvedItem(owner, ListingType.SWAP, admin());
        mvc.perform(multipart("/api/items/{id}", item.getId()).with(r -> { r.setMethod("PUT"); return r; }).with(as(other)).with(csrf())
                        .param("name", "Hijacked listing").param("categoryId", item.getCategory().getId().toString())
                        .param("description", "Changed by somebody else").param("condition", "GOOD")
                        .param("listingType", "SWAP").param("pickupLocation", "Anywhere"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/items/{id}", item.getId()).with(as(other)).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void invalidUploadsAreRejected() throws Exception {
        User student = student("Ben Tan", AccountStatus.ACTIVE);
        var fake = new MockMultipartFile("photos", "photo.jpg", "image/jpeg", "<script>alert(1)</script>".getBytes());
        mvc.perform(multipart("/api/items").file(fake).with(as(student)).with(csrf())
                        .param("name", "Desk lamp").param("categoryId", categories.findBySlug("household").orElseThrow().getId().toString())
                        .param("description", "Working desk lamp with LED bulb").param("condition", "GOOD")
                        .param("listingType", "GIVEAWAY").param("pickupLocation", "KK8"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("photos"));
    }

    @Test
    void pendingListingsAreHiddenFromOthers() throws Exception {
        User owner = student("Aisha Karim", AccountStatus.ACTIVE);
        Item item = approvedItem(owner, ListingType.SWAP, admin());
        jdbc.update("update items set approval_status = 'PENDING_APPROVAL' where item_id = ?", item.getId());
        mvc.perform(get("/api/items/{id}", item.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/items/{id}", item.getId()).with(as(owner))).andExpect(status().isOk());
        mvc.perform(get("/api/items")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void unknownLoginAndWrongPasswordGiveTheSameAnswer() throws Exception {
        student("Ben Tan", AccountStatus.ACTIVE);
        for (String email : new String[]{"ben.tan@siswa.um.edu.my", "nobody@siswa.um.edu.my"}) {
            mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(body(Map.of("email", email, "password", "WrongPass1", "rememberMe", false))))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
    }
}
