package foodbank.webapp.integration;

import foodbank.webapp.api.ApiTestSupport;
import foodbank.webapp.config.ListingCoordinatesBackfill;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end distance flow with nothing stubbed inside the app: the real GeocodingService
 * calls a fake Mapbox server over HTTP, coordinates are stored in the database, and the
 * listings API computes distances from them.
 */
class DistanceIntegrationTest extends ApiTestSupport {

    private static final String TOKEN = "pk.test-token";
    private static final FakeMapboxServer mapbox = new FakeMapboxServer();

    @DynamicPropertySource
    static void useFakeMapbox(DynamicPropertyRegistry registry) {
        registry.add("mapbox.base-url", mapbox::baseUrl);
        registry.add("mapbox.access-token", () -> TOKEN);
    }

    @AfterAll
    static void stopServer() {
        mapbox.stop();
    }

    @Autowired
    private ListingCoordinatesBackfill coordinatesBackfill;

    private Donor donor;
    private String donorToken;

    @BeforeEach
    void setUp() {
        mapbox.reset();
        donor = createDonor();
        donorToken = bearer(donor.getUserId(), "donor");
    }

    private Integer createListing(String name, String addressLine, String suburb) throws Exception {
        Integer categoryId = categoryRepository.findByNameIgnoreCase("Produce").orElseThrow().getCategoryId();
        String response = mockMvc.perform(post("/api/donors/{donorId}/listings", donor.getId())
                        .header("Authorization", donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "categoryId": %d, "quantity": 5,
                                 "addressLine": "%s", "suburb": "%s", "region": "SA",
                                 "isAutoApproved": false, "usePickupCode": false, "status": "LIVE",
                                 "tagIds": [], "timeslots": []}
                                """.formatted(name, categoryId, addressLine, suburb)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Integer.valueOf(response.replaceAll(".*\"listingId\"\\s*:\\s*(\\d+).*", "$1"));
    }

    @Test
    void sendsAddressAndTokenToMapbox() throws Exception {
        createListing("It request check", "16 Moseley St", "Glenelg");

        assertThat(mapbox.requests()).hasSize(1);
        Map<String, String> request = mapbox.requests().get(0);
        assertThat(request).containsEntry("q", "16 Moseley St, Glenelg, SA")
                .containsEntry("country", "au")
                .containsEntry("limit", "1")
                .containsEntry("access_token", TOKEN);
    }

    @Test
    void donorCreatesListingsAndRecipientSeesThemNearestFirst() throws Exception {
        mapbox.respondWith("16 Moseley St, Glenelg, SA", -34.9806, 138.5136);
        mapbox.respondWith("1 Victoria Pde, Victor Harbor, SA", -35.5522, 138.6178);
        createListing("It flow far", "1 Victoria Pde", "Victor Harbor");
        createListing("It flow near", "16 Moseley St", "Glenelg");

        Recipient recipient = createRecipient();
        String recipientToken = recipientToken(recipient);
        mockMvc.perform(put("/api/recipients/me/location").header("Authorization", recipientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressLine": "1 King William St", "suburb": "Adelaide", "region": "SA",
                                 "latitude": -34.9285, "longitude": 138.6007}
                                """))
                .andExpect(status().isOk());

        // No location in the request: the recipient's saved one is used
        mockMvc.perform(get("/api/listings").param("foodName", "it flow")
                        .header("Authorization", recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].foodName", contains("It flow near", "It flow far")))
                .andExpect(jsonPath("$.content[0].distanceKm", closeTo(9.8, 0.3)))
                .andExpect(jsonPath("$.content[1].distanceKm", closeTo(69.4, 0.5)));
    }

    @Test
    void recipientLocationTypedWithoutCoordinatesIsGeocoded() throws Exception {
        mapbox.respondWith("16 Moseley St, Glenelg, SA", -34.9806, 138.5136);
        mapbox.respondWith("1 King William St, Adelaide, SA", -34.9285, 138.6007);
        createListing("It typed location", "16 Moseley St", "Glenelg");
        Recipient recipient = createRecipient();
        String recipientToken = recipientToken(recipient);

        mockMvc.perform(put("/api/recipients/me/location").header("Authorization", recipientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressLine": "1 King William St", "suburb": "Adelaide", "region": "SA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(-34.9285));

        mockMvc.perform(get("/api/listings").param("foodName", "it typed location")
                        .header("Authorization", recipientToken))
                .andExpect(jsonPath("$.content[0].distanceKm", closeTo(9.8, 0.3)));
    }

    @Test
    void mapboxErrorStillSavesListingWithoutDistance() throws Exception {
        mapbox.failWith("16 Moseley St, Glenelg, SA", 500);
        Integer listingId = createListing("It mapbox down", "16 Moseley St", "Glenelg");

        Listing saved = listingRepository.findById(listingId).orElseThrow();
        assertThat(saved.getLatitude()).isNull();

        mockMvc.perform(get("/api/listings").param("foodName", "it mapbox down")
                        .param("latitude", "-34.9285").param("longitude", "138.6007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].distanceKm").value(nullValue()));
    }

    @Test
    void unknownAddressIsSavedWithoutCoordinates() throws Exception {
        Integer listingId = createListing("It unknown address", "Nowhere Road", "Atlantis");

        assertThat(mapbox.requests()).hasSize(1);
        assertThat(listingRepository.findById(listingId).orElseThrow().getLatitude()).isNull();
    }

    @Test
    void backfillAddsCoordinatesToOldListings() throws Exception {
        mapbox.respondWith("16 Moseley St, Glenelg, SA", -34.9806, 138.5136);
        Listing old = createLiveListing(donor, "It backfill old");
        old.setAddressLine("16 Moseley St");
        old.setSuburb("Glenelg");
        listingRepository.save(old);
        Listing unknown = createLiveListing(donor, "It backfill unknown");
        unknown.setAddressLine("Nowhere Road");
        listingRepository.save(unknown);

        coordinatesBackfill.run(null);

        Listing updated = listingRepository.findById(old.getListingId()).orElseThrow();
        assertThat(updated.getLatitude()).isEqualTo(-34.9806);
        assertThat(updated.getLongitude()).isEqualTo(138.5136);
        assertThat(listingRepository.findById(unknown.getListingId()).orElseThrow().getLatitude()).isNull();
    }

    @Test
    void backfillSkipsListingsThatAlreadyHaveCoordinates() throws Exception {
        Listing located = createLiveListing(donor, "It backfill located");
        located.setAddressLine("16 Moseley St");
        located.setLatitude(-34.0);
        located.setLongitude(138.0);
        listingRepository.save(located);

        coordinatesBackfill.run(null);

        assertThat(mapbox.requests()).isEmpty();
        assertThat(listingRepository.findById(located.getListingId()).orElseThrow().getLatitude()).isEqualTo(-34.0);
    }
}
