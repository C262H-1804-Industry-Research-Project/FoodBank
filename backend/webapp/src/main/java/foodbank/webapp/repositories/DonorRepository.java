package foodbank.webapp.repositories;

import foodbank.webapp.entity.Donor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonorRepository extends JpaRepository<Donor, Integer> {
}
