package foodbank.webapp.api;

import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.entity.Timeslot;
import foodbank.webapp.entity.User;
import foodbank.webapp.repositories.CategoryRepository;
import foodbank.webapp.repositories.DonorRepository;
import foodbank.webapp.repositories.ListingRepository;
import foodbank.webapp.repositories.RecipientRepository;
import foodbank.webapp.repositories.TimeslotRepository;
import foodbank.webapp.repositories.UserRepository;
import foodbank.webapp.service.AuthTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for API tests. Each test runs against the full app (security,
 * controllers, services) on an in-memory H2 database, and is rolled back
 * afterwards so tests don't see each other's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class ApiTestSupport {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected AuthTokenService authTokenService;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected UserRepository userRepository;
    @Autowired protected RecipientRepository recipientRepository;
    @Autowired protected DonorRepository donorRepository;
    @Autowired protected CategoryRepository categoryRepository;
    @Autowired protected ListingRepository listingRepository;
    @Autowired protected TimeslotRepository timeslotRepository;

    protected static final String PASSWORD = "Password123!";

    protected User createUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .phone("0400000000")
                .status("ACTIVE")
                .build());
    }

    protected Recipient createRecipient() {
        User user = createUser("recipient-" + UUID.randomUUID() + "@example.com");
        return recipientRepository.save(Recipient.builder()
                .userId(user.getUserId())
                .name("Test Recipient")
                .build());
    }

    protected Donor createDonor() {
        User user = createUser("donor-" + UUID.randomUUID() + "@example.com");
        return donorRepository.save(Donor.builder()
                .userId(user.getUserId())
                .orgName("Test Donor")
                .addressLine("1 Test Street")
                .suburb("Adelaide")
                .region("SA")
                .approvalStatus("APPROVED")
                .hideContact(false)
                .build());
    }

    protected Listing createLiveListing(Donor donor, String name) {
        return listingRepository.save(Listing.builder()
                .donor(donor)
                .name(name)
                .category(categoryRepository.findByNameIgnoreCase("Produce").orElseThrow())
                .quantity(10f)
                .expiryDate(LocalDate.now().plusDays(3))
                .suburb("Adelaide")
                .region("SA")
                .status("LIVE")
                .isAutoApproved(false)
                .usePickupCode(false)
                .build());
    }

    protected Timeslot createTimeslot(Listing listing, int remainingSlot) {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        return timeslotRepository.save(Timeslot.builder()
                .listing(listing)
                .startTime(start)
                .endTime(start.plusHours(1))
                .maxRecipients(Math.max(remainingSlot, 1))
                .remainingSlot(remainingSlot)
                .build());
    }

    protected String bearer(Integer userId, String role) {
        return "Bearer " + authTokenService.createToken(userId, role);
    }

    protected String recipientToken(Recipient recipient) {
        return bearer(recipient.getUserId(), "recipient");
    }
}
