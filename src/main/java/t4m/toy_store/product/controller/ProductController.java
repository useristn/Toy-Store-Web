package t4m.toy_store.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.product.dto.ProductResponse;
import t4m.toy_store.product.entity.Category;
import t4m.toy_store.product.entity.Product;
import t4m.toy_store.product.service.ProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<Product> products = productService.getAllProducts(PageRequest.of(page, size));
        Page<ProductResponse> response = products.map(ProductResponse::fromEntity);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ProductResponse.fromEntity(product));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<Product> products = productService.getProductsByCategory(categoryId, PageRequest.of(page, size));
        Page<ProductResponse> response = products.map(ProductResponse::fromEntity);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ProductResponse>> getFeaturedProducts() {
        List<Product> products = productService.getFeaturedProducts();
        List<ProductResponse> response = products.stream()
            .map(ProductResponse::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<Product> products = productService.searchProducts(keyword, PageRequest.of(page, size));
        Page<ProductResponse> response = products.map(ProductResponse::fromEntity);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/filter")
    public ResponseEntity<Page<ProductResponse>> filterProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<Product> products = productService.filterProducts(keyword, categoryId, minPrice, maxPrice, sort, PageRequest.of(page, size));
        Page<ProductResponse> response = products.map(ProductResponse::fromEntity);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }
}
