package t4m.toy_store.product.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import t4m.toy_store.product.entity.Product;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    List<Product> findByFeaturedTrue();
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR COALESCE(p.discountPrice, p.price) >= :minPrice) AND " +
           "(:maxPrice IS NULL OR COALESCE(p.discountPrice, p.price) <= :maxPrice)")
    Page<Product> findByFilters(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );
    
    // Query with price sorting (ascending) - using COALESCE for actual price
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR COALESCE(p.discountPrice, p.price) >= :minPrice) AND " +
           "(:maxPrice IS NULL OR COALESCE(p.discountPrice, p.price) <= :maxPrice) " +
           "ORDER BY COALESCE(p.discountPrice, p.price) ASC")
    Page<Product> findByFiltersPriceAsc(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );
    
    // Query with price sorting (descending) - using COALESCE for actual price
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR COALESCE(p.discountPrice, p.price) >= :minPrice) AND " +
           "(:maxPrice IS NULL OR COALESCE(p.discountPrice, p.price) <= :maxPrice) " +
           "ORDER BY COALESCE(p.discountPrice, p.price) DESC")
    Page<Product> findByFiltersPriceDesc(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );
    
    // Query with name sorting
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR COALESCE(p.discountPrice, p.price) >= :minPrice) AND " +
           "(:maxPrice IS NULL OR COALESCE(p.discountPrice, p.price) <= :maxPrice) " +
           "ORDER BY p.name ASC")
    Page<Product> findByFiltersNameAsc(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );
    
    // Query with newest sorting
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR COALESCE(p.discountPrice, p.price) >= :minPrice) AND " +
           "(:maxPrice IS NULL OR COALESCE(p.discountPrice, p.price) <= :maxPrice) " +
           "ORDER BY p.createdAt DESC")
    Page<Product> findByFiltersNewest(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );
}
