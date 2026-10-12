package foodbank.webapp.api;

import foodbank.webapp.entity.Admin;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.repositories.AdminRepository;
import foodbank.webapp.repositories.BanRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminApiTest extends ApiTestSupport {
    @Autowired AdminRepository admins;
    @Autowired BanRecordRepository bans;
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
    @Test void defaultAdminIsSeededOnStartup() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@test.com\",\"password\":\"password\",\"role\":\"admin\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.name").value("admin"));
    }
    @Test void rejectsDuplicateAndMissingFields() throws Exception {
        mockMvc.perform(post("/api/admin/admins").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Dup\",\"email\":\"STAFF@example.com\",\"password\":\"x\"}")).andExpect(status().isConflict());
        mockMvc.perform(post("/api/admin/admins").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"No Email\",\"password\":\"x\"}")).andExpect(status().isBadRequest());
    }
    @Test void accountListsIncludeContactAndLocation() throws Exception {
        Donor donor = createDonor(); Recipient recipient = createRecipient();
        recipient.setSuburb("Norwood"); recipient.setRegion("SA"); recipientRepository.save(recipient);
        String donorEmail = userRepository.findById(donor.getUserId()).orElseThrow().getEmail();
        String recipientEmail = userRepository.findById(recipient.getUserId()).orElseThrow().getEmail();
        mockMvc.perform(get("/api/donors").header("Authorization",token)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(donorEmail)).andExpect(jsonPath("$[0].phone").value("0400000000"))
                .andExpect(jsonPath("$[0].suburb").value("Adelaide")).andExpect(jsonPath("$[0].region").value("SA"));
        mockMvc.perform(get("/api/recipients").header("Authorization",token)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(recipientEmail)).andExpect(jsonPath("$[0].phone").value("0400000000"))
                .andExpect(jsonPath("$[0].suburb").value("Norwood")).andExpect(jsonPath("$[0].region").value("SA"));
    }
    @Test void permanentBanThenUnbanRestoresAccess() throws Exception {
        Recipient recipient = createRecipient();
        String email = userRepository.findById(recipient.getUserId()).orElseThrow().getEmail();
        String login = "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"recipient\"}";
        mockMvc.perform(post("/api/admin/users/{id}/ban",recipient.getUserId()).header("Authorization",token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Fraud\",\"banType\":\"PERMANENT\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userStatus").value("BANNED"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login)).andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/users/{id}/unban",recipient.getUserId()).header("Authorization",token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userStatus").value("ACTIVE"));
        assertThat(bans.findByUserIdAndActiveTrueOrderByStartDateAsc(recipient.getUserId())).isEmpty();
        assertThat(bans.countByUserId(recipient.getUserId())).isEqualTo(1);
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login)).andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/users/{id}/unban",recipient.getUserId()).header("Authorization",token)).andExpect(status().isConflict());
        mockMvc.perform(post("/api/admin/users/{id}/unban",999999).header("Authorization",token)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/admin/users/{id}/unban",recipient.getUserId())
                .header("Authorization",bearer(recipient.getUserId(),"recipient"))).andExpect(status().isForbidden());
    }
    @Test void accountsArePagedAndFilteredByRoleStatusAndSearch() throws Exception {
        Donor donor = createDonor(); createDonor();
        Recipient recipient = createRecipient(); createRecipient(); createRecipient();
        var banned = userRepository.findById(recipient.getUserId()).orElseThrow();
        banned.setStatus("BANNED"); userRepository.save(banned);
        mockMvc.perform(get("/api/admin/accounts?page=0&size=2").header("Authorization",token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(5)).andExpect(jsonPath("$.page.totalPages").value(3));
        mockMvc.perform(get("/api/admin/accounts?page=2&size=2").header("Authorization",token))
                .andExpect(jsonPath("$.content.length()").value(1));
        mockMvc.perform(get("/api/admin/accounts?role=donor").header("Authorization",token))
                .andExpect(jsonPath("$.page.totalElements").value(2)).andExpect(jsonPath("$.content[0].role").value("DONOR"));
        mockMvc.perform(get("/api/admin/accounts?role=RECIPIENT&status=BANNED").header("Authorization",token))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(recipient.getUserId()))
                .andExpect(jsonPath("$.content[0].status").value("BANNED"));
        mockMvc.perform(get("/api/admin/accounts?status=ACTIVE").header("Authorization",token))
                .andExpect(jsonPath("$.page.totalElements").value(4));
        String donorEmail = userRepository.findById(donor.getUserId()).orElseThrow().getEmail();
        mockMvc.perform(get("/api/admin/accounts").param("q", donorEmail.toUpperCase()).header("Authorization",token))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(donor.getUserId()))
                .andExpect(jsonPath("$.content[0].email").value(donorEmail));
    }
    @Test void accountsRejectInvalidFiltersAndNonAdmins() throws Exception {
        mockMvc.perform(get("/api/admin/accounts?role=ADMIN").header("Authorization",token)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/accounts?status=PENDING").header("Authorization",token)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/accounts?size=101").header("Authorization",token)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/accounts")).andExpect(status().isUnauthorized());
        Recipient recipient = createRecipient();
        mockMvc.perform(get("/api/admin/accounts").header("Authorization",bearer(recipient.getUserId(),"recipient")))
                .andExpect(status().isForbidden());
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
