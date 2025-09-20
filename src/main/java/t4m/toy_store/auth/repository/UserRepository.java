package t4m.toy_store.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import t4m.toy_store.auth.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}