package t4m.toy_store.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import t4m.toy_store.admin.dto.ProductCreateRequest;
import t4m.toy_store.admin.dto.ProductUpdateRequest;
import t4m.toy_store.admin.dto.ProductStockStats;
import t4m.toy_store.product.dto.ProductResponse;
import t4m.toy_store.product.entity.Product;
import t4m.toy_store.product.service.CloudinaryService;
import t4m.toy_store.product.service.ProductService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminProductController {
    private final ProductService productService;
    private final CloudinaryService cloudinaryService;

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

    /**
     * Upload image to Cloudinary
     */
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            Map<String, String> uploadResult = cloudinaryService.uploadImage(file);
            return ResponseEntity.ok(uploadResult);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload image: " + e.getMessage()));
        }
    }

    /**
     * Create product with image upload
     */
    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProductWithImage(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") String price,
            @RequestParam(value = "discountPrice", required = false) String discountPrice,
            @RequestParam("stock") Integer stock,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "featured", defaultValue = "false") Boolean featured,
            @RequestParam("image") MultipartFile image) {
        try {
            // Upload image first
            Map<String, String> uploadResult = cloudinaryService.uploadImage(image);

            // Create product request
            ProductCreateRequest request = new ProductCreateRequest();
            request.setName(name);
            request.setDescription(description);
            request.setPrice(new java.math.BigDecimal(price));
            if (discountPrice != null && !discountPrice.isEmpty()) {
                request.setDiscountPrice(new java.math.BigDecimal(discountPrice));
            }
            request.setImageUrl(uploadResult.get("url"));
            request.setStock(stock);
            request.setCategoryId(categoryId);
            request.setFeatured(featured);

            // Create product
            Product product = productService.createProduct(request);
            
            // Set cloudinary public id and save
            product.setCloudinaryPublicId(uploadResult.get("publicId"));
            product = productService.saveProduct(product);

            return ResponseEntity.ok(ProductResponse.fromEntity(product));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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

    /**
     * Update product with optional image upload
     */
    @PutMapping(value = "/{id}/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProductWithImage(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") String price,
            @RequestParam(value = "discountPrice", required = false) String discountPrice,
            @RequestParam("stock") Integer stock,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "featured", defaultValue = "false") Boolean featured,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            Product existingProduct = productService.getProductById(id);
            if (existingProduct == null) {
                return ResponseEntity.notFound().build();
            }

            String imageUrl = existingProduct.getImageUrl();
            String publicId = existingProduct.getCloudinaryPublicId();

            // If new image provided, replace old one
            if (image != null && !image.isEmpty()) {
                Map<String, String> uploadResult = cloudinaryService.replaceImage(publicId, image);
                imageUrl = uploadResult.get("url");
                publicId = uploadResult.get("publicId");
            }

            // Update product
            ProductUpdateRequest request = ProductUpdateRequest.builder()
                    .name(name)
                    .description(description)
                    .price(new java.math.BigDecimal(price))
                    .discountPrice(discountPrice != null && !discountPrice.isEmpty() 
                        ? new java.math.BigDecimal(discountPrice) : null)
                    .imageUrl(imageUrl)
                    .stock(stock)
                    .categoryId(categoryId)
                    .featured(featured)
                    .build();

            Product product = productService.updateProduct(id, request);
            product.setCloudinaryPublicId(publicId);

            return ResponseEntity.ok(ProductResponse.fromEntity(product));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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

    @GetMapping("/in-stock")
    public ResponseEntity<Page<ProductResponse>> getInStockProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "10") int threshold) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.getInStockProducts(threshold, pageable);
        Page<ProductResponse> response = products.map(ProductResponse::fromEntity);
        return ResponseEntity.ok(response);
    }
}
