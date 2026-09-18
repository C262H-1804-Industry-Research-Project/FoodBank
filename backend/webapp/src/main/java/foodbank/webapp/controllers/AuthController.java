package foodbank.webapp.controllers;

import foodbank.webapp.dto.LoginRequest;
import foodbank.webapp.dto.LoginResponse;
import foodbank.webapp.dto.RecipientRegisterRequest;
import foodbank.webapp.dto.RecipientResponse;
import foodbank.webapp.service.AuthService;
import foodbank.webapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

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
}