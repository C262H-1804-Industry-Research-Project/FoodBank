package foodbank.webapp.controllers;

import foodbank.webapp.dto.AdminDonorAccountResponse;
import foodbank.webapp.dto.AdminRecipientAccountResponse;
import foodbank.webapp.service.AdminAccountService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping("/donors")
    public List<AdminDonorAccountResponse> getDonors() {
        return adminAccountService.getDonors();
    }

    @GetMapping("/recipients")
    public List<AdminRecipientAccountResponse> getRecipients() {
        return adminAccountService.getRecipients();
    }
}
