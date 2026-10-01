package foodbank.webapp.service;

import foodbank.webapp.dto.LoginRequest;
import foodbank.webapp.dto.LoginResponse;
import foodbank.webapp.dto.LoginUserResponse;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.User;
import foodbank.webapp.entity.Admin;
import foodbank.webapp.exceptions.UnauthorizedException;
import foodbank.webapp.repositories.DonorRepository;
import foodbank.webapp.repositories.RecipientRepository;
import foodbank.webapp.repositories.UserRepository;
import foodbank.webapp.repositories.AdminRepository;
import foodbank.webapp.exceptions.ForbiddenException;
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

        if (!Set.of("recipient", "admin", "donor").contains(role)) {
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
                userRepository.findAllByEmailIgnoreCase(email);

        for (User user : users) {
            boolean correctRole = switch (role) {
                case "donor" ->
                        donorRepository.existsByUserId(user.getUserId());

                case "recipient" ->
                        recipientRepository.existsByUserId(user.getUserId());

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
                        .findByUserId(user.getUserId())
                        .orElseThrow();
                name = donor.getOrgName();
                donorId = donor.getId();
            } else {
                name = recipientRepository
                        .findByUserId(user.getUserId())
                        .orElseThrow()
                        .getName();
            }

            String accessToken =
                    authTokenService.createToken(
                            user.getUserId(),
                            role
                    );

            return new LoginResponse(
                    accessToken,
                    "Bearer",
                    new LoginUserResponse(name, role, donorId)
            );
        }

        throw new UnauthorizedException(
                "Invalid email or password"
        );
    }
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
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

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Field " + field + " is missing/invalid"
            );
        }
    }
}
