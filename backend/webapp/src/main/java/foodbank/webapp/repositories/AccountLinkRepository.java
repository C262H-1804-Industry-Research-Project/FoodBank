package foodbank.webapp.repositories;

import foodbank.webapp.entity.AccountLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountLinkRepository extends JpaRepository<AccountLink, Integer> {
    Optional<AccountLink> findByDonorUserId(Integer userId);
    Optional<AccountLink> findByRecipientUserId(Integer userId);
}
