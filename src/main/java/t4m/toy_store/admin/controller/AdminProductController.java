package t4m.toy_store.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.admin.dto.ProductCreateRequest;
import t4m.toy_store.admin.dto.ProductUpdateRequest;
import t4m.toy_store.admin.dto.ProductStockStats;
import t4m.toy_store.product.dto.ProductResponse;
import t4m.toy_store.product.entity.Product;
import t4m.toy_store.product.service.ProductService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminProductController {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        
        Page<Product> products;
        if (search != null && !search.trim().isEmpty()) {
            products = productService.searchProducts(search, PageRequest.of(page, size, Sort.by("id").descending()));
        } else {
            products = productService.getAllProducts(PageRequest.of(page, size, Sort.by("id").descending()));
        }
        
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

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductCreateRequest request) {
        try {
            Product product = productService.createProduct(request);
            return ResponseEntity.ok(ProductResponse.fromEntity(product));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody ProductUpdateRequest request) {
        try {
            Product product = productService.updateProduct(id, request);
            if (product == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Product not found");
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ProductResponse.fromEntity(product));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Product deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/stats/stock")
    public ResponseEntity<ProductStockStats> getStockStats() {
        ProductStockStats stats = productService.getStockStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/out-of-stock")
    public ResponseEntity<Page<ProductResponse>> getOutOfStockProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.getOutOfStockProducts(pageable);
        Page<ProductResponse> response = products.map(ProductResponse::fromEntity);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<Page<ProductResponse>> getLowStockProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "10") int threshold) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.getLowStockProducts(threshold, pageable);
        Page<ProductResponse> response = products.map(ProductResponse::fromEntity);
        return ResponseEntity.ok(response);
    }
}
