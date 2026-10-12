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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].reason").value("Recipient no-show"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].listingName").value("Bread rolls"))
                .andExpect(jsonPath("$.content[0].reportedName").value("Test Recipient"))
                .andExpect(jsonPath("$.content[0].timeslotStart").value(startsWith(timeslot.getStartTime().toLocalDate().toString())))
                .andExpect(jsonPath("$.content[0].evidenceImageUrl").value("/uploads/evidence.jpg"))
                .andExpect(jsonPath("$.content[0].actualTransactionTime").value("2026-10-12T17:45:00"));
    }

    @Test
    void listsAllReportStatuses() throws Exception {
        mockMvc.perform(get("/api/reports/statuses")
                        .header("Authorization", donorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("PENDING", "IN_REVIEW", "RESOLVED", "DISMISSED")));
    }

    @Test
    void donorReportListFiltersByStatus() throws Exception {
        submitDonorReport("null").andExpect(status().isCreated());
        submitDonorReport("null").andExpect(status().isCreated());
        Report resolved = reportRepository.findAll().get(0);
        resolved.setStatus("RESOLVED");
        reportRepository.save(resolved);

        mockMvc.perform(get("/api/donors/{donorId}/reports", donor.getId())
                        .param("status", "resolved")
                        .header("Authorization", donorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].reportId").value(resolved.getReportId()))
                .andExpect(jsonPath("$.content[0].status").value("RESOLVED"));

        // No status means every report
        mockMvc.perform(get("/api/donors/{donorId}/reports", donor.getId())
                        .header("Authorization", donorToken()))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void unknownStatusFilterIsRejected() throws Exception {
        mockMvc.perform(get("/api/donors/{donorId}/reports", donor.getId())
                        .param("status", "ARCHIVED")
                        .header("Authorization", donorToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void donorReportListIsPaginatedNewestFirst() throws Exception {
        for (int i = 0; i < 3; i++) {
            submitDonorReport("null").andExpect(status().isCreated());
        }
        Integer newestId = reportRepository.findAll().stream()
                .map(Report::getReportId)
                .max(Integer::compare)
                .orElseThrow();

        mockMvc.perform(get("/api/donors/{donorId}/reports", donor.getId())
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", donorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].reportId").value(newestId))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        mockMvc.perform(get("/api/donors/{donorId}/reports", donor.getId())
                        .param("page", "1")
                        .param("size", "2")
                        .header("Authorization", donorToken()))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void donorReportListDefaultsToTenPerPage() throws Exception {
        mockMvc.perform(get("/api/donors/{donorId}/reports", donor.getId())
                        .header("Authorization", donorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    void donorReportListSearchesListingRecipientAndReason() throws Exception {
        submitDonorReport("null").andExpect(status().isCreated());

        for (String term : new String[] {"bread", "TEST RECIP", "no-show", "did not"}) {
            mockMvc.perform(get("/api/donors/{donorId}/reports", donor.getId())
                            .param("q", term)
                            .header("Authorization", donorToken()))
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }

        // LIKE wildcards in the search are matched literally
        mockMvc.perform(get("/api/donors/{donorId}/reports", donor.getId())
                        .param("q", "%")
                        .header("Authorization", donorToken()))
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void invalidPageSizeIsRejected() throws Exception {
        mockMvc.perform(get("/api/donors/{donorId}/reports", donor.getId())
                        .param("size", "0")
                        .header("Authorization", donorToken()))
                .andExpect(status().isBadRequest());
    }
}
