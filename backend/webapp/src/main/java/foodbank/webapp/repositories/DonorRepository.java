package foodbank.webapp.repositories;

import foodbank.webapp.entity.Donor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DonorRepository extends JpaRepository<Donor, Integer> {

    Optional<Donor> findByUserId(Integer userId);

    boolean existsByUserId(Integer userId);
}