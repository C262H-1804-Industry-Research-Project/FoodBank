package foodbank.webapp.repositories;

import foodbank.webapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByEmailIgnoreCase(String email);
}