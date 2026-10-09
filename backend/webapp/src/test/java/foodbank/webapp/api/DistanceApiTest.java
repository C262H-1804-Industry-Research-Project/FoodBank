package foodbank.webapp.api;

import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.service.GeocodingService;
import foodbank.webapp.service.GeocodingService.Coordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Distance feature at the API level. Mapbox lookups are stubbed so each test controls
 * which coordinates an address resolves to.
 */
class DistanceApiTest extends ApiTestSupport {

    // Real coordinates, so expected distances can be checked against known values
    private static final Coordinates ADELAIDE_CBD = new Coordinates(-34.9285, 138.6007);
    private static final Coordinates GLENELG = new Coordinates(-34.9806, 138.5136);       // ~9.8 km from CBD
    private static final Coordinates MAWSON_LAKES = new Coordinates(-34.8106, 138.6091);  // ~13.1 km from CBD
    private static final Coordinates VICTOR_HARBOR = new Coordinates(-35.5522, 138.6178); // ~69 km from CBD

    @MockitoBean
    private GeocodingService geocodingService;

    private Donor donor;

    @BeforeEach
    void setUp() {
        donor = createDonor();
        when(geocodingService.geocode(any(), any(), any())).thenReturn(Optional.empty());
    }

    private Listing listingAt(String name, Coordinates coordinates) {
        Listing listing = createLiveListing(donor, name);
        listing.setLatitude(coordinates == null ? null : coordinates.latitude());
        listing.setLongitude(coordinates == null ? null : coordinates.longitude());
        return listingRepository.save(listing);
    }

    private String donorToken() {
        return bearer(donor.getUserId(), "donor");
    }

    // --- Browsing: GET /api/listings with a location ---

    @Test
    void sortsNearestFirstWithDistances() throws Exception {
        listingAt("Zz distance far", VICTOR_HARBOR);
        listingAt("Zz distance near", GLENELG);
        listingAt("Zz distance middle", MAWSON_LAKES);

        mockMvc.perform(get("/api/listings").param("foodName", "zz distance")
                        .param("latitude", "-34.9285").param("longitude", "138.6007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].foodName", contains(
                        "Zz distance near", "Zz distance middle", "Zz distance far")))
                .andExpect(jsonPath("$.content[0].distanceKm", closeTo(9.8, 0.3)))
                .andExpect(jsonPath("$.content[1].distanceKm", closeTo(13.1, 0.3)))
                .andExpect(jsonPath("$.content[2].distanceKm", closeTo(69.4, 0.5)));
    }

    @Test
    void listingsWithoutCoordinatesComeLastWithoutDistance() throws Exception {
        listingAt("Zz nocoords unknown", null);
        listingAt("Zz nocoords far", VICTOR_HARBOR);

        mockMvc.perform(get("/api/listings").param("foodName", "zz nocoords")
                        .param("latitude", "-34.9285").param("longitude", "138.6007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].foodName", contains("Zz nocoords far", "Zz nocoords unknown")))
                .andExpect(jsonPath("$.content[1].distanceKm").value(nullValue()));
    }

    @Test
    void sortingKeepsPaginationCorrect() throws Exception {
        listingAt("Zz paging 3", VICTOR_HARBOR);
        listingAt("Zz paging 1", GLENELG);
        listingAt("Zz paging 2", MAWSON_LAKES);

        mockMvc.perform(get("/api/listings").param("foodName", "zz paging")
                        .param("latitude", "-34.9285").param("longitude", "138.6007")
                        .param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].foodName", contains("Zz paging 3")))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void withoutLocationThereIsNoDistance() throws Exception {
        listingAt("Zz anonymous", GLENELG);

        mockMvc.perform(get("/api/listings").param("foodName", "zz anonymous"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].distanceKm").value(nullValue()));
    }

    @Test
    void distanceWorksTogetherWithSuburbFilter() throws Exception {
        Listing inGlenelg = listingAt("Zz combo glenelg", GLENELG);
        inGlenelg.setSuburb("Glenelg");
        listingRepository.save(inGlenelg);
        listingAt("Zz combo adelaide", MAWSON_LAKES);

        mockMvc.perform(get("/api/listings").param("foodName", "zz combo")
                        .param("suburbs", "Glenelg|SA")
                        .param("latitude", "-34.9285").param("longitude", "138.6007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].foodName", contains("Zz combo glenelg")))
                .andExpect(jsonPath("$.content[0].distanceKm", closeTo(9.8, 0.3)));
    }

    @Test
    void rejectsLatitudeWithoutLongitude() throws Exception {
        mockMvc.perform(get("/api/listings").param("latitude", "-34.9"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/listings").param("longitude", "138.6"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsOutOfRangeCoordinates() throws Exception {
        mockMvc.perform(get("/api/listings").param("latitude", "-91").param("longitude", "138.6"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/listings").param("latitude", "-34.9").param("longitude", "181"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void explicitLocationOverridesRecipientsSavedLocation() throws Exception {
        listingAt("Zz override", VICTOR_HARBOR);
        Recipient recipient = createRecipient();
        recipient.setSuburb("Adelaide");
        recipient.setLatitude(ADELAIDE_CBD.latitude());
        recipient.setLongitude(ADELAIDE_CBD.longitude());
        recipientRepository.save(recipient);

        // Standing in Victor Harbor itself, so the listing is ~0 km away rather than ~69 km
        mockMvc.perform(get("/api/listings").param("foodName", "zz override")
                        .param("latitude", "-35.5522").param("longitude", "138.6178")
                        .header("Authorization", recipientToken(recipient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].distanceKm", closeTo(0, 0.01)));
    }

    @Test
    void donorTokenDoesNotUseAnySavedLocation() throws Exception {
        listingAt("Zz donor browse", GLENELG);

        mockMvc.perform(get("/api/listings").param("foodName", "zz donor browse")
                        .header("Authorization", donorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].distanceKm").value(nullValue()));
    }

    // --- Recipient location: coordinates looked up when the browser sends none ---

    @Test
    void savingLocationWithoutCoordinatesLooksThemUp() throws Exception {
        when(geocodingService.geocode("Glenelg", "Glenelg", "SA")).thenReturn(Optional.of(GLENELG));
        Recipient recipient = createRecipient();

        mockMvc.perform(put("/api/recipients/me/location").header("Authorization", recipientToken(recipient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"suburb": "Glenelg", "region": "SA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(GLENELG.latitude()))
                .andExpect(jsonPath("$.longitude").value(GLENELG.longitude()));
    }

    @Test
    void savingLocationWithCoordinatesSkipsLookup() throws Exception {
        Recipient recipient = createRecipient();

        mockMvc.perform(put("/api/recipients/me/location").header("Authorization", recipientToken(recipient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"suburb": "Adelaide", "region": "SA", "latitude": -34.9285, "longitude": 138.6007}
                                """))
                .andExpect(status().isOk());

        verify(geocodingService, never()).geocode(any(), any(), any());
    }

    // --- Donor listings: pickup address is geocoded when saved ---

    private String listingBody(String addressLine, String suburb, String region) {
        return """
                {"name": "Geocoded bread", "categoryId": %d, "quantity": 5,
                 "addressLine": %s, "suburb": %s, "region": %s,
                 "isAutoApproved": false, "usePickupCode": false, "status": "LIVE",
                 "tagIds": [], "timeslots": []}
                """.formatted(produceCategoryId(), json(addressLine), json(suburb), json(region));
    }

    private static String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private Integer createListingViaApi(String body) throws Exception {
        String response = mockMvc.perform(post("/api/donors/{donorId}/listings", donor.getId())
                        .header("Authorization", donorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Integer.valueOf(response.replaceAll(".*\"listingId\"\\s*:\\s*(\\d+).*", "$1"));
    }

    private Integer produceCategoryId() {
        return categoryRepository.findByNameIgnoreCase("Produce").orElseThrow().getCategoryId();
    }

    @Test
    void creatingListingStoresPickupCoordinates() throws Exception {
        when(geocodingService.geocode("16 Moseley St", "Glenelg", "SA")).thenReturn(Optional.of(GLENELG));

        Integer listingId = createListingViaApi(listingBody("16 Moseley St", "Glenelg", "SA"));

        Listing saved = listingRepository.findById(listingId).orElseThrow();
        assertThat(saved.getLatitude()).isEqualTo(GLENELG.latitude());
        assertThat(saved.getLongitude()).isEqualTo(GLENELG.longitude());
    }

    @Test
    void blankAddressGeocodesDonorsOwnAddress() throws Exception {
        when(geocodingService.geocode("1 Test Street", "Adelaide", "SA")).thenReturn(Optional.of(ADELAIDE_CBD));

        Integer listingId = createListingViaApi(listingBody("", null, null));

        Listing saved = listingRepository.findById(listingId).orElseThrow();
        assertThat(saved.getAddressLine()).isEqualTo("1 Test Street");
        assertThat(saved.getLatitude()).isEqualTo(ADELAIDE_CBD.latitude());
    }

    @Test
    void failedLookupStillCreatesListingWithoutCoordinates() throws Exception {
        Integer listingId = createListingViaApi(listingBody("Nowhere Road", "Atlantis", "SA"));

        Listing saved = listingRepository.findById(listingId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("LIVE");
        assertThat(saved.getLatitude()).isNull();
        assertThat(saved.getLongitude()).isNull();
    }

    private String updateBody(String addressLine, String suburb, String region) {
        return """
                {"name": "Geocoded bread", "categoryId": %d, "quantity": 5,
                 "addressLine": %s, "suburb": %s, "region": %s,
                 "isAutoApproved": false, "usePickupCode": false, "hideContact": false,
                 "slotCapacity": 1, "status": "LIVE", "tagIds": [], "timeslots": []}
                """.formatted(produceCategoryId(), json(addressLine), json(suburb), json(region));
    }

    private void updateListingViaApi(Integer listingId, String body) throws Exception {
        mockMvc.perform(put("/api/donors/{donorId}/listings/{listingId}", donor.getId(), listingId)
                        .header("Authorization", donorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void changingAddressLooksUpNewCoordinates() throws Exception {
        when(geocodingService.geocode("16 Moseley St", "Glenelg", "SA")).thenReturn(Optional.of(GLENELG));
        when(geocodingService.geocode("1 Victoria Pde", "Victor Harbor", "SA")).thenReturn(Optional.of(VICTOR_HARBOR));
        Integer listingId = createListingViaApi(listingBody("16 Moseley St", "Glenelg", "SA"));

        updateListingViaApi(listingId, updateBody("1 Victoria Pde", "Victor Harbor", "SA"));

        Listing saved = listingRepository.findById(listingId).orElseThrow();
        assertThat(saved.getLatitude()).isEqualTo(VICTOR_HARBOR.latitude());
        assertThat(saved.getLongitude()).isEqualTo(VICTOR_HARBOR.longitude());
    }

    @Test
    void unchangedAddressDoesNotLookUpAgain() throws Exception {
        when(geocodingService.geocode("16 Moseley St", "Glenelg", "SA")).thenReturn(Optional.of(GLENELG));
        Integer listingId = createListingViaApi(listingBody("16 Moseley St", "Glenelg", "SA"));
        org.mockito.Mockito.clearInvocations(geocodingService);

        updateListingViaApi(listingId, updateBody("16 Moseley St", "Glenelg", "SA"));

        verify(geocodingService, never()).geocode(anyString(), anyString(), anyString());
        assertThat(listingRepository.findById(listingId).orElseThrow().getLatitude()).isEqualTo(GLENELG.latitude());
    }

    @Test
    void unchangedAddressWithoutCoordinatesIsRetried() throws Exception {
        // The first lookup fails (e.g. Mapbox was down), so the listing has no coordinates yet
        Integer listingId = createListingViaApi(listingBody("16 Moseley St", "Glenelg", "SA"));
        when(geocodingService.geocode("16 Moseley St", "Glenelg", "SA")).thenReturn(Optional.of(GLENELG));

        updateListingViaApi(listingId, updateBody("16 Moseley St", "Glenelg", "SA"));

        assertThat(listingRepository.findById(listingId).orElseThrow().getLatitude()).isEqualTo(GLENELG.latitude());
    }
}
