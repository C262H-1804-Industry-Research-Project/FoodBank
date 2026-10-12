package foodbank.webapp.repositories;

import foodbank.webapp.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Integer>, JpaSpecificationExecutor<Listing> {
    List<Listing> findByDonorId(Integer donorId);

    long countByDonor_Id(Integer donorId);

    @Query("""
    SELECT DISTINCT l.name
    FROM Listing l
    WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%'))
    LIMIT 5
""")
    List<Listing> findByName(String search);
}
