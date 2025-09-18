package t4m.toy_store.product.repository;

import org.springframework.stereotype.Repository;
import t4m.toy_store.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
}