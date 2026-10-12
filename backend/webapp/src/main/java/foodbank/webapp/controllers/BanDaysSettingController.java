package foodbank.webapp.controllers;

import foodbank.webapp.dto.BanDaysResponse;
import foodbank.webapp.dto.UpdateBanDaysRequest;
import foodbank.webapp.service.BanDaysSettingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings/ban-days")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class BanDaysSettingController {

    private final BanDaysSettingService banDaysSettingService;

    @GetMapping
    public BanDaysResponse getSettings() {
        return banDaysSettingService.getSettings();
    }

    @PutMapping
    public BanDaysResponse updateSettings(
            @RequestBody UpdateBanDaysRequest request
    ) {
        return banDaysSettingService.updateSettings(request);
    }
}