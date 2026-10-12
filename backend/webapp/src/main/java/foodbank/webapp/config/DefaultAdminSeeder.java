package foodbank.webapp.config;

import foodbank.webapp.entity.Admin;
import foodbank.webapp.repositories.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * On startup, creates the default admin account from the admin.default.* properties
 * if it does not exist yet, so there is always an admin who can log in and create
 * other admins. Leave admin.default.email blank to turn this off.
 */
@Component
@RequiredArgsConstructor
public class DefaultAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminSeeder.class);

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default.name:admin}")
    private String name;

    @Value("${admin.default.email:}")
    private String email;

    @Value("${admin.default.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank()) {
            return;
        }
        if (password.isBlank()) {
            log.warn("admin.default.password is not set; default admin {} was not created.", email);
            return;
        }
        if (adminRepository.findByEmailIgnoreCase(email.trim()).isPresent()) {
            return;
        }
        adminRepository.save(Admin.builder()
                .name(name.trim())
                .email(email.trim())
                .password(passwordEncoder.encode(password))
                .build());
        log.info("Created default admin account {}.", email.trim());
    }
}
