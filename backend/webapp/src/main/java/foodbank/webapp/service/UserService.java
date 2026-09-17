package foodbank.webapp.service;

import foodbank.webapp.dto.CreateDonorRequest;
import foodbank.webapp.dto.DonorResponse;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.User;
import foodbank.webapp.repositories.DonorRepository;
import foodbank.webapp.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final DonorRepository donorRepository;

    @Transactional
    public DonorResponse createDonor(CreateDonorRequest request) {
        requireText(request.email(), "email");
        requireText(request.password(), "password");
        requireText(request.orgName(), "orgName");
        requireText(request.addressLine(), "addressLine");
        requireText(request.suburb(), "suburb");
        requireText(request.region(), "region");

        // check email existence
        String email = request.email().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An account already exists for this email");
        }

        User user = userRepository.save(User.builder()
                .email(email)
                .password(request.password())
                .phone(blankToNull(request.phone()))
                .status("PENDING")
                .build());
        Donor donor = donorRepository.save(Donor.builder()
                .userId(user.getUserId())
                .orgName(request.orgName().trim())
                .addressLine(request.addressLine().trim())
                .suburb(request.suburb().trim())
                .region(request.region().trim())
                .approvalStatus("PENDING")
                .hideContact(Boolean.FALSE)
                .build());

        return new DonorResponse(donor.getId(), user.getUserId(), user.getEmail(), donor.getOrgName(),
                donor.getAddressLine(), donor.getSuburb(), donor.getRegion(), donor.getApprovalStatus(), donor.getHideContact());
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
