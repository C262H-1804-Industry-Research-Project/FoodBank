package foodbank.webapp.api;

import foodbank.webapp.entity.*;
import foodbank.webapp.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminModerationApiTest extends ApiTestSupport {
    @Autowired AdminRepository admins;
    @Autowired ReportRepository reports;
    @Autowired BanRecordRepository bans;
    String token;
    Donor donor;
    Recipient recipient;
    Listing listing;
    Report report;
    @BeforeEach void setup() {
        Admin admin = admins.save(Admin.builder().name("Staff").email("moderation@example.com").password(passwordEncoder.encode(PASSWORD)).build());
        token = bearer(admin.getAdminId(), "admin");
        donor = createDonor(); recipient = createRecipient(); listing = createLiveListing(donor, "Food");
        report = reports.save(Report.builder().reporterId(donor.getUserId()).reportedId(recipient.getUserId())
                .listingId(listing.getListingId()).reportType("RECIPIENT_BEHAVIOUR").reason("No show").status("PENDING").build());
    }
    @Test void listsAllStatusesAndBanPersists() throws Exception {
        Listing draft = createLiveListing(donor, "Draft"); draft.setStatus("DRAFT"); listingRepository.save(draft);
        mockMvc.perform(get("/api/admin/listings").header("Authorization",token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mockMvc.perform(post("/api/admin/listings/{id}/ban",listing.getListingId()).header("Authorization",token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BANNED"));
        assertThat(listingRepository.findById(listing.getListingId()).orElseThrow().getStatus()).isEqualTo("BANNED");
        mockMvc.perform(post("/api/admin/listings/{id}/ban",listing.getListingId()).header("Authorization",token)).andExpect(status().isConflict());
        mockMvc.perform(get("/api/listings")).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
    }
    @Test void rejectsNonAdminAndInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/admin/listings")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/listings").header("Authorization",bearer(recipient.getUserId(),"recipient"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/listings?size=101").header("Authorization",token)).andExpect(status().isBadRequest());
    }
    @Test void processingTemporaryBanStoresReportAndRevokesToken() throws Exception {
        String userToken = bearer(recipient.getUserId(), "recipient");
        mockMvc.perform(patch("/api/admin/reports/{id}",report.getReportId()).header("Authorization",token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"action\":\"BAN\",\"banDays\":3,\"adminNote\":\"Checked\",\"outcome\":\"Temporary suspension\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.banCount").value(1)).andExpect(jsonPath("$.reportedName").value("Test Recipient"));
        assertThat(userRepository.findById(recipient.getUserId()).orElseThrow().getStatus()).isEqualTo("BANNED");
        assertThat(bans.countByUserId(recipient.getUserId())).isEqualTo(1);
        mockMvc.perform(get("/api/requests").header("Authorization",userToken)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/admin/reports/{id}",report.getReportId()).header("Authorization",token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"action\":\"BAN\",\"banDays\":3}"))
                .andExpect(status().isConflict());
    }
    @Test void failedBanDoesNotMarkReportResolved() throws Exception {
        mockMvc.perform(patch("/api/admin/reports/{id}",report.getReportId()).header("Authorization",token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"action\":\"BAN\",\"banDays\":0}"))
                .andExpect(status().isBadRequest());
        assertThat(reports.findById(report.getReportId()).orElseThrow().getStatus()).isEqualTo("PENDING");
        assertThat(bans.countByUserId(recipient.getUserId())).isZero();
    }
    @Test void reviewWarningAndDismissDoNotBan() throws Exception {
        for (String[] action : new String[][] {{"REVIEW","IN_REVIEW"},{"WARNING","RESOLVED"}}) {
            mockMvc.perform(patch("/api/admin/reports/{id}",report.getReportId()).header("Authorization",token)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\""+action[1]+"\",\"action\":\""+action[0]+"\",\"adminNote\":\"Verified\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(action[1]));
        }
        assertThat(bans.countByUserId(recipient.getUserId())).isZero();
        report.setStatus("PENDING"); reports.save(report);
        mockMvc.perform(patch("/api/admin/reports/{id}",report.getReportId()).header("Authorization",token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISMISSED\",\"action\":\"DISMISS\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISMISSED"));
    }
    @Test void permanentBanAndPolicyPersist() throws Exception {
        mockMvc.perform(patch("/api/admin/reports/{id}",report.getReportId()).header("Authorization",token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"action\":\"DEACTIVATE\"}"))
                .andExpect(status().isOk());
        assertThat(bans.findAll().stream().filter(b -> b.getUserId().equals(recipient.getUserId())).findFirst().orElseThrow().getBanType()).isEqualTo("PERMANENT");
        mockMvc.perform(put("/api/settings/ban-days").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"first\":2,\"second\":5,\"third\":20}" )).andExpect(status().isOk());
        mockMvc.perform(get("/api/settings/ban-days").header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.first").value(2));
    }
}
