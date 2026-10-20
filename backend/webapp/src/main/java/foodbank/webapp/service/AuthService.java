package foodbank.webapp.service;

import foodbank.webapp.dto.LoginRequest;
import foodbank.webapp.dto.LoginResponse;
import foodbank.webapp.dto.LoginUserResponse;
import foodbank.webapp.entity.Admin;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Recipient;
import foodbank.webapp.entity.User;
import foodbank.webapp.exceptions.ForbiddenException;
import foodbank.webapp.exceptions.UnauthorizedException;
import foodbank.webapp.repositories.AdminRepository;
import foodbank.webapp.repositories.DonorRepository;
import foodbank.webapp.repositories.RecipientRepository;
import foodbank.webapp.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final RecipientRepository recipientRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    public LoginResponse login(LoginRequest request) {
        requireText(request.email(), "email");
        requireText(request.password(), "password");
        requireText(request.role(), "role");

        String email = request.email().trim();
        String role = request.role().trim().toLowerCase();

        if (!Set.of(
                "recipient",
                "admin",
                "donor"
        ).contains(role)) {
            throw new IllegalArgumentException(
                    "role must be one of recipient, admin, donor"
            );
        }

        if (role.equals("admin")) {
            Admin admin = adminRepository
                    .findByEmailIgnoreCase(email)
                    .orElseThrow(() ->
                            new UnauthorizedException(
                                    "Invalid email or password"
                            )
                    );

            if (!passwordEncoder.matches(
                    request.password(),
                    admin.getPassword()
            )) {
                throw new UnauthorizedException(
                        "Invalid email or password"
                );
            }

            String accessToken =
                    authTokenService.createToken(
                            admin.getAdminId(),
                            "admin"
                    );

            return new LoginResponse(
                    accessToken,
                    "Bearer",
                    new LoginUserResponse(
                            admin.getName(),
                            "admin",
                            null
                    )
            );
        }

        List<User> users =
                userRepository.findAllByEmailIgnoreCase(
                        email
                );

        for (User user : users) {
            boolean correctRole = switch (role) {
                case "donor" ->
                        donorRepository.existsByUserId(
                                user.getUserId()
                        );

                case "recipient" ->
                        recipientRepository.existsByUserId(
                                user.getUserId()
                        );

                default -> false;
            };

            if (!correctRole) {
                continue;
            }

            if (!passwordEncoder.matches(
                    request.password(),
                    user.getPassword()
            )) {
                continue;
            }

            if ("BANNED".equalsIgnoreCase(
                    user.getStatus()
            )) {
                throw new ForbiddenException(
                        "Account is banned"
                );
            }

            String name;
            Integer donorId = null;

            if (role.equals("donor")) {
                Donor donor = donorRepository
                        .findByUserId(
                                user.getUserId()
                        )
                        .orElseThrow();

                name = donor.getOrgName();
                donorId = donor.getId();
            } else {
                Recipient recipient =
                        recipientRepository
                                .findByUserId(
                                        user.getUserId()
                                )
                                .orElseThrow();

                name = recipient.getName();
            }

            String accessToken =
                    authTokenService.createToken(
                            user.getUserId(),
                            role
                    );

            return new LoginResponse(
                    accessToken,
                    "Bearer",
                    new LoginUserResponse(
                            name,
                            role,
                            donorId
                    )
            );
        }

        throw new UnauthorizedException(
                "Invalid email or password"
        );
    }

    public LoginResponse switchRole(
            Integer userId,
            String targetRoleValue,
            String authorizationHeader
    ) {
        requireText(
                targetRoleValue,
                "targetRole"
        );

        if (authorizationHeader == null
                || !authorizationHeader.startsWith(
                "Bearer "
        )) {
            throw new UnauthorizedException(
                    "User not logged in"
            );
        }

        String currentToken = authorizationHeader
                .substring(7)
                .trim();

        if (currentToken.isBlank()
                || !authTokenService.isActive(
                currentToken
        )) {
            throw new UnauthorizedException(
                    "User not logged in"
            );
        }

        String targetRole = targetRoleValue
                .trim()
                .toLowerCase();

        if (!Set.of(
                "donor",
                "recipient"
        ).contains(targetRole)) {
            throw new IllegalArgumentException(
                    "targetRole must be donor or recipient"
            );
        }

        String currentRole = authTokenService
                .getClaims(currentToken)
                .get(
                        "role",
                        String.class
                );

        if (targetRole.equalsIgnoreCase(
                currentRole
        )) {
            throw new IllegalArgumentException(
                    "User is already using the "
                            + targetRole
                            + " role"
            );
        }

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "User does not exist"
                        )
                );

        if ("BANNED".equalsIgnoreCase(
                user.getStatus()
        )) {
            throw new ForbiddenException(
                    "Account is banned"
            );
        }

        String name;
        Integer donorId = null;

        if (targetRole.equals("donor")) {
            Donor donor = donorRepository
                    .findByUserId(userId)
                    .orElseThrow(() ->
                            new ForbiddenException(
                                    "Donor profile does not exist"
                            )
                    );

            requireProfileAvailable(
                    donor.getApprovalStatus(),
                    "Donor"
            );

            name = donor.getOrgName();
            donorId = donor.getId();
        } else {
            Recipient recipient =
                    recipientRepository
                            .findByUserId(userId)
                            .orElseThrow(() ->
                                    new ForbiddenException(
                                            "Recipient profile does not exist"
                                    )
                            );

            requireProfileAvailable(
                    recipient.getApprovalStatus(),
                    "Recipient"
            );

            name = recipient.getName();
        }

        String newToken =
                authTokenService.createToken(
                        userId,
                        targetRole
                );

        // The previous JWT can no longer be used.
        authTokenService.revokeToken(
                currentToken
        );

        return new LoginResponse(
                newToken,
                "Bearer",
                new LoginUserResponse(
                        name,
                        targetRole,
                        donorId
                )
        );
    }

    public void logout(
            String authorizationHeader
    ) {
        if (authorizationHeader == null
                || !authorizationHeader.startsWith(
                "Bearer "
        )) {
            throw new UnauthorizedException(
                    "User not logged in"
            );
        }

        String token = authorizationHeader
                .substring(7)
                .trim();

        if (token.isBlank()
                || !authTokenService.isActive(token)) {
            throw new UnauthorizedException(
                    "User not logged in"
            );
        }

        authTokenService.revokeToken(token);
    }

    private static void requireProfileAvailable(
            String approvalStatus,
            String roleName
    ) {
        if (approvalStatus != null
                && !approvalStatus.equalsIgnoreCase(
                "APPROVED"
        )) {
            throw new ForbiddenException(
                    roleName
                            + " profile is not approved"
            );
        }
    }

    private static void requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Field "
                            + field
                            + " is missing/invalid"
            );
        }
    }
}