package foodbank.webapp.controllers;

import foodbank.webapp.dto.RecipientCollectionHistoryResponse;
import foodbank.webapp.service.CollectionHistoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/recipients/me/collection-history")
public class RecipientHistoryController {

    private final CollectionHistoryService
            collectionHistoryService;

    /**
     * Returns one page of collection history belonging to
     * the currently authenticated recipient.
     */
    @GetMapping
    public PagedModel<RecipientCollectionHistoryResponse>
    getMyCollectionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Authentication authentication
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Integer userId = Integer.valueOf(
                authentication.getName()
        );

        return new PagedModel<>(
                collectionHistoryService
                        .getHistoryByRecipientUserId(
                                userId,
                                pageable
                        )
        );
    }
}