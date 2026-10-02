package foodbank.webapp.controllers;

import foodbank.webapp.dto.*;
import foodbank.webapp.service.AuthService;
import foodbank.webapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register-donors")
    @ResponseStatus(HttpStatus.CREATED)
    public DonorResponse createDonor(@RequestBody CreateDonorRequest request) {
        return userService.createDonor(request);
    }

    @PostMapping("/register-recipients")
    public RecipientResponse registerRecipient(
            @RequestBody RecipientRegisterRequest request
    ) {
        return userService.createRecipient(request);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, String> logout(
            HttpServletRequest request
    ) {
        String authorizationHeader =
                request.getHeader("Authorization");

        authService.logout(authorizationHeader);

        return Map.of(
                "message",
                "Logout successful"
        );
    }
}