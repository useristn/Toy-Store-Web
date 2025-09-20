package t4m.toy_store.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import t4m.toy_store.auth.entity.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRname(String rname);
}