package t4m.toy_store.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;
import t4m.toy_store.product.entity.Category;
import t4m.toy_store.product.service.CategoryService;


import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
public class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

//    @Test
//    void testCreateCategory() {
//        Category c = Category.builder()
//                .name("Đồ chơi lắp ráp")
//                .slug("do-choi-lap-rap")
//                .build();
//
//        Category saved = categoryService.createCategory(c);
//        assertNotNull(saved.getId());
//        assertEquals("do-choi-lap-rap", saved.getSlug());
//    }
//
//    @Test
//    void testUpdateCategory() {
//        Category c = Category.builder()
//                .name("Xe mô hình")
//                .slug("xe-mo-hinh")
//                .build();
//        Category saved = categoryService.createCategory(c);
//
//        Category update = Category.builder()
//                .name("Xe điều khiển")
//                .slug("xe-dieu-khien")
//                .build();
//        Category updated = categoryService.updateCategory(saved.getId(), update);
//
//        assertEquals("Xe điều khiển", updated.getName());
//        assertEquals("xe-dieu-khien", updated.getSlug());
//    }
//
//    @Test
//    void testGetCategoryBySlug() {
//        Category c = Category.builder()
//                .name("Búp bê")
//                .slug("bup-be")
//                .build();
//        categoryService.createCategory(c);
//
//        Optional<Category> found = categoryService.getCategoryBySlug("bup-be");
//        assertTrue(found.isPresent());
//        assertEquals("Búp bê", found.get().getName());
//    }

    @Test
    void testDeleteCategory() {
        Category c = Category.builder()
                .name("Đồ chơi tre em")
                .slug("do-choi-tre-em")
                .build();
        Category saved = categoryService.createCategory(c);

        categoryService.deleteCategory(saved.getId());
        Optional<Category> found = categoryService.getCategoryById(saved.getId());

        assertFalse(found.isPresent());
    }

    @Test
    void testGetAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        assertNotNull(categories);
    }
}