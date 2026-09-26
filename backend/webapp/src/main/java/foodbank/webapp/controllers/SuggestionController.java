package foodbank.webapp.controllers;


import foodbank.webapp.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;


    @GetMapping("/foodnames")
    public List<String> getFoodNameSuggestions(@RequestParam(required = false) String query){
        return suggestionService.suggestFoodNames(query);
    }


}
