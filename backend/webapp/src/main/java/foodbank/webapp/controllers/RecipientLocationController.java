package foodbank.webapp.controllers;

import foodbank.webapp.dto.RecipientLocation;
import foodbank.webapp.service.RecipientLocationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/recipients/me/location")
@RequiredArgsConstructor
public class RecipientLocationController {
    private final RecipientLocationService recipientLocationService;

    // JwtAuthenticationFilter stores the user ID as the principal name.
    @GetMapping
    public ResponseEntity<RecipientLocation> getLocation(Authentication authentication) {
        return recipientLocationService.getLocation(Integer.valueOf(authentication.getName()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    public RecipientLocation saveLocation(@RequestBody RecipientLocation location,
                                          Authentication authentication) {
        return recipientLocationService.saveLocation(Integer.valueOf(authentication.getName()), location);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearLocation(Authentication authentication) {
        recipientLocationService.clearLocation(Integer.valueOf(authentication.getName()));
    }
}
