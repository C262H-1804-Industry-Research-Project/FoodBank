package foodbank.webapp.api;

import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ListingDailyCapacityApiTest extends ApiTestSupport {

    @ParameterizedTest
    @CsvSource({
            "4, Max quantity per recipient / day cannot exceed total quantity.",
            "0, maxDailyCapacity must be greater than zero",
            "-1, maxDailyCapacity must be greater than zero"
    })
    void creatingAndEditingRejectInvalidDailyLimits(int dailyLimit, String error) throws Exception {
        Donor donor = createDonor();
        Listing listing = createLiveListing(donor, "Original bread");
        long originalCount = listingRepository.count();
        String body = listingBody(listing, dailyLimit);

        create(donor, body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(error));
        update(donor, listing, body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(error));

        assertThat(listingRepository.count()).isEqualTo(originalCount);
        Listing unchanged = listingRepository.findById(listing.getListingId()).orElseThrow();
        assertThat(unchanged.getQuantity()).isEqualTo(10f);
        assertThat(unchanged.getMaxDailyCapacity()).isNull();
        assertThat(unchanged.getName()).isEqualTo("Original bread");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {1, 3})
    void creatingAndEditingAcceptOmittedOrValidDailyLimits(Integer dailyLimit) throws Exception {
        Donor donor = createDonor();
        Listing listing = createLiveListing(donor, "Original bread");
        String body = listingBody(listing, dailyLimit);

        create(donor, body).andExpect(status().isCreated());
        update(donor, listing, body).andExpect(status().isOk());

        Listing updated = listingRepository.findById(listing.getListingId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(3f);
        assertThat(updated.getMaxDailyCapacity()).isEqualTo(dailyLimit);
    }

    @Test
    void loweringQuantityBelowTheExistingDailyLimitIsRejected() throws Exception {
        Donor donor = createDonor();
        Listing listing = createLiveListing(donor, "Original bread");
        listing.setMaxDailyCapacity(8);
        listingRepository.save(listing);

        update(donor, listing, listingBody(listing, 8))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Max quantity per recipient / day cannot exceed total quantity."));

        Listing unchanged = listingRepository.findById(listing.getListingId()).orElseThrow();
        assertThat(unchanged.getQuantity()).isEqualTo(10f);
        assertThat(unchanged.getMaxDailyCapacity()).isEqualTo(8);
    }

    private String listingBody(Listing listing, Integer dailyLimit) {
        return """
                {"name":"Updated bread","categoryId":%d,"quantity":3,"maxDailyCapacity":%s,
                 "addressLine":"1 King William St","suburb":"Adelaide","region":"SA",
                 "isAutoApproved":false,"usePickupCode":false,"hideContact":false,
                 "slotCapacity":1,"status":"LIVE","tagIds":[],"timeslots":[]}
                """.formatted(listing.getCategory().getCategoryId(), dailyLimit);
    }

    private ResultActions create(Donor donor, String body) throws Exception {
        return mockMvc.perform(post("/api/donors/{donorId}/listings", donor.getId())
                .header("Authorization", bearer(donor.getUserId(), "donor"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions update(Donor donor, Listing listing, String body) throws Exception {
        return mockMvc.perform(put("/api/donors/{donorId}/listings/{listingId}", donor.getId(), listing.getListingId())
                .header("Authorization", bearer(donor.getUserId(), "donor"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
