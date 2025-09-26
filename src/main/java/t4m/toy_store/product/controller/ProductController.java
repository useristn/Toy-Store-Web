package t4m.toy_store.product.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import t4m.toy_store.product.entity.Product;
import t4m.toy_store.product.entity.ProductImage;
import t4m.toy_store.product.repository.ProductImageRepository;
import t4m.toy_store.product.repository.ProductRepository;
import t4m.toy_store.product.service.CloudinaryService;
import t4m.toy_store.product.service.ProductService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.io.IOException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductImageRepository productImageRepository;
    private final CloudinaryService cloudinaryService;
    private final ProductRepository productRepository;

    public ProductController(ProductService productService, ProductImageRepository productImageRepository, CloudinaryService cloudinaryService, ProductRepository productRepository) {
        this.productService = productService;
        this.productImageRepository = productImageRepository;
        this.cloudinaryService = cloudinaryService;
        this.productRepository = productRepository;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET BY SLUG
    @GetMapping("/slug/{slug}")
    public ResponseEntity<Product> getBySlug(@PathVariable String slug) {
        return productService.getProductBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // SEARCH
    @GetMapping("/search")
    public ResponseEntity<List<Product>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    //UPLOAD IMAGE
    @PostMapping("/{id}/upload-image")
    public ResponseEntity<ProductImage> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isThumbnail", defaultValue = "false") boolean isThumbnail
    ) throws IOException {

        // 1. Upload file lên Cloudinary
        String imageUrl = cloudinaryService.uploadFile(file,"products/" + id);

        // 2. Lấy product từ DB
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 3. Tạo ProductImage entity
        ProductImage image = ProductImage.builder()
                .product(product)
                .url(imageUrl)
                .isThumbnail(isThumbnail)
                .build();

        // 4. Lưu image
        ProductImage saved = productImageRepository.save(image);

        return ResponseEntity.ok(saved);
    }
}
