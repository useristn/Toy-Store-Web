package t4m.toy_store.product.service;


import t4m.toy_store.product.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    Product createProduct(Product product);
    Product updateProduct(Long id, Product product);
    void deleteProduct(Long id);
    Optional<Product> getProductById(Long id);
    Optional<Product> getProductBySlug(String name);
    List<Product> getAllProducts();
    List<Product> searchProducts(String keyword);
}
