package foodbank.webapp.api;

import foodbank.webapp.entity.Admin;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.repositories.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminApiTest extends ApiTestSupport {
    @Autowired AdminRepository admins;
    String token;
    @BeforeEach void setup() {
        Admin admin = admins.save(Admin.builder().name("Staff").email("staff@example.com").password(passwordEncoder.encode(PASSWORD)).build());
        token = bearer(admin.getAdminId(), "admin");
    }
    @Test void createsAdminWhoCanLogIn() throws Exception {
        mockMvc.perform(post("/api/admin/admins").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Admin\",\"email\":\" new@example.com \",\"password\":\"Secret123\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.adminId").isNumber())
                .andExpect(jsonPath("$.email").value("new@example.com")).andExpect(jsonPath("$.password").doesNotExist());
        assertThat(admins.findByEmailIgnoreCase("new@example.com").orElseThrow().getPassword()).isNotEqualTo("Secret123");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@example.com\",\"password\":\"Secret123\",\"role\":\"admin\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("admin"));
    }
    @Test void rejectsDuplicateAndMissingFields() throws Exception {
        mockMvc.perform(post("/api/admin/admins").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Dup\",\"email\":\"STAFF@example.com\",\"password\":\"x\"}")).andExpect(status().isConflict());
        mockMvc.perform(post("/api/admin/admins").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"No Email\",\"password\":\"x\"}")).andExpect(status().isBadRequest());
    }
    @Test void rejectsNonAdmin() throws Exception {
        String body = "{\"name\":\"X\",\"email\":\"x@example.com\",\"password\":\"x\"}";
        mockMvc.perform(post("/api/admin/admins").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        Recipient recipient = createRecipient();
        mockMvc.perform(post("/api/admin/admins").header("Authorization",bearer(recipient.getUserId(),"recipient"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        assertThat(admins.findByEmailIgnoreCase("x@example.com")).isEmpty();
    }
}
