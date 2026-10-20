package foodbank.webapp.service;

import foodbank.webapp.dto.CreateDonorRequest;
import foodbank.webapp.dto.AddRoleRequest;
import foodbank.webapp.dto.AddRoleResponse;
import foodbank.webapp.dto.DonorResponse;
import foodbank.webapp.dto.DualRoleRegisterRequest;
import foodbank.webapp.dto.DualRoleRegisterResponse;
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
import foodbank.webapp.exceptions.ForbiddenException;

import java.util.NoSuchElementException;

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
                .status("ACTIVE")
                .build());
        Donor donor = donorRepository.save(Donor.builder()
                .userId(user.getUserId())
                .orgName(request.orgName().trim())
                .addressLine(request.addressLine().trim())
                .suburb(request.suburb().trim())
                .region(request.region().trim())
                .approvalStatus("APPROVED")
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
                .status("ACTIVE")
                .build());

        String orgName = blankToNull(request.orgName());

        String approvalStatus =
                orgName == null ? null : "APPROVED";

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
    @Transactional
    public DualRoleRegisterResponse createDualRoleUser(
            DualRoleRegisterRequest request
    ) {
        requireText(request.email(), "email");
        requireText(request.password(), "password");
        requireText(request.phone(), "phone");

        requireText(request.donorOrgName(), "donorOrgName");
        requireText(request.addressLine(), "addressLine");
        requireText(request.suburb(), "suburb");
        requireText(request.region(), "region");

        requireText(request.recipientName(), "recipientName");

        String email = request.email().trim();

        // This endpoint is only for a completely new account.
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(
                    "This email is already registered"
            );
        }

        // Create only one User and encode the password only once.
        User user = userRepository.save(
                User.builder()
                        .email(email)
                        .password(
                                passwordEncoder.encode(
                                        request.password()
                                )
                        )
                        .phone(request.phone().trim())
                        .status("ACTIVE")
                        .build()
        );

        // Create the Donor profile with the same userId.
        Donor donor = donorRepository.save(
                Donor.builder()
                        .userId(user.getUserId())
                        .orgName(
                                request.donorOrgName().trim()
                        )
                        .addressLine(
                                request.addressLine().trim()
                        )
                        .suburb(request.suburb().trim())
                        .region(request.region().trim())
                        .approvalStatus("APPROVED")
                        .hideContact(Boolean.FALSE)
                        .build()
        );

        String recipientOrgName =
                blankToNull(request.recipientOrgName());

        String recipientApprovalStatus =
                recipientOrgName == null
                        ? null
                        : "APPROVED";

        // Create the Recipient profile with the same userId.
        Recipient recipient = recipientRepository.save(
                Recipient.builder()
                        .userId(user.getUserId())
                        .name(
                                request.recipientName().trim()
                        )
                        .orgName(recipientOrgName)
                        .approvalStatus(
                                recipientApprovalStatus
                        )
                        .claimLimit(100)
                        .build()
        );

        return new DualRoleRegisterResponse(
                user.getUserId(),
                donor.getId(),
                recipient.getRecipientId(),
                user.getEmail(),
                donor.getApprovalStatus(),
                recipient.getApprovalStatus()
        );
    }

    @Transactional
    public AddRoleResponse addRole(
            Integer userId,
            AddRoleRequest request
    ) {
        requireText(request.targetRole(), "targetRole");

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "User does not exist"
                        )
                );

        if ("BANNED".equalsIgnoreCase(user.getStatus())) {
            throw new ForbiddenException("Account is banned");
        }

        String targetRole = request.targetRole()
                .trim()
                .toLowerCase();

        if (targetRole.equals("donor")) {
            if (donorRepository.existsByUserId(userId)) {
                throw new ConflictException(
                        "Donor profile already exists"
                );
            }

            requireText(request.donorOrgName(), "donorOrgName");
            requireText(request.addressLine(), "addressLine");
            requireText(request.suburb(), "suburb");
            requireText(request.region(), "region");

            Donor donor = donorRepository.save(
                    Donor.builder()
                            .userId(userId)
                            .orgName(request.donorOrgName().trim())
                            .addressLine(request.addressLine().trim())
                            .suburb(request.suburb().trim())
                            .region(request.region().trim())
                            .approvalStatus("APPROVED")
                            .hideContact(Boolean.FALSE)
                            .build()
            );

            return new AddRoleResponse(
                    "Donor profile created successfully",
                    "donor",
                    donor.getId()
            );
        }

        if (targetRole.equals("recipient")) {
            if (recipientRepository.existsByUserId(userId)) {
                throw new ConflictException(
                        "Recipient profile already exists"
                );
            }

            requireText(request.recipientName(), "recipientName");

            String orgName = blankToNull(
                    request.recipientOrgName()
            );

            Recipient recipient = recipientRepository.save(
                    Recipient.builder()
                            .userId(userId)
                            .name(request.recipientName().trim())
                            .orgName(orgName)
                            .approvalStatus(
                                    orgName == null
                                            ? null
                                            : "APPROVED"
                            )
                            .claimLimit(100)
                            .build()
            );

            return new AddRoleResponse(
                    "Recipient profile created successfully",
                    "recipient",
                    recipient.getRecipientId()
            );
        }

        throw new IllegalArgumentException(
                "targetRole must be donor or recipient"
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
