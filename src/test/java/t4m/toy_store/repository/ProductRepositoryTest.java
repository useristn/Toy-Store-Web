package t4m.toy_store.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import t4m.toy_store.product.entity.Category;
import t4m.toy_store.product.entity.Product;
import t4m.toy_store.product.repository.CategoryRepository;
import t4m.toy_store.product.repository.ProductRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
public class ProductRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private ProductRepository productRepo;

    @Test
    public void saveProduct_withCategory() {
        // Tạo Category
        Category c = Category.builder()
                .name("Đồ chơi gỗ")
                .slug("do-choi-go")
                .build();
        c = categoryRepo.save(c);

        // Tạo Product
        Product p = Product.builder()
                .name("Xylophone gỗ")
                .slug("xylophone-go")
                .shortDescription("Đồ chơi âm nhạc cho bé")
                .description("Chi tiết về sản phẩm Xylophone gỗ")
                .category(c)
                .shopId(1L) // giả định shop_id = 1 đã tồn tại
                .build();
        p = productRepo.save(p);

        assertNotNull(p.getId());
        assertEquals("xylophone-go", p.getSlug());
        assertEquals("Đồ chơi gỗ", p.getCategory().getName());
    }

    @Test
    public void findProductBySlug() {
        // Tạo Category
        Category c = categoryRepo.save(
                Category.builder()
                        .name("Đồ chơi nhựa")
                        .slug("do-choi-nhua")
                        .build()
        );

        // Tạo Product
        Product p = Product.builder()
                .name("Xe hơi nhựa")
                .slug("xe-hoi-nhua")
                .shortDescription("Đồ chơi xe hơi")
                .description("Mô tả chi tiết xe hơi nhựa")
                .category(c)
                .shopId(1L)
                .build();
        productRepo.save(p);

        // Test tìm kiếm theo slug
        Optional<Product> found = productRepo.findBySlug("xe-hoi-nhua");
        assertTrue(found.isPresent());
        assertEquals("Xe hơi nhựa", found.get().getName());
    }
}