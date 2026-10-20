package foodbank.webapp.controllers;

import foodbank.webapp.dto.LoginResponse;
import foodbank.webapp.dto.AddRoleRequest;
import foodbank.webapp.dto.AddRoleResponse;
import foodbank.webapp.dto.SwitchRoleRequest;
import foodbank.webapp.dto.LinkAccountRequest;
import foodbank.webapp.dto.LinkAccountResponse;
import foodbank.webapp.service.AuthService;
import foodbank.webapp.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/link")
    @SecurityRequirement(name = "bearerAuth")
    public LinkAccountResponse linkAccount(
            @RequestBody LinkAccountRequest request,
            Authentication authentication
    ) {
        String currentRole = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(role -> role.equals("ROLE_DONOR") || role.equals("ROLE_RECIPIENT"))
                .map(role -> role.substring(5).toLowerCase())
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported role"));
        return authService.linkAccount(Integer.valueOf(authentication.getName()), currentRole, request);
    }

    @PostMapping("/add-role")
    @SecurityRequirement(name = "bearerAuth")
    public AddRoleResponse addRole(
            @RequestBody AddRoleRequest request,
            Authentication authentication
    ) {
        Integer userId = Integer.valueOf(
                authentication.getName()
        );

        return userService.addRole(userId, request);
    }

    @PostMapping("/switch-role")
    @SecurityRequirement(name = "bearerAuth")
    public LoginResponse switchRole(
            @RequestBody SwitchRoleRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        Integer userId = Integer.valueOf(
                authentication.getName()
        );

        String authorizationHeader =
                httpRequest.getHeader(
                        "Authorization"
                );

        return authService.switchRole(
                userId,
                request.targetRole(),
                authorizationHeader
        );
    }
}
