package foodbank.webapp.controllers;

import foodbank.webapp.dto.CreateDonorRequest;
import foodbank.webapp.dto.DonorResponse;
import foodbank.webapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DonorResponse createDonor(@RequestBody CreateDonorRequest request) {
        return userService.createDonor(request);
    }
}
