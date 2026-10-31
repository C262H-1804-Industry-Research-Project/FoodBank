package foodbank.webapp.api;

import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ListingDatesApiTest extends ApiTestSupport {

    static Stream<Arguments> invalidDates() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(LocalDate.of(1970, 1, 1), null, "Expiry date must be later than today."),
                Arguments.of(today.minusDays(1), null, "Expiry date must be later than today."),
                Arguments.of(today, null, "Expiry date must be later than today."),
                Arguments.of(null, LocalDate.of(1970, 1, 1), "Latest collection deadline must be later than today."),
                Arguments.of(today.plusDays(3), today.minusDays(1), "Latest collection deadline must be later than today."),
                Arguments.of(today.plusDays(3), today, "Latest collection deadline must be later than today."),
                Arguments.of(today.plusDays(1), today.plusDays(2), "collectionDeadline cannot be after expiryDate")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidDates")
    void creatingAndEditingRejectInvalidDates(
            LocalDate expiryDate, LocalDate collectionDeadline, String error
    ) throws Exception {
        Donor donor = createDonor();
        Listing listing = createLiveListing(donor, "Original bread");
        LocalDate originalExpiry = listing.getExpiryDate();
        long originalCount = listingRepository.count();
        String body = listingBody(listing, expiryDate, collectionDeadline);

        create(donor, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(error));
        update(donor, listing, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(error));

        assertThat(listingRepository.count()).isEqualTo(originalCount);
        Listing unchanged = listingRepository.findById(listing.getListingId()).orElseThrow();
        assertThat(unchanged.getExpiryDate()).isEqualTo(originalExpiry);
        assertThat(unchanged.getName()).isEqualTo("Original bread");
    }

    static Stream<Arguments> validDates() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return Stream.of(
                Arguments.of(tomorrow.plusDays(2), tomorrow),
                Arguments.of(tomorrow, tomorrow),
                Arguments.of(tomorrow, null),
                Arguments.of(null, tomorrow),
                Arguments.of(null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("validDates")
    void creatingAndEditingAcceptFutureOrOmittedDates(
            LocalDate expiryDate, LocalDate collectionDeadline
    ) throws Exception {
        Donor donor = createDonor();
        Listing listing = createLiveListing(donor, "Original bread");
        String body = listingBody(listing, expiryDate, collectionDeadline);

        create(donor, body).andExpect(status().isCreated());
        update(donor, listing, body).andExpect(status().isOk());

        Listing updated = listingRepository.findById(listing.getListingId()).orElseThrow();
        assertThat(updated.getExpiryDate()).isEqualTo(expiryDate);
        assertThat(updated.getCollectionDeadline()).isEqualTo(collectionDeadline);
    }

    private String listingBody(Listing listing, LocalDate expiryDate, LocalDate collectionDeadline) {
        return """
                {"name":"Updated bread","categoryId":%d,"quantity":10,
                 "expiryDate":%s,"collectionDeadline":%s,
                 "addressLine":"1 King William St","suburb":"Adelaide","region":"SA",
                 "isAutoApproved":false,"usePickupCode":false,"hideContact":false,
                 "slotCapacity":1,"status":"LIVE","tagIds":[],"timeslots":[]}
                """.formatted(listing.getCategory().getCategoryId(), jsonDate(expiryDate), jsonDate(collectionDeadline));
    }

    private static String jsonDate(LocalDate date) {
        return date == null ? "null" : "\"" + date + "\"";
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
