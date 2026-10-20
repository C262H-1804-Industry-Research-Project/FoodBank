package foodbank.webapp.repositories;

import foodbank.webapp.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Integer>, JpaSpecificationExecutor<Listing> {
    List<Listing> findByDonorId(Integer donorId);

    // Row lock held until the transaction ends; used wherever stock is checked and then changed
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Listing l WHERE l.listingId = :listingId")
    Optional<Listing> findByIdForUpdate(@Param("listingId") Integer listingId);

    long countByDonor_Id(Integer donorId);

    @Query("""
    SELECT DISTINCT l.name
    FROM Listing l
    WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%'))
    LIMIT 5
""")
    List<Listing> findByName(String search);
}
