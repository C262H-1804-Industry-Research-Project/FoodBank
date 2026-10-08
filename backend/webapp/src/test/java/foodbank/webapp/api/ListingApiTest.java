package foodbank.webapp.api;

import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Timeslot;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ListingApiTest extends ApiTestSupport {

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
}
