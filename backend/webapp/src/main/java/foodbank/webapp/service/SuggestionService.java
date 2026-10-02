package foodbank.webapp.service;

import foodbank.webapp.entity.Listing;
import foodbank.webapp.repositories.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final ListingRepository listingRepository;


    public List<String> suggestFoodNames(String query) {
        return listingRepository.findByName(query)
                .stream()
                .map(Listing::getName)
                .toList();
    }
}
