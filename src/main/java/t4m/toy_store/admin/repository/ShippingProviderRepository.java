package t4m.toy_store.admin.repository;

import t4m.toy_store.admin.entity.ShippingProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingProviderRepository extends JpaRepository<ShippingProvider, Long> {
}
