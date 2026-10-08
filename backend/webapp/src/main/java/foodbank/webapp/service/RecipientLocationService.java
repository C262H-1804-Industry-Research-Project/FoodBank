package foodbank.webapp.service;

import foodbank.webapp.dto.RecipientLocation;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.exceptions.ForbiddenException;
import foodbank.webapp.repositories.RecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecipientLocationService {
    private final RecipientRepository recipientRepository;
    private final GeocodingService geocodingService;

    public Optional<RecipientLocation> getLocation(Integer userId) {
        Recipient recipient = findRecipientForUser(userId);
        if (recipient.getAddressLine() == null && recipient.getSuburb() == null) {
            return Optional.empty();
        }
        return Optional.of(toLocation(recipient));
    }

    @Transactional
    public RecipientLocation saveLocation(Integer userId, RecipientLocation location) {
        if (location == null) throw new IllegalArgumentException("request body is required");
        String addressLine = blankToNull(location.addressLine());
        String suburb = blankToNull(location.suburb());
        String region = blankToNull(location.region());
        if (addressLine == null && suburb == null) {
            throw new IllegalArgumentException("addressLine or suburb is required");
        }

        Double latitude = location.latitude();
        Double longitude = location.longitude();
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("latitude and longitude must be given together");
        }
        if (latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("latitude or longitude is out of range");
        }
        // Coordinates normally come from the address autocomplete; look them up if they are missing
        if (latitude == null) {
            var coordinates = geocodingService.geocode(addressLine == null ? suburb : addressLine, suburb, region);
            latitude = coordinates.map(GeocodingService.Coordinates::latitude).orElse(null);
            longitude = coordinates.map(GeocodingService.Coordinates::longitude).orElse(null);
        }

        Recipient recipient = findRecipientForUser(userId);
        recipient.setAddressLine(addressLine);
        recipient.setSuburb(suburb);
        recipient.setRegion(region);
        recipient.setLatitude(latitude);
        recipient.setLongitude(longitude);
        return toLocation(recipientRepository.save(recipient));
    }

    @Transactional
    public void clearLocation(Integer userId) {
        Recipient recipient = findRecipientForUser(userId);
        recipient.setAddressLine(null);
        recipient.setSuburb(null);
        recipient.setRegion(null);
        recipient.setLatitude(null);
        recipient.setLongitude(null);
        recipientRepository.save(recipient);
    }

    /** The saved coordinates of a recipient, if they have saved a location that could be located. */
    public Optional<GeocodingService.Coordinates> findSavedCoordinates(Integer userId) {
        return recipientRepository.findByUserId(userId)
                .filter(recipient -> recipient.getLatitude() != null && recipient.getLongitude() != null)
                .map(recipient -> new GeocodingService.Coordinates(recipient.getLatitude(), recipient.getLongitude()));
    }

    private Recipient findRecipientForUser(Integer userId) {
        return recipientRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Recipient profile does not exist"));
    }

    private static RecipientLocation toLocation(Recipient recipient) {
        return new RecipientLocation(
                recipient.getAddressLine(),
                recipient.getSuburb(),
                recipient.getRegion(),
                recipient.getLatitude(),
                recipient.getLongitude()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
