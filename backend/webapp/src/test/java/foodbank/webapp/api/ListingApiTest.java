package foodbank.webapp.api;

import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Request;
import foodbank.webapp.entity.Timeslot;
import foodbank.webapp.repositories.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ListingApiTest extends ApiTestSupport {
    @Autowired RequestRepository requestRepository;


    @Test
    void browseIsPublicAndIncludesTimeslotIds() throws Exception {
        Donor donor = createDonor();
        Listing listing = createLiveListing(donor, "Unique carrot crate");
        Timeslot timeslot = createTimeslot(listing, 2);

        mockMvc.perform(get("/api/listings").param("foodName", "unique carrot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].foodName").value("Unique carrot crate"))
                .andExpect(jsonPath("$.content[0].donorName").value("Test Donor"))
                .andExpect(jsonPath("$.content[0].timeslots[0].timeslotId").value(timeslot.getTimeslotId()))
                .andExpect(jsonPath("$.content[0].timeslots[0].remainingSlot").value(2));
    }

    @Test
    void browseHidesListingsThatAreNotLive() throws Exception {
        Listing listing = createLiveListing(createDonor(), "Hidden draft soup");
        listing.setStatus("DRAFT");
        listingRepository.save(listing);

        mockMvc.perform(get("/api/listings").param("foodName", "hidden draft soup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void categoriesAndTagsArePublic() throws Exception {
        mockMvc.perform(get("/api/listings/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Produce")));

        mockMvc.perform(get("/api/listings/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("vegan")));
    }

    @Test
    void donorListingsIncludeTimeslotIds() throws Exception {
        Donor donor = createDonor();
        Listing listing = createLiveListing(donor, "Donor's own listing");
        Timeslot timeslot = createTimeslot(listing, 1);

        mockMvc.perform(get("/api/donors/{donorId}/listings", donor.getId())
                        .header("Authorization", bearer(donor.getUserId(), "donor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timeslots[0].timeslotId").value(timeslot.getTimeslotId()));
    }

    @Test
    void donorListingsRequireDonorRole() throws Exception {
        Donor donor = createDonor();

        mockMvc.perform(get("/api/donors/{donorId}/listings", donor.getId()))
                .andExpect(status().isUnauthorized());
    }

    // Edit the listing with exactly these timeslots ([start, end] pairs)
    private ResultActions updateTimeslots(Donor donor, Listing listing, LocalDateTime[]... slots) throws Exception {
        StringBuilder timeslots = new StringBuilder();
        for (LocalDateTime[] slot : slots) {
            if (!timeslots.isEmpty()) timeslots.append(", ");
            timeslots.append("{\"startTime\": \"%s\", \"endTime\": \"%s\", \"maxRecipients\": 1}".formatted(slot[0], slot[1]));
        }
        return mockMvc.perform(put("/api/donors/{donorId}/listings/{listingId}", donor.getId(), listing.getListingId())
                .header("Authorization", bearer(donor.getUserId(), "donor"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Bread", "categoryId": %d, "quantity": 5, "addressLine": "1 King William St",
                         "suburb": "Adelaide", "region": "SA", "isAutoApproved": false, "usePickupCode": false,
                         "hideContact": false, "slotCapacity": 1, "status": "LIVE", "tagIds": [], "timeslots": [%s]}
                        """.formatted(listing.getCategory().getCategoryId(), timeslots)));
    }

    private Timeslot bookedSlot(Listing listing) {
        Timeslot slot = createTimeslot(listing, 1);
        slot.setRemainingSlot(0);
        return timeslotRepository.save(slot);
    }

    private static LocalDateTime[] times(Timeslot slot) {
        return new LocalDateTime[] {slot.getStartTime(), slot.getEndTime()};
    }

    @Test
    void quantityCannotBeLoweredBelowReservedStock() throws Exception {
        Donor donor = createDonor();
        Listing listing = createLiveListing(donor, "Bread");
        Timeslot booked = bookedSlot(listing);
        requestRepository.save(Request.builder().timeslotId(booked.getTimeslotId())
                .recipientId(createRecipient().getRecipientId()).quantityRequested(6f).status("PENDING").build());

        updateTimeslots(donor, listing, times(booked))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Quantity cannot be below reserved stock (6)"));
        assertThat(listingRepository.findById(listing.getListingId()).orElseThrow().getQuantity()).isEqualTo(10f);
    }

    @Test
    void quantityCanBeLoweredOnceExpiredStockIsReleased() throws Exception {
        Donor donor = createDonor();
        Listing listing = createLiveListing(donor, "Bread");
        Timeslot booked = bookedSlot(listing);
        requestRepository.save(Request.builder().timeslotId(booked.getTimeslotId())
                .recipientId(createRecipient().getRecipientId()).quantityRequested(6f).status("EXPIRED")
                .stockReleasedAt(LocalDateTime.now()).build());

        updateTimeslots(donor, listing, times(booked)).andExpect(status().isOk());
        assertThat(listingRepository.findById(listing.getListingId()).orElseThrow().getQuantity()).isEqualTo(5f);
    }

    @Test
    void addingATimeslotKeepsExistingBookedSlots() throws Exception {
        Donor donor = createDonor();
        Listing listing = createLiveListing(donor, "Bread");
        Timeslot booked = bookedSlot(listing);
        LocalDateTime newStart = booked.getStartTime().plusDays(1);

        updateTimeslots(donor, listing, times(booked), new LocalDateTime[] {newStart, newStart.plusHours(1)})
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeslots", hasSize(2)));

        List<Timeslot> slots = timeslotRepository.findByListing(listing);
        assertThat(slots).hasSize(2);
        Timeslot kept = timeslotRepository.findById(booked.getTimeslotId()).orElseThrow();
        assertThat(kept.getRemainingSlot()).isZero();
        assertThat(slots).anySatisfy(slot -> {
            assertThat(slot.getStartTime()).isEqualTo(newStart);
            assertThat(slot.getRemainingSlot()).isEqualTo(1);
        });
    }

    @Test
    void unbookedTimeslotsCanBeRemovedButBookedOnesCannot() throws Exception {
        Donor donor = createDonor();
        Listing listing = createLiveListing(donor, "Bread");
        Timeslot booked = bookedSlot(listing);
        Timeslot free = createTimeslot(listing, 1);
        free.setStartTime(free.getStartTime().plusDays(2));
        free.setEndTime(free.getEndTime().plusDays(2));
        timeslotRepository.save(free);

        updateTimeslots(donor, listing, times(booked)).andExpect(status().isOk());
        assertThat(timeslotRepository.findByListing(listing))
                .extracting(Timeslot::getTimeslotId).containsExactly(booked.getTimeslotId());

        updateTimeslots(donor, listing).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Pickup timeslots with bookings cannot be removed"));
        assertThat(timeslotRepository.findById(booked.getTimeslotId())).isPresent();
    }
}
