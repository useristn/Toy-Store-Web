package t4m.toy_store.test.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import t4m.toy_store.test.model.TestEntity;

public interface TestRepository extends JpaRepository<TestEntity, Long> {
}
