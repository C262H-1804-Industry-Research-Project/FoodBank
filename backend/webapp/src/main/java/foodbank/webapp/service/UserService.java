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
import org.springframework.security.crypto.password.PasswordEncoder;
import foodbank.webapp.dto.RecipientRegisterRequest;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.repositories.RecipientRepository;
import foodbank.webapp.dto.RecipientResponse;
import foodbank.webapp.exceptions.ConflictException;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecipientRepository recipientRepository;

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
        if (emailAlreadyRegisteredAsDonor(email)) {
            throw new ConflictException(
                    "This email is already registered as a donor"
            );
        }

        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
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
    @Transactional
    public RecipientResponse createRecipient(RecipientRegisterRequest request) {
        requireText(request.name(), "name");
        requireText(request.phone(), "phone");
        requireText(request.email(), "email");
        requireText(request.password(), "password");

        String email = request.email().trim();

        if (emailAlreadyRegisteredAsRecipient(email)) {
            throw new ConflictException(
                    "This email is already registered as a recipient"
            );}

        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone().trim())
                .status("PENDING")
                .build());

        String orgName = blankToNull(request.orgName());

        String approvalStatus =
                orgName == null ? null : "PENDING";

        Recipient recipient = recipientRepository.save(
                Recipient.builder()
                        .userId(user.getUserId())
                        .name(request.name().trim())
                        .orgName(orgName)
                        .approvalStatus(approvalStatus)
                        .claimLimit(100)
                        .build()
        );
        return new RecipientResponse(
                recipient.getRecipientId(),
                user.getUserId(),
                user.getEmail(),
                recipient.getName(),
                recipient.getApprovalStatus()
        );
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
    private boolean emailAlreadyRegisteredAsDonor(String email) {
        return userRepository
                .findAllByEmailIgnoreCase(email)
                .stream()
                .anyMatch(user ->
                        donorRepository.existsByUserId(user.getUserId())
                );
    }

    private boolean emailAlreadyRegisteredAsRecipient(String email) {
        return userRepository
                .findAllByEmailIgnoreCase(email)
                .stream()
                .anyMatch(user ->
                        recipientRepository.existsByUserId(user.getUserId())
                );
    }
}
