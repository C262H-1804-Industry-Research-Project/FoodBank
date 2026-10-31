package foodbank.webapp.api;

import foodbank.webapp.entity.Admin;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.entity.Timeslot;
import foodbank.webapp.repositories.AdminRepository;
import foodbank.webapp.repositories.CollectionHistoryRepository;
import foodbank.webapp.repositories.RequestRepository;
import foodbank.webapp.service.RequestExpiryService;
import foodbank.webapp.service.StockReleaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ShumingFlowsApiTest extends ApiTestSupport {
    @Autowired RequestRepository requestRepository;
    @Autowired AdminRepository adminRepository;
    @Autowired RequestExpiryService requestExpiryService;
    @Autowired StockReleaseService stockReleaseService;
    @Autowired CollectionHistoryRepository collectionHistoryRepository;
    private Donor donor;
    private Recipient recipient;
    private Listing listing;
    private Timeslot slot;

    @BeforeEach
    void prepare() {
        donor = createDonor();
        recipient = createRecipient();
        recipient.setOrgName("Test Kitchen");
        recipientRepository.save(recipient);
        listing = createLiveListing(donor, "Test Food");
        listing.setUsePickupCode(true);
        listing.setAddressLine("50 Pickup Road");
        listingRepository.save(listing);
        slot = createTimeslot(listing, 2);
    }

    private String path(Donor owner) { return "/api/donors/" + owner.getId() + "/requests"; }
    private String donorToken() { return bearer(donor.getUserId(), "donor"); }
    private ResultActions submit(Recipient owner) throws Exception {
        return submit(owner, 2);
    }
    private ResultActions submit(Recipient owner, float quantity) throws Exception {
        return mockMvc.perform(post("/api/requests").header("Authorization", recipientToken(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeslotId\":" + slot.getTimeslotId() + ",\"quantityRequested\":" + quantity
                        + ",\"note\":\"  lunch  \"}"));
    }
    private ResultActions browseThisListing() throws Exception {
        return mockMvc.perform(get("/api/listings").param("foodName", "Test Food")
                .header("Authorization", recipientToken(recipient)));
    }
    private int savedRequest() throws Exception {
        submit(recipient).andExpect(status().isCreated());
        return requestRepository.findByTimeslotId(slot.getTimeslotId()).get(0).getRequestId();
    }

    @Test
    void submitApproveViewCancelAndSubmitAgain() throws Exception {
        int id = savedRequest();
        mockMvc.perform(get("/api/requests").header("Authorization", recipientToken(recipient)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].pickupAddress").value(nullValue()))
                .andExpect(jsonPath("$[0].pickupCode").value(nullValue()))
                .andExpect(jsonPath("$[0].donorName").value("Test Donor"));
        mockMvc.perform(get(path(donor)).header("Authorization", donorToken()))
                .andExpect(jsonPath("$[0].recipientOrg").value("Test Kitchen"))
                .andExpect(jsonPath("$[0].recipientPhone").value("0400000000"))
                .andExpect(jsonPath("$[0].listingQuantity").value(10.0))
                .andExpect(jsonPath("$[0].note").value("lunch"));
        mockMvc.perform(patch(path(donor) + "/" + id + "/approve").header("Authorization", donorToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.pickupCode").value(matchesPattern("\\d{6}")));
        mockMvc.perform(get(path(donor)).header("Authorization", donorToken()))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].status").value("APPROVED"));
        mockMvc.perform(get("/api/requests").header("Authorization", recipientToken(recipient)))
                .andExpect(jsonPath("$[0].pickupAddress").value("50 Pickup Road, Adelaide, SA"));
        mockMvc.perform(delete("/api/requests/" + id).header("Authorization", recipientToken(recipient)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/requests").header("Authorization", recipientToken(recipient)))
                .andExpect(jsonPath("$[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$[0].pickupCode").value(nullValue()))
                .andExpect(jsonPath("$[0].pickupAddress").value(nullValue()));
        assertThat(timeslotRepository.findById(slot.getTimeslotId()).orElseThrow().getRemainingSlot()).isEqualTo(2);
        submit(recipient).andExpect(status().isCreated());
    }

    @Test
    void rejectRestoresOnePlaceAndCannotBeRepeatedOrApproved() throws Exception {
        int id = savedRequest();
        mockMvc.perform(patch(path(donor) + "/" + id + "/reject").header("Authorization", donorToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("DECLINED_BY_DONOR"));
        for (String action : new String[]{"approve", "reject"}) {
            mockMvc.perform(patch(path(donor) + "/" + id + "/" + action).header("Authorization", donorToken()))
                    .andExpect(status().isConflict());
        }
        assertThat(timeslotRepository.findById(slot.getTimeslotId()).orElseThrow().getRemainingSlot()).isEqualTo(2);
        mockMvc.perform(get(path(donor)).header("Authorization", donorToken()))
                .andExpect(jsonPath("$[0].status").value("REJECTED"));
    }

    @Test
    void handoverCompletesApprovedRequestOnlyOnce() throws Exception {
        int id = savedRequest();
        mockMvc.perform(patch(path(donor) + "/" + id + "/complete").header("Authorization", donorToken()))
                .andExpect(status().isConflict());
        mockMvc.perform(patch(path(donor) + "/" + id + "/approve").header("Authorization", donorToken()))
                .andExpect(status().isOk());
        mockMvc.perform(patch(path(donor) + "/" + id + "/complete").header("Authorization", donorToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        for (String action : new String[]{"approve", "reject", "complete"}) {
            mockMvc.perform(patch(path(donor) + "/" + id + "/" + action).header("Authorization", donorToken()))
                    .andExpect(status().isConflict());
        }
        assertThat(timeslotRepository.findById(slot.getTimeslotId()).orElseThrow().getRemainingSlot()).isEqualTo(1);
        mockMvc.perform(get("/api/requests").header("Authorization", recipientToken(recipient)))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
        mockMvc.perform(delete("/api/requests/" + id).header("Authorization", recipientToken(recipient)))
                .andExpect(status().isConflict());
    }

    // The default handover grace period is 15 minutes
    private void endSlot(int minutesAgo) {
        slot.setStartTime(LocalDateTime.now().minusHours(2));
        slot.setEndTime(LocalDateTime.now().minusMinutes(minutesAgo));
        timeslotRepository.save(slot);
    }

    private int approvedRequest() throws Exception {
        int id = savedRequest();
        mockMvc.perform(patch(path(donor) + "/" + id + "/approve").header("Authorization", donorToken()))
                .andExpect(status().isOk());
        return id;
    }

    @Test
    void expiryJobRejectsUnreviewedAndExpiresUncollectedRequests() throws Exception {
        int pending = savedRequest();
        Recipient other = createRecipient();
        submit(other).andExpect(status().isCreated());
        int approved = requestRepository.findByTimeslotId(slot.getTimeslotId()).stream()
                .filter(r -> r.getRequestId() != pending).findFirst().orElseThrow().getRequestId();
        mockMvc.perform(patch(path(donor) + "/" + approved + "/approve").header("Authorization", donorToken()))
                .andExpect(status().isOk());
        Timeslot future = createTimeslot(listing, 1);
        int untouched = requestRepository.save(foodbank.webapp.entity.Request.builder()
                .timeslotId(future.getTimeslotId()).recipientId(recipient.getRecipientId())
                .quantityRequested(1f).status("PENDING").build()).getRequestId();
        endSlot(20);

        requestExpiryService.closeEndedRequests();

        assertThat(requestRepository.findById(pending).orElseThrow().getStatus()).isEqualTo("REJECTED");
        assertThat(requestRepository.findById(approved).orElseThrow().getStatus()).isEqualTo("EXPIRED");
        assertThat(requestRepository.findById(untouched).orElseThrow().getStatus()).isEqualTo("PENDING");
        assertThat(timeslotRepository.findById(slot.getTimeslotId()).orElseThrow().getRemainingSlot()).isEqualTo(0);
        mockMvc.perform(get("/api/requests").header("Authorization", recipientToken(recipient)))
                .andExpect(jsonPath("$[?(@.requestId == " + pending + ")].rejectionReason").value("NOT_REVIEWED_IN_TIME"));
        mockMvc.perform(get("/api/requests").header("Authorization", recipientToken(other)))
                .andExpect(jsonPath("$[0].status").value("EXPIRED"))
                .andExpect(jsonPath("$[0].pickupAddress").value(nullValue()))
                .andExpect(jsonPath("$[0].rejectionReason").value(nullValue()));
        for (String action : new String[]{"approve", "reject", "complete"}) {
            mockMvc.perform(patch(path(donor) + "/" + pending + "/" + action).header("Authorization", donorToken()))
                    .andExpect(status().isConflict());
        }
        for (String action : new String[]{"approve", "reject"}) {
            mockMvc.perform(patch(path(donor) + "/" + approved + "/" + action).header("Authorization", donorToken()))
                    .andExpect(status().isConflict());
        }
    }

    @Test
    void donorCanCorrectAnExpiredRequestToCompletedBeforeItsStockIsReleased() throws Exception {
        int id = approvedRequest();
        endSlot(20);
        requestExpiryService.closeEndedRequests();
        assertThat(requestRepository.findById(id).orElseThrow().getStatus()).isEqualTo("EXPIRED");

        mockMvc.perform(patch(path(donor) + "/" + id + "/complete").header("Authorization", donorToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(collectionHistoryRepository.findAll()).anyMatch(h -> h.getRequestId() == id);
        // Completed keeps holding the stock the expired request held
        browseThisListing().andExpect(jsonPath("$.content[0].availableQuantity").value(8.0));
        stockReleaseService.releaseExpiredStock();
        assertThat(requestRepository.findById(id).orElseThrow().getStockReleasedAt()).isNull();
    }

    @Test
    void expiredRequestCannotBeCorrectedOnceItsStockIsReleased() throws Exception {
        int id = saveRequestWith(slotEndingAt(midnight().minusHours(1)), "EXPIRED", 2);
        stockReleaseService.releaseExpiredStock();

        mockMvc.perform(patch(path(donor) + "/" + id + "/complete").header("Authorization", donorToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("The stock for this request has already been released"));
        assertThat(requestRepository.findById(id).orElseThrow().getStatus()).isEqualTo("EXPIRED");
        assertThat(collectionHistoryRepository.findAll()).noneMatch(h -> h.getRequestId() == id);
    }

    @Test
    void donorCanCompleteWithinTheGracePeriodAfterTheSlotEnds() throws Exception {
        int id = approvedRequest();
        endSlot(5);
        requestExpiryService.closeEndedRequests();
        assertThat(requestRepository.findById(id).orElseThrow().getStatus()).isEqualTo("APPROVED");
        mockMvc.perform(get(path(donor)).header("Authorization", donorToken()))
                .andExpect(jsonPath("$[0].handoverDeadline").value(slot.getEndTime().plusMinutes(15).toString()));
        mockMvc.perform(patch(path(donor) + "/" + id + "/complete").header("Authorization", donorToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(collectionHistoryRepository.findAll()).anyMatch(h -> h.getRequestId() == id);
    }

    @Test
    void handoverIsRefusedAfterTheGracePeriodEvenBeforeTheJobRuns() throws Exception {
        int id = approvedRequest();
        endSlot(16);
        mockMvc.perform(patch(path(donor) + "/" + id + "/complete").header("Authorization", donorToken()))
                .andExpect(status().isConflict());
        assertThat(requestRepository.findById(id).orElseThrow().getStatus()).isEqualTo("APPROVED");
        assertThat(collectionHistoryRepository.findAll()).noneMatch(h -> h.getRequestId() == id);
    }

    @Test
    void pendingRequestIsRejectedRightAtSlotEndWithoutGrace() throws Exception {
        int id = savedRequest();
        endSlot(1);
        requestExpiryService.closeEndedRequests();
        assertThat(requestRepository.findById(id).orElseThrow().getStatus()).isEqualTo("REJECTED");
    }

    @Test
    void approveAndRejectAreBlockedOnceTheSlotEndsEvenBeforeTheJobRuns() throws Exception {
        int id = savedRequest();
        endSlot(1);
        for (String action : new String[]{"approve", "reject"}) {
            mockMvc.perform(patch(path(donor) + "/" + id + "/" + action).header("Authorization", donorToken()))
                    .andExpect(status().isConflict());
        }
        assertThat(requestRepository.findById(id).orElseThrow().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void requestHoldsStockFromSubmissionUntilRejected() throws Exception {
        int id = savedRequest();
        browseThisListing().andExpect(jsonPath("$.content[0].availableQuantity").value(8.0));
        mockMvc.perform(get(path(donor)).header("Authorization", donorToken()))
                .andExpect(jsonPath("$[0].listingQuantity").value(10.0))
                .andExpect(jsonPath("$[0].availableQuantity").value(8.0));
        mockMvc.perform(patch(path(donor) + "/" + id + "/reject").header("Authorization", donorToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.availableQuantity").value(10.0));
    }

    @Test
    void cancellingGivesStockBackButCompletedAndExpiredKeepIt() throws Exception {
        int id = savedRequest();
        mockMvc.perform(delete("/api/requests/" + id).header("Authorization", recipientToken(recipient)))
                .andExpect(status().isNoContent());
        browseThisListing().andExpect(jsonPath("$.content[0].availableQuantity").value(10.0));
        for (String status : new String[]{"COMPLETED", "EXPIRED"}) {
            requestRepository.save(foodbank.webapp.entity.Request.builder().timeslotId(slot.getTimeslotId())
                    .recipientId(createRecipient().getRecipientId()).quantityRequested(3f).status(status).build());
        }
        browseThisListing().andExpect(jsonPath("$.content[0].availableQuantity").value(4.0));
    }

    @Test
    void cannotRequestMoreThanIsLeftAndFullyReservedListingStaysVisible() throws Exception {
        submit(recipient, 11).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Only 10 left for this listing"));
        submit(recipient, 10).andExpect(status().isCreated());
        submit(createRecipient(), 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("This listing is fully reserved"));
        browseThisListing().andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].availableQuantity").value(0.0));
        assertThat(timeslotRepository.findById(slot.getTimeslotId()).orElseThrow().getRemainingSlot()).isEqualTo(1);
    }

    @Test
    void differentRecipientsCannotOrderMoreThanFourAcrossTimeslots() throws Exception {
        listing.setQuantity(4f);
        listingRepository.save(listing);
        submit(recipient, 2).andExpect(status().isCreated());
        browseThisListing().andExpect(jsonPath("$.content[0].availableQuantity").value(2.0));

        slot = createTimeslot(listing, 3);
        Recipient other = createRecipient();
        submit(other, 4).andExpect(status().isConflict());
        submit(other, 2).andExpect(status().isCreated());
        browseThisListing().andExpect(jsonPath("$.content[0].availableQuantity").value(0.0));
        submit(createRecipient(), 1).andExpect(status().isConflict());
        assertThat(timeslotRepository.findById(slot.getTimeslotId()).orElseThrow().getRemainingSlot())
                .isEqualTo(2);
    }

    private int saveRequestWith(String status, float quantity) {
        return saveRequestWith(slot, status, quantity);
    }
    private int saveRequestWith(Timeslot on, String status, float quantity) {
        return requestRepository.save(foodbank.webapp.entity.Request.builder().timeslotId(on.getTimeslotId())
                .recipientId(createRecipient().getRecipientId()).quantityRequested(quantity).status(status)
                .build()).getRequestId();
    }
    // The release cutoff: the most recent midnight in Adelaide time
    private static LocalDateTime midnight() {
        return java.time.LocalDate.now(java.time.ZoneId.of("Australia/Adelaide")).atStartOfDay();
    }
    private Timeslot slotEndingAt(LocalDateTime end) {
        Timeslot ended = createTimeslot(listing, 1);
        ended.setStartTime(end.minusHours(1));
        ended.setEndTime(end);
        return timeslotRepository.save(ended);
    }
    private java.time.LocalDateTime releasedAt(int id) {
        return requestRepository.findById(id).orElseThrow().getStockReleasedAt();
    }

    @Test
    void releaseFreesOnlyExpiredStockAndKeepsTheRequestExpired() throws Exception {
        Timeslot yesterday = slotEndingAt(midnight());
        int expired = saveRequestWith(yesterday, "EXPIRED", 3);
        int completed = saveRequestWith(yesterday, "COMPLETED", 2);
        int approved = saveRequestWith("APPROVED", 1);
        int pending = saveRequestWith("PENDING", 1);
        browseThisListing().andExpect(jsonPath("$.content[0].availableQuantity").value(3.0));

        stockReleaseService.releaseExpiredStock();

        browseThisListing().andExpect(jsonPath("$.content[0].availableQuantity").value(6.0));
        assertThat(requestRepository.findById(expired).orElseThrow().getStatus()).isEqualTo("EXPIRED");
        assertThat(releasedAt(expired)).isNotNull();
        for (int id : new int[]{completed, approved, pending}) {
            assertThat(releasedAt(id)).isNull();
        }
        mockMvc.perform(get(path(donor)).header("Authorization", donorToken()))
                .andExpect(jsonPath("$[?(@.requestId == " + expired + ")].stockReleased").value(true))
                .andExpect(jsonPath("$[?(@.requestId == " + completed + ")].stockReleased").value(false));

        var firstRelease = releasedAt(expired);
        stockReleaseService.releaseExpiredStock();
        assertThat(releasedAt(expired)).isEqualTo(firstRelease);
    }

    @Test
    void expiredRequestFromTodaysSlotWaitsForTheNextRelease() throws Exception {
        int fromYesterday = saveRequestWith(slotEndingAt(midnight().minusHours(1)), "EXPIRED", 3);
        int fromToday = saveRequestWith(slotEndingAt(midnight().plusMinutes(1)), "EXPIRED", 2);

        stockReleaseService.releaseExpiredStock();

        assertThat(releasedAt(fromYesterday)).isNotNull();
        assertThat(releasedAt(fromToday)).isNull();
        browseThisListing().andExpect(jsonPath("$.content[0].availableQuantity").value(8.0));
    }

    @Test
    void requestStillApprovedAtReleaseTimeIsReleasedByTheNextRun() throws Exception {
        int id = saveRequestWith(slotEndingAt(midnight().minusMinutes(1)), "APPROVED", 2);
        stockReleaseService.releaseExpiredStock();
        assertThat(releasedAt(id)).isNull();

        var late = requestRepository.findById(id).orElseThrow();
        late.setStatus("EXPIRED");
        requestRepository.save(late);
        stockReleaseService.releaseExpiredStock();
        assertThat(releasedAt(id)).isNotNull();
    }

    @Test
    void startupCatchUpReleasesLeftoversFromAMissedNight() throws Exception {
        int missed = saveRequestWith(slotEndingAt(midnight().minusDays(1)), "EXPIRED", 2);
        int fromToday = saveRequestWith(slotEndingAt(midnight().plusMinutes(1)), "EXPIRED", 1);

        stockReleaseService.releaseOnStartup();

        assertThat(releasedAt(missed)).isNotNull();
        assertThat(releasedAt(fromToday)).isNull();
    }

    @Test
    void anotherDonorCannotReadOrProcessThisRequest() throws Exception {
        int id = savedRequest();
        Donor other = createDonor();
        String token = bearer(other.getUserId(), "donor");
        mockMvc.perform(get(path(donor)).header("Authorization", token)).andExpect(status().isForbidden());
        for (String action : new String[]{"approve", "reject", "complete"}) {
            mockMvc.perform(patch(path(donor) + "/" + id + "/" + action).header("Authorization", token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch(path(other) + "/" + id + "/" + action).header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get(path(other)).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void recipientCannotSeeOrCancelAnotherRecipientsRequest() throws Exception {
        int id = savedRequest();
        Recipient other = createRecipient();
        mockMvc.perform(get("/api/requests").header("Authorization", recipientToken(other)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(delete("/api/requests/" + id).header("Authorization", recipientToken(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void pendingCancellationRestoresCapacityAndPreventsLaterProcessing() throws Exception {
        int id = savedRequest();
        mockMvc.perform(delete("/api/requests/" + id).header("Authorization", recipientToken(recipient)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/requests/" + id).header("Authorization", recipientToken(recipient)))
                .andExpect(status().isConflict());
        mockMvc.perform(patch(path(donor) + "/" + id + "/approve").header("Authorization", donorToken()))
                .andExpect(status().isConflict());
        assertThat(timeslotRepository.findById(slot.getTimeslotId()).orElseThrow().getRemainingSlot()).isEqualTo(2);
    }

    @Test
    void pickupStartPreventsCancellationAndNewSubmission() throws Exception {
        int id = savedRequest();
        slot.setStartTime(LocalDateTime.now().minusMinutes(1));
        timeslotRepository.save(slot);
        mockMvc.perform(delete("/api/requests/" + id).header("Authorization", recipientToken(recipient)))
                .andExpect(status().isConflict());
        submit(createRecipient()).andExpect(status().isConflict());
    }

    @Test
    void autoApprovalImmediatelyReturnsAddressAndCode() throws Exception {
        listing.setIsAutoApproved(true);
        listingRepository.save(listing);
        submit(recipient).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.pickupAddress").value("50 Pickup Road, Adelaide, SA"))
                .andExpect(jsonPath("$.pickupCode").value(matchesPattern("\\d{6}")));
    }

    @Test
    void donorEndpointsRequireDonorAuthentication() throws Exception {
        int id = savedRequest();
        mockMvc.perform(get(path(donor))).andExpect(status().isUnauthorized());
        mockMvc.perform(get(path(donor)).header("Authorization", recipientToken(recipient)))
                .andExpect(status().isForbidden());
        for (String action : new String[]{"approve", "reject"}) {
            mockMvc.perform(patch(path(donor) + "/" + id + "/" + action)
                            .header("Authorization", recipientToken(recipient)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void adminLoginAuthenticatesThePasswordAndProducesAWorkingAdminToken() throws Exception {
        String email = "staff-" + UUID.randomUUID() + "@example.com";
        adminRepository.save(Admin.builder().email(email).name("Test Staff")
                .password(passwordEncoder.encode(PASSWORD)).build());
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"admin\"}";
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("admin"))
                .andReturn().getResponse().getContentAsString();
        var match = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertThat(match.find()).isTrue();
        mockMvc.perform(get("/api/admin/reports").header("Authorization", "Bearer " + match.group(1)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace(PASSWORD, "incorrect"))).andExpect(status().isUnauthorized());
    }
}
