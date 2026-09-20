package foodbank.webapp.controllers;

import foodbank.webapp.dto.CollectionHistoryResponse;
import foodbank.webapp.service.CollectionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donors/{donorId}/collection-history")
@RequiredArgsConstructor
public class DonorHistoryController {
    private final CollectionHistoryService collectionHistoryService;

    @GetMapping
    public List<CollectionHistoryResponse> getHistory(@PathVariable Integer donorId) {
        return collectionHistoryService.getHistoryByDonor(donorId);
    }
}
