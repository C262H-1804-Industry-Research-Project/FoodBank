package foodbank.webapp.service;

import foodbank.webapp.dto.AdminResponse;
import foodbank.webapp.dto.CreateAdminRequest;
import foodbank.webapp.entity.Admin;
import foodbank.webapp.exceptions.ConflictException;
import foodbank.webapp.repositories.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminResponse createAdmin(CreateAdminRequest request) {
        requireText(request.name(), "name");
        requireText(request.email(), "email");
        requireText(request.password(), "password");

        String email = request.email().trim();
        if (adminRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException(
                    "This email is already registered as an admin"
            );
        }

        Admin admin = adminRepository.save(Admin.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .build());

        return new AdminResponse(admin.getAdminId(), admin.getName(), admin.getEmail());
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Field " + field + " is missing/invalid"
            );
        }
    }
}
