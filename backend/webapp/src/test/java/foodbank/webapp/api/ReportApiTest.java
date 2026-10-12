package foodbank.webapp.api;

import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.entity.Report;
import foodbank.webapp.entity.Request;
import foodbank.webapp.entity.Timeslot;
import foodbank.webapp.repositories.ReportRepository;
import foodbank.webapp.repositories.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportApiTest extends ApiTestSupport {

    @Autowired private RequestRepository requestRepository;
    @Autowired private ReportRepository reportRepository;

    private Recipient recipient;
    private Donor donor;
    private Listing listing;
    private Timeslot timeslot;
    private Request approvedRequest;

    @BeforeEach
    void setUp() {
        recipient = createRecipient();
        donor = createDonor();
        listing = createLiveListing(donor, "Bread rolls");
        timeslot = createTimeslot(listing, 3);
        approvedRequest = saveRequest("APPROVED");
    }

    private Request saveRequest(String status) {
        return requestRepository.save(Request.builder()
                .timeslotId(timeslot.getTimeslotId())
                .recipientId(recipient.getRecipientId())
                .quantityRequested(1f)
                .status(status)
                .build());
    }

    private String donorToken() {
        return bearer(donor.getUserId(), "donor");
    }

    private ResultActions submitDonorReport(String evidenceJson) throws Exception {
        return mockMvc.perform(post("/api/reports")
                .header("Authorization", donorToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reportType": "RECIPIENT_BEHAVIOUR", "listingId": %s, "pickupRequestId": %s,
                         "reportedUserId": %s, "reason": "Recipient no-show", "details": "Did not arrive",
                         "evidenceImageUrl": %s, "actualTransactionTime": "2026-10-12T17:45:00"}
                        """.formatted(listing.getListingId(), approvedRequest.getRequestId(),
                        recipient.getUserId(), evidenceJson)));
    }

    @Test
    void listsOnlyApprovedRequestsAsReportable() throws Exception {
        saveRequest("PENDING");
        saveRequest("REJECTED");

        mockMvc.perform(get("/api/donors/{donorId}/requests/reportable", donor.getId())
                        .header("Authorization", donorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].requestId").value(approvedRequest.getRequestId()))
                .andExpect(jsonPath("$[0].listingId").value(listing.getListingId()))
                .andExpect(jsonPath("$[0].listingName").value("Bread rolls"))
                .andExpect(jsonPath("$[0].timeslotId").value(timeslot.getTimeslotId()))
                .andExpect(jsonPath("$[0].recipientUserId").value(recipient.getUserId()))
                .andExpect(jsonPath("$[0].recipientName").value("Test Recipient"));
    }

    @Test
    void donorReportStoresEvidenceImageUrl() throws Exception {
        submitDonorReport("\"/uploads/evidence.jpg\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertThat(reportRepository.findAll())
                .singleElement()
                .extracting(Report::getEvidenceImageUrl)
                .isEqualTo("/uploads/evidence.jpg");
    }

    @Test
    void blankEvidenceImageUrlIsStoredAsNull() throws Exception {
        submitDonorReport("\"   \"")
                .andExpect(status().isCreated());

        assertThat(reportRepository.findAll())
                .singleElement()
                .extracting(Report::getEvidenceImageUrl)
                .isNull();
    }

    @Test
    void donorReportListIncludesDisplayFields() throws Exception {
        submitDonorReport("\"/uploads/evidence.jpg\"")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/donors/{donorId}/reports", donor.getId())
                        .header("Authorization", donorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reason").value("Recipient no-show"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].listingName").value("Bread rolls"))
                .andExpect(jsonPath("$[0].reportedName").value("Test Recipient"))
                .andExpect(jsonPath("$[0].timeslotStart").value(startsWith(timeslot.getStartTime().toLocalDate().toString())))
                .andExpect(jsonPath("$[0].evidenceImageUrl").value("/uploads/evidence.jpg"))
                .andExpect(jsonPath("$[0].actualTransactionTime").value("2026-10-12T17:45:00"));
    }

    private Report submitAndFindReport() throws Exception {
        submitDonorReport("null").andExpect(status().isCreated());
        return reportRepository.findAll().get(0);
    }

    private ResultActions editReport(Integer reportId, String token) throws Exception {
        return mockMvc.perform(put("/api/donors/{donorId}/reports/{reportId}", donor.getId(), reportId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reason": "Late collection", "details": "  Arrived an hour late  ",
                         "evidenceImageUrl": "/uploads/new.jpg", "actualTransactionTime": "2026-10-12T18:45:00"}
                        """));
    }

    @Test
    void donorCanEditReportUnderReview() throws Exception {
        Report report = submitAndFindReport();

        editReport(report.getReportId(), donorToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        Report updated = reportRepository.findById(report.getReportId()).orElseThrow();
        assertThat(updated.getReason()).isEqualTo("Late collection");
        assertThat(updated.getDetails()).isEqualTo("Arrived an hour late");
        assertThat(updated.getEvidenceImageUrl()).isEqualTo("/uploads/new.jpg");
        assertThat(updated.getActualTransactionTime()).hasToString("2026-10-12T18:45");
    }

    @Test
    void donorCanCancelReportUnderReview() throws Exception {
        Report report = submitAndFindReport();

        mockMvc.perform(patch("/api/donors/{donorId}/reports/{reportId}/cancel", donor.getId(), report.getReportId())
                        .header("Authorization", donorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // A cancelled report can no longer be edited or cancelled again
        editReport(report.getReportId(), donorToken())
                .andExpect(status().isConflict());
    }

    @Test
    void reportAnAdminPickedUpCannotBeEdited() throws Exception {
        for (String adminStatus : new String[] {"IN_REVIEW", "RESOLVED"}) {
            Report report = submitAndFindReport();
            report.setStatus(adminStatus);
            reportRepository.save(report);

            editReport(report.getReportId(), donorToken())
                    .andExpect(status().isConflict());
            reportRepository.deleteAll();
        }
    }

    @Test
    void donorCannotEditAnotherDonorsReport() throws Exception {
        Report report = submitAndFindReport();
        Donor otherDonor = createDonor();

        mockMvc.perform(put("/api/donors/{donorId}/reports/{reportId}", otherDonor.getId(), report.getReportId())
                        .header("Authorization", bearer(otherDonor.getUserId(), "donor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Other\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void editRequiresReason() throws Exception {
        Report report = submitAndFindReport();

        mockMvc.perform(put("/api/donors/{donorId}/reports/{reportId}", donor.getId(), report.getReportId())
                        .header("Authorization", donorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"  \"}"))
                .andExpect(status().isBadRequest());
    }
}
