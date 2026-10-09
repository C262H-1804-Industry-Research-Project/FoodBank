package foodbank.webapp.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeocodingServiceTest {

    @Test
    void distanceBetweenKnownPlaces() {
        // Adelaide CBD to Glenelg is about 9.8 km; Adelaide to Melbourne about 654 km
        assertThat(GeocodingService.distanceKm(-34.9285, 138.6007, -34.9806, 138.5136)).isCloseTo(9.8, within(0.3));
        assertThat(GeocodingService.distanceKm(-34.9285, 138.6007, -37.8136, 144.9631)).isCloseTo(654, within(5.0));
    }

    @Test
    void distanceToSamePointIsZero() {
        assertThat(GeocodingService.distanceKm(-34.9285, 138.6007, -34.9285, 138.6007)).isZero();
    }

    @Test
    void distanceIsSymmetric() {
        double there = GeocodingService.distanceKm(-34.9285, 138.6007, -35.5522, 138.6178);
        double back = GeocodingService.distanceKm(-35.5522, 138.6178, -34.9285, 138.6007);
        assertThat(there).isCloseTo(back, within(1e-9));
    }

    @Test
    void lookupsAreSkippedWithoutToken() {
        GeocodingService service = new GeocodingService("  ", "http://127.0.0.1:1");

        assertThat(service.isEnabled()).isFalse();
        assertThat(service.geocode("16 Moseley St", "Glenelg", "SA")).isEmpty();
    }

    @Test
    void blankAddressIsNotLookedUp() {
        GeocodingService service = new GeocodingService("pk.test", "http://127.0.0.1:1");

        assertThat(service.geocode("  ", "Glenelg", "SA")).isEmpty();
    }

    @Test
    void unreachableMapboxReturnsEmptyInsteadOfThrowing() {
        // Port 1 refuses connections, like Mapbox being unreachable
        GeocodingService service = new GeocodingService("pk.test", "http://127.0.0.1:1");

        assertThat(service.geocode("16 Moseley St", "Glenelg", "SA")).isEmpty();
    }
}
