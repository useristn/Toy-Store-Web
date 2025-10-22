package t4m.toy_store.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import t4m.toy_store.product.entity.Category;
import t4m.toy_store.product.entity.Product;
import t4m.toy_store.product.repository.CategoryRepository;
import t4m.toy_store.product.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

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
    
    public Page<Product> filterProducts(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, String sortType, Pageable pageable) {
        // Create pageable without sort (sort is handled in query)
        Pageable pageableWithoutSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        
        // Call appropriate repository method based on sort type
        if (sortType != null) {
            switch (sortType) {
                case "price-asc":
                    return productRepository.findByFiltersPriceAsc(keyword, categoryId, minPrice, maxPrice, pageableWithoutSort);
                case "price-desc":
                    return productRepository.findByFiltersPriceDesc(keyword, categoryId, minPrice, maxPrice, pageableWithoutSort);
                case "name":
                    return productRepository.findByFiltersNameAsc(keyword, categoryId, minPrice, maxPrice, pageableWithoutSort);
                case "newest":
                    return productRepository.findByFiltersNewest(keyword, categoryId, minPrice, maxPrice, pageableWithoutSort);
                default:
                    return productRepository.findByFiltersNewest(keyword, categoryId, minPrice, maxPrice, pageableWithoutSort);
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
}
