package foodbank.webapp.api;

import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SuburbFilterApiTest extends ApiTestSupport {

    @BeforeEach
    void setUp() {
        Donor donor = createDonor();
        createListingIn(donor, "Zz suburb test bread", "Glenelg", "SA", "LIVE");
        createListingIn(donor, "Zz suburb test milk", "Glenelg North", "SA", "LIVE");
        createListingIn(donor, "Zz suburb test apples", " glenelg ", "sa", "LIVE");
        createListingIn(donor, "Zz suburb test soup", "Richmond", "SA", "LIVE");
        createListingIn(donor, "Zz suburb test rice", "Richmond", "VIC", "LIVE");
    }

    private void createListingIn(Donor donor, String name, String suburb, String region, String status) {
        Listing listing = createLiveListing(donor, name);
        listing.setSuburb(suburb);
        listing.setRegion(region);
        listing.setStatus(status);
        listingRepository.save(listing);
    }

    @Test
    void filtersByAnyOfTheSelectedSuburbs() throws Exception {
        mockMvc.perform(get("/api/listings").param("foodName", "zz suburb test")
                        .param("suburbs", "Glenelg|SA", "Richmond|VIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].foodName", containsInAnyOrder(
                        "Zz suburb test bread", "Zz suburb test apples", "Zz suburb test rice")));
    }

    @Test
    void suburbWithoutRegionMatchesEveryState() throws Exception {
        mockMvc.perform(get("/api/listings").param("foodName", "zz suburb test")
                        .param("suburbs", "richmond"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].foodName", containsInAnyOrder(
                        "Zz suburb test soup", "Zz suburb test rice")));
    }
}
