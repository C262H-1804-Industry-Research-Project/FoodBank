package foodbank.webapp.api;

import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecipientLocationApiTest extends ApiTestSupport {

    private static final String LOCATION_URL = "/api/recipients/me/location";
    // Adelaide CBD and Glenelg are roughly 10 km apart
    private static final String ADELAIDE_CBD = """
            {"addressLine": " 1 King William St ", "suburb": "Adelaide", "region": "SA",
             "latitude": -34.9285, "longitude": 138.6007}
            """;

    private Recipient recipient;
    private String token;

    @BeforeEach
    void setUp() {
        recipient = createRecipient();
        token = recipientToken(recipient);
    }

    private ResultActions saveLocation(String token, String body) throws Exception {
        var request = put(LOCATION_URL).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) request.header("Authorization", token);
        return mockMvc.perform(request);
    }

    @Test
    void noSavedLocationReturnsNoContent() throws Exception {
        mockMvc.perform(get(LOCATION_URL).header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void savedLocationIsReturnedLater() throws Exception {
        saveLocation(token, ADELAIDE_CBD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressLine").value("1 King William St"));

        mockMvc.perform(get(LOCATION_URL).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressLine").value("1 King William St"))
                .andExpect(jsonPath("$.suburb").value("Adelaide"))
                .andExpect(jsonPath("$.region").value("SA"))
                .andExpect(jsonPath("$.latitude").value(-34.9285))
                .andExpect(jsonPath("$.longitude").value(138.6007));
    }

    @Test
    void clearedLocationIsGone() throws Exception {
        saveLocation(token, ADELAIDE_CBD).andExpect(status().isOk());

        mockMvc.perform(delete(LOCATION_URL).header("Authorization", token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(LOCATION_URL).header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void suburbOnlyLocationIsSavedWithoutCoordinates() throws Exception {
        // Geocoding is disabled in tests (no Mapbox token), so no coordinates are found
        saveLocation(token, """
                {"suburb": "Glenelg", "region": "SA"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressLine").value(nullValue()))
                .andExpect(jsonPath("$.suburb").value("Glenelg"))
                .andExpect(jsonPath("$.latitude").value(nullValue()));
    }

    @Test
    void rejectsEmptyAddress() throws Exception {
        saveLocation(token, """
                {"addressLine": "  ", "suburb": ""}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsHalfCoordinates() throws Exception {
        saveLocation(token, """
                {"suburb": "Adelaide", "latitude": -34.9}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsOutOfRangeCoordinates() throws Exception {
        saveLocation(token, """
                {"suburb": "Adelaide", "latitude": -134.9, "longitude": 138.6}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresRecipientToken() throws Exception {
        saveLocation(null, ADELAIDE_CBD).andExpect(status().isUnauthorized());

        Donor donor = createDonor();
        saveLocation(bearer(donor.getUserId(), "donor"), ADELAIDE_CBD).andExpect(status().isForbidden());
    }

    @Test
    void listingsUseSavedLocationForDistance() throws Exception {
        Listing listing = createLiveListing(createDonor(), "Glenelg distance pies");
        listing.setLatitude(-34.9806);
        listing.setLongitude(138.5136);
        listingRepository.save(listing);
        saveLocation(token, ADELAIDE_CBD).andExpect(status().isOk());

        mockMvc.perform(get("/api/listings").param("foodName", "glenelg distance pies")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].distanceKm", closeTo(9.8, 0.5)));
    }

    @Test
    void listingsWithoutSavedLocationHaveNoDistance() throws Exception {
        Listing listing = createLiveListing(createDonor(), "No origin pies");
        listing.setLatitude(-34.9806);
        listing.setLongitude(138.5136);
        listingRepository.save(listing);

        mockMvc.perform(get("/api/listings").param("foodName", "no origin pies")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].distanceKm").value(nullValue()));
    }
}
