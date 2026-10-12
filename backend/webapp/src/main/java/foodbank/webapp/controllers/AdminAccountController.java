package foodbank.webapp.controllers;

import foodbank.webapp.dto.AdminAccountResponse;
import foodbank.webapp.dto.AdminDonorAccountResponse;
import foodbank.webapp.dto.AdminRecipientAccountResponse;
import foodbank.webapp.service.AdminAccountService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    private static final int MAX_PAGE_SIZE = 100;

    // role is DONOR or RECIPIENT, status is ACTIVE, UNDER_REVIEW or BANNED, and q matches the
    // user ID, name, email or suburb; leave any of them out to not filter by it.
    @GetMapping("/admin/accounts")
    public PagedModel<AdminAccountResponse> getAccounts(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE
            );
        }
        return new PagedModel<>(adminAccountService.findAccounts(
                role, status, q, PageRequest.of(page, size)
        ));
    }

    @GetMapping("/donors")
    public List<AdminDonorAccountResponse> getDonors() {
        return adminAccountService.getDonors();
    }

    @GetMapping("/recipients")
    public List<AdminRecipientAccountResponse> getRecipients() {
        return adminAccountService.getRecipients();
    }
}
