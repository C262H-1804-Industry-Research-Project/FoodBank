package foodbank.webapp.controllers;

import foodbank.webapp.dto.BanUserRequest;
import foodbank.webapp.dto.BanUserResponse;
import foodbank.webapp.service.AdminBanService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminBanController {

    private final AdminBanService adminBanService;

    @PostMapping("/{userId}/ban")
    @ResponseStatus(HttpStatus.CREATED)
    public BanUserResponse banUser(
            @PathVariable Integer userId,
            @RequestBody BanUserRequest request,
            Authentication authentication
    ) {
        Integer adminId =
                Integer.valueOf(authentication.getName());

        return adminBanService.banUser(
                userId,
                request,
                adminId
        );
    }
}