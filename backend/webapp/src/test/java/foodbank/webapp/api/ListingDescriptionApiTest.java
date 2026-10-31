package foodbank.webapp.api;

import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ListingDescriptionApiTest extends ApiTestSupport {

    @ParameterizedTest
    @ValueSource(ints = {300, 1000, 2000})
    void creatingAndEditingPersistLongDescriptions(int length) throws Exception {
        Donor donor = createDonor();
        Listing original = createLiveListing(donor, "Original bread");
        String description = "a".repeat(length);
        String body = listingBody(original, description);

        create(donor, body).andExpect(status().isCreated());
        // Force the database write so this checks column capacity, not just the DTO.
        listingRepository.flush();
        assertThat(listingRepository.findByDonorId(donor.getId()))
                .anySatisfy(saved -> {
                    assertThat(saved.getName()).isEqualTo("Updated bread");
                    assertThat(saved.getDescription()).isEqualTo(description);
                });

        update(donor, original, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value(description));
        listingRepository.flush();
        assertThat(listingRepository.findById(original.getListingId()).orElseThrow().getDescription())
                .isEqualTo(description);
    }

    @Test
    void creatingAndEditingRejectOverlongDescriptionsAsBadRequests() throws Exception {
        Donor donor = createDonor();
        Listing original = createLiveListing(donor, "Original bread");
        String body = listingBody(original, "a".repeat(2001));
        long originalCount = listingRepository.count();

        create(donor, body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Storage instructions / description must be 2000 characters or fewer."));
        update(donor, original, body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Storage instructions / description must be 2000 characters or fewer."));

        assertThat(listingRepository.count()).isEqualTo(originalCount);
        assertThat(listingRepository.findById(original.getListingId()).orElseThrow().getName())
                .isEqualTo("Original bread");
    }

    private String listingBody(Listing listing, String description) {
        return """
                {"name":"Updated bread","description":"%s","categoryId":%d,"quantity":10,
                 "addressLine":"1 King William St","suburb":"Adelaide","region":"SA",
                 "isAutoApproved":false,"usePickupCode":false,"hideContact":false,
                 "slotCapacity":1,"status":"LIVE","tagIds":[],"timeslots":[]}
                """.formatted(description, listing.getCategory().getCategoryId());
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
