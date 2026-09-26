package foodbank.webapp.config;

import foodbank.webapp.entity.Category;
import foodbank.webapp.entity.Tag;
import foodbank.webapp.repositories.CategoryRepository;
import foodbank.webapp.repositories.TagRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CatalogueInitializer {
    @Bean
    CommandLineRunner initializeCatalogue(CategoryRepository categoryRepository, TagRepository tagRepository) {
        return arguments -> {
            List.of("Bakery", "Produce", "Pantry", "Dairy", "Fruit", "Meat&Seafood").forEach(name -> categoryRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> categoryRepository.save(Category.builder().name(name).build())));
            List.of("no-pork", "nut-free", "vegetarian", "vegan", "halal").forEach(name -> tagRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(name).type("DIETARY").build())));
        };
    }
}
