package t4m.toy_store.product.service;

import t4m.toy_store.product.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    Category createCategory(Category category);
    Category updateCategory(Long id, Category category);
    void deleteCategory(Long id);
    Optional<Category> getCategoryById(Long id);
    Optional<Category> getCategoryBySlug(String slug);
    List<Category> getAllCategories();
}