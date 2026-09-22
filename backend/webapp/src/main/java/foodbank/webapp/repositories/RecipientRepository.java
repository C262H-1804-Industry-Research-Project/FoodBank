package foodbank.webapp.repositories;

import foodbank.webapp.entity.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecipientRepository extends JpaRepository<Recipient, Integer> {

    Optional<Recipient> findByUserId(Integer userId);

    boolean existsByUserId(Integer userId);
}