package foodbank.webapp.repositories;

import foodbank.webapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByEmailIgnoreCase(String email);

    // Lock both accounts in a consistent order while establishing a link.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId in :ids order by u.userId")
    List<User> lockAccounts(@Param("ids") List<Integer> ids);
}
