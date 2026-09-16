package foodbank.webapp.config;

import foodbank.webapp.entity.Tag;
import foodbank.webapp.repositories.TagRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TagCatalogueInitializer {
    private static final List<String> APPROVED_TAGS = List.of("no pork", "nut free", "vegetarian", "halal");

    @Bean
    CommandLineRunner initializeApprovedTags(TagRepository tagRepository) {
        return arguments -> APPROVED_TAGS.forEach(name -> tagRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(name).type("DIETARY").build())));
    }
}
