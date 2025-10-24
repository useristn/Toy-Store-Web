package t4m.toy_store.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import t4m.toy_store.admin.dto.ProductCreateRequest;
import t4m.toy_store.admin.dto.ProductStockStats;
import t4m.toy_store.admin.dto.ProductUpdateRequest;
import t4m.toy_store.product.entity.Category;
import t4m.toy_store.product.entity.Product;
import t4m.toy_store.product.repository.CategoryRepository;
import t4m.toy_store.product.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    public Page<Product> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable);
    }

    public List<Product> getFeaturedProducts() {
        return productRepository.findByFeaturedTrue();
    }

    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(keyword, pageable);
    }

    public Page<Product> filterProducts(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
            String sortType, Pageable pageable) {
        // Create pageable without sort (sort is handled in query)
        Pageable pageableWithoutSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        // Call appropriate repository method based on sort type
        if (sortType != null) {
            switch (sortType) {
                case "price-asc":
                    return productRepository.findByFiltersPriceAsc(keyword, categoryId, minPrice, maxPrice,
                            pageableWithoutSort);
                case "price-desc":
                    return productRepository.findByFiltersPriceDesc(keyword, categoryId, minPrice, maxPrice,
                            pageableWithoutSort);
                case "name":
                    return productRepository.findByFiltersNameAsc(keyword, categoryId, minPrice, maxPrice,
                            pageableWithoutSort);
                case "newest":
                    return productRepository.findByFiltersNewest(keyword, categoryId, minPrice, maxPrice,
                            pageableWithoutSort);
                default:
                    return productRepository.findByFiltersNewest(keyword, categoryId, minPrice, maxPrice,
                            pageableWithoutSort);
            }
        }

        // Default: newest
        return productRepository.findByFiltersNewest(keyword, categoryId, minPrice, maxPrice, pageableWithoutSort);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    // Admin CRUD operations
    public Product createProduct(ProductCreateRequest request) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .imageUrl(request.getImageUrl())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .category(category)
                .featured(request.getFeatured() != null ? request.getFeatured() : false)
                .build();

        return productRepository.save(product);
    }

    public Product updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return null;
        }

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getDiscountPrice() != null) {
            product.setDiscountPrice(request.getDiscountPrice());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }
        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }
        if (request.getFeatured() != null) {
            product.setFeatured(request.getFeatured());
        }

        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found");
        }
        productRepository.deleteById(id);
    }

    public ProductStockStats getStockStats() {
        List<Product> allProducts = productRepository.findAll();

        long total = allProducts.size();
        long inStock = allProducts.stream().filter(p -> p.getStock() != null && p.getStock() > 0).count();
        long outOfStock = allProducts.stream().filter(p -> p.getStock() == null || p.getStock() == 0).count();
        long lowStock = allProducts.stream().filter(p -> p.getStock() != null && p.getStock() > 0 && p.getStock() <= 10)
                .count();
        long totalQuantity = allProducts.stream()
                .filter(p -> p.getStock() != null)
                .mapToLong(Product::getStock)
                .sum();

        return ProductStockStats.builder()
                .totalProducts(total)
                .inStockProducts(inStock)
                .outOfStockProducts(outOfStock)
                .lowStockProducts(lowStock)
                .totalStockQuantity(totalQuantity)
                .build();
    }

    public Page<Product> getOutOfStockProducts(Pageable pageable) {
        List<Product> outOfStock = productRepository.findAll().stream()
                .filter(p -> p.getStock() == null || p.getStock() == 0)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), outOfStock.size());
        List<Product> pageContent = outOfStock.subList(start, end);

        return new PageImpl<>(pageContent, pageable, outOfStock.size());
    }

    public Page<Product> getLowStockProducts(int threshold, Pageable pageable) {
        List<Product> lowStock = productRepository.findAll().stream()
                .filter(p -> p.getStock() != null && p.getStock() > 0 && p.getStock() <= threshold)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), lowStock.size());
        List<Product> pageContent = lowStock.subList(start, end);

        return new PageImpl<>(pageContent, pageable, lowStock.size());
    }

    public Page<Product> getInStockProducts(int threshold, Pageable pageable) {
        List<Product> inStock = productRepository.findAll().stream()
                .filter(p -> p.getStock() != null && p.getStock() > threshold)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), inStock.size());
        List<Product> pageContent = inStock.subList(start, end);

        return new PageImpl<>(pageContent, pageable, inStock.size());
    }

    public Page<Product> getLowRatingProducts(Double maxRating, Long categoryId, Pageable pageable) {
        return productRepository.findLowRatingProducts(maxRating, categoryId, pageable);
    }

    public Map<String, Object> getRatingStats() {
        Map<String, Object> stats = new HashMap<>();

        // Count products by rating ranges
        Long excellent = productRepository.countByRatingRange(4.5, 5.1); // 4.5-5.0
        Long good = productRepository.countByRatingRange(3.5, 4.5); // 3.5-4.4
        Long average = productRepository.countByRatingRange(2.5, 3.5); // 2.5-3.4
        Long poor = productRepository.countByRatingRange(1.5, 2.5); // 1.5-2.4
        Long veryPoor = productRepository.countByRatingRange(0.0, 1.5); // 0.0-1.4

        Long totalWithRating = productRepository.countProductsWithRating();
        Long totalProducts = productRepository.count();
        Long noRating = totalProducts - totalWithRating;

        stats.put("excellent", excellent); // 4.5-5.0 ⭐⭐⭐⭐⭐
        stats.put("good", good); // 3.5-4.4 ⭐⭐⭐⭐
        stats.put("average", average); // 2.5-3.4 ⭐⭐⭐
        stats.put("poor", poor); // 1.5-2.4 ⭐⭐
        stats.put("veryPoor", veryPoor); // 0.0-1.4 ⭐
        stats.put("noRating", noRating); // Chưa có đánh giá
        stats.put("totalProducts", totalProducts);
        stats.put("totalWithRating", totalWithRating);

        return stats;
    }

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }
}
