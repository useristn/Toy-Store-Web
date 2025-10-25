package t4m.toy_store;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;
import t4m.toy_store.auth.entity.Role;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.auth.repository.RoleRepository;
import t4m.toy_store.auth.repository.UserRepository;
import t4m.toy_store.product.entity.Category;
import t4m.toy_store.product.entity.Product;
import t4m.toy_store.product.repository.CategoryRepository;
import t4m.toy_store.product.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@EnableScheduling // Enable scheduled tasks for chatbot cleanup
public class ToyStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToyStoreApplication.class, args);
    }

    @Bean
    @Order(1)
    public ApplicationRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            List<String> roleNames = Arrays.asList("ROLE_USER", "ROLE_VENDOR", "ROLE_SHIPPER", "ROLE_ADMIN");

            for (String rname : roleNames) {
                if (roleRepository.findByRname(rname).isEmpty()) {
                    Role role = new Role();
                    role.setRname(rname);
                    roleRepository.save(role);
                    System.out.println("Initialized role: " + rname);
                } else {
                    System.out.println("Role already exists: " + rname);
                }
            }
        };
    }

    @Bean
    @Order(2)
    public ApplicationRunner initAdminUser(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@toystore.com";

            // Check if admin user already exists
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                // Get ADMIN role
                Role adminRole = roleRepository.findByRname("ROLE_ADMIN")
                        .orElseThrow(() -> new RuntimeException(
                                "ROLE_ADMIN not found. Please ensure initRoles runs first."));

                // Create admin user
                User adminUser = new User();
                adminUser.setEmail(adminEmail);
                adminUser.setPasswd(passwordEncoder.encode("admin123")); // Password: admin123
                adminUser.setName("Admin User");
                adminUser.setPhone("0987654321");
                adminUser.setAddress("Admin Office");
                adminUser.setActivated(true);
                adminUser.setCreated(LocalDateTime.now());
                adminUser.setUpdated(LocalDateTime.now());
                adminUser.getRoles().add(adminRole);

                userRepository.save(adminUser);

                System.out.println("==================================================");
                System.out.println("✅ Admin user created successfully!");
                System.out.println("📧 Email: " + adminEmail);
                System.out.println("🔑 Password: admin123");
                System.out.println("==================================================");
            } else {
                System.out.println("Admin user already exists: " + adminEmail);
            }
        };
    }

    @Bean
    @Order(3)
    public ApplicationRunner initShipperUsers(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Get SHIPPER role
            Role shipperRole = roleRepository.findByRname("ROLE_SHIPPER")
                .orElseThrow(() -> new RuntimeException("ROLE_SHIPPER not found. Please ensure initRoles runs first."));
            
            // Shipper 1
            String shipper1Email = "shipper@t4m.com";
            if (userRepository.findByEmail(shipper1Email).isEmpty()) {
                User shipper1 = new User();
                shipper1.setEmail(shipper1Email);
                shipper1.setPasswd(passwordEncoder.encode("shipper123")); // Password: shipper123
                shipper1.setName("John Shipper");
                shipper1.setPhone("0901234567");
                shipper1.setAddress("123 Shipper Street, District 1, Ho Chi Minh City");
                shipper1.setActivated(true);
                shipper1.setCreated(LocalDateTime.now());
                shipper1.setUpdated(LocalDateTime.now());
                shipper1.getRoles().add(shipperRole);
                
                userRepository.save(shipper1);
                
                System.out.println("==================================================");
                System.out.println("✅ Shipper user 1 created successfully!");
                System.out.println("📧 Email: " + shipper1Email);
                System.out.println("🔑 Password: shipper123");
                System.out.println("🚚 Role: SHIPPER");
                System.out.println("==================================================");
            } else {
                System.out.println("Shipper user 1 already exists: " + shipper1Email);
            }
            
            // Shipper 2
            String shipper2Email = "shipper2@t4m.com";
            if (userRepository.findByEmail(shipper2Email).isEmpty()) {
                User shipper2 = new User();
                shipper2.setEmail(shipper2Email);
                shipper2.setPasswd(passwordEncoder.encode("shipper123")); // Password: shipper123
                shipper2.setName("Jane Delivery");
                shipper2.setPhone("0912345678");
                shipper2.setAddress("456 Delivery Avenue, District 3, Ho Chi Minh City");
                shipper2.setActivated(true);
                shipper2.setCreated(LocalDateTime.now());
                shipper2.setUpdated(LocalDateTime.now());
                shipper2.getRoles().add(shipperRole);
                
                userRepository.save(shipper2);
                
                System.out.println("==================================================");
                System.out.println("✅ Shipper user 2 created successfully!");
                System.out.println("📧 Email: " + shipper2Email);
                System.out.println("🔑 Password: shipper123");
                System.out.println("🚚 Role: SHIPPER");
                System.out.println("==================================================");
            } else {
                System.out.println("Shipper user 2 already exists: " + shipper2Email);
            }
        };
    }

    @Bean
    @Order(4)
    public ApplicationRunner initCategories(CategoryRepository categoryRepository, ProductRepository productRepository) {
        return args -> {
            if (categoryRepository.count() == 0) {
                // Create categories
                Category dolls = Category.builder().name("Búp bê & Công chúa").description("Búp bê xinh đẹp").icon("👸")
                        .build();
                Category vehicles = Category.builder().name("Xe & Phi thuyền").description("Phương tiện vũ trụ")
                        .icon("🚀").build();
                Category building = Category.builder().name("Xếp hình & Ghép").description("Đồ chơi sáng tạo")
                        .icon("🧩").build();
                Category science = Category.builder().name("Khoa học & Thí nghiệm").description("Học tập vui vẻ")
                        .icon("🔬").build();
                Category outdoor = Category.builder().name("Ngoài trời & Thể thao").description("Vận động ngoài trời")
                        .icon("⚽").build();
                Category arts = Category.builder().name("Nghệ thuật & Sáng tạo").description("Phát triển nghệ thuật")
                        .icon("🎨").build();
                Category electronic = Category.builder().name("Điện tử & Robot").description("Công nghệ hiện đại")
                        .icon("🤖").build();
                Category board = Category.builder().name("Board Game & Trí tuệ").description("Trò chơi trí tuệ")
                        .icon("🎲").build();

                categoryRepository
                        .saveAll(Arrays.asList(dolls, vehicles, building, science, outdoor, arts, electronic, board));

                // Create sample products - DOLLS & PRINCESSES (12 products)
                productRepository.saveAll(Arrays.asList(
                        Product.builder().name("Búp bê Công chúa Elsa")
                                .description("Công chúa băng giá xinh đẹp với bộ váy lung linh")
                                .price(new BigDecimal("299000")).discountPrice(new BigDecimal("249000")).category(dolls)
                                .stock(50).featured(true)
                                .imageUrl(
                                        "https://res.cloudinary.com/t4m/image/upload/v1761204262/disney-frozen-cong-chua-elsa-2-hlw48-hlw46_jcd9hw.jpg")
                                .build(),
                        Product.builder().name("Búp bê Anna cổ tích")
                                .description("Công chúa dũng cảm với trang phục đẹp mắt")
                                .price(new BigDecimal("289000")).category(dolls).stock(45).featured(false)
                                .imageUrl(
                                        "https://res.cloudinary.com/t4m/image/upload/v1761204088/disney-frozen-cong-chua-anna-hmj43-hmj41_v8ztjq.jpg")
                                .build(),
                        Product.builder().name("Búp bê Barbie Dream House")
                                .description("Búp bê Barbie sang trọng với ngôi nhà mơ ước")
                                .price(new BigDecimal("1299000")).discountPrice(new BigDecimal("999000"))
                                .category(dolls).stock(20).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761204482/3_xa65js.webp")
                                .build(),
                        Product.builder().name("Búp bê Ariel nàng tiên cá")
                                .description("Nàng tiên cá xinh đẹp với đuôi cá lấp lánh")
                                .price(new BigDecimal("329000")).category(dolls).stock(40).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761204637/disney-princess-nang-tien-ca-ariel-hlx30-hlx29_cfaa07ed-2118-480e-a244-a8779b7da1f1_ycf8f6.jpg").build(),
                        Product.builder().name("Búp bê Belle người đẹp").description("Công chúa Belle yêu đọc sách")
                                .price(new BigDecimal("319000")).category(dolls).stock(38).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761204691/disney-princess-cong-chua-nguoi-dep-va-quai-vat-belle-hlw11-hlw02_vjiu7a.jpg").build(),
                        Product.builder().name("Búp bê Jasmine công chúa")
                                .description("Công chúa Jasmine với trang phục Ả Rập")
                                .price(new BigDecimal("309000")).discountPrice(new BigDecimal("269000")).category(dolls)
                                .stock(42).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761204755/df63150f947cfe022a07cfaf2d9971a7_ybjqwl.jpg").build(),
                        Product.builder().name("Búp bê Cinderella lọ lem")
                                .description("Công chúa Lọ Lem với giày thủy tinh")
                                .price(new BigDecimal("299000")).category(dolls).stock(47).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761204799/disney-princess-cong-chua-lo-lem-cinderella-hlw06-hlw02_wrny5i.jpg").build(),
                        Product.builder().name("Set búp bê gia đình hạnh phúc")
                                .description("Bộ búp bê gia đình 4 người")
                                .price(new BigDecimal("599000")).discountPrice(new BigDecimal("499000")).category(dolls)
                                .stock(25).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761204930/do-choi-deo-spy-x-family-gia-dinh-diep-vien-series-1-spy-family-s1-sxf11521_9_ik41qn.png").build(),
                        Product.builder().name("Búp bê baby doll").description("Em bé búp bê biết khóc, cười")
                                .price(new BigDecimal("459000")).category(dolls).stock(30).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761205333/dw60280_1_de4ec532-2512-4235-a56b-9a231996b30f_iwhq9p.jpg").build(),
                        Product.builder().name("Búp bê LOL Surprise").description("Búp bê bất ngờ với nhiều phụ kiện")
                                .price(new BigDecimal("199000")).category(dolls).stock(60).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761205423/589365euc_6_531b0aaf-789f-4d3c-8674-daefcd0238e4_e2qcwp.jpg").build(),
                        Product.builder().name("Set búp bê Disney Princess").description("Bộ 5 công chúa Disney")
                                .price(new BigDecimal("899000")).discountPrice(new BigDecimal("749000")).category(dolls)
                                .stock(15).featured(true)
                                .imageUrl("https://www.mykingdom.com.vn/cdn/shop/files/43219_f5a25a4d-dba3-4ea2-9539-97dde924e077.jpg?v=1725527687&width=1206").build(),
                        Product.builder().name("Búp bê Aurora ngủ trong rừng")
                                .description("Công chúa ngủ trong rừng xinh đẹp")
                                .price(new BigDecimal("329000")).category(dolls).stock(34).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761205542/disney-princess-cong-chua-aurora-hlw09-hlw02_h8ay3b.jpg").build()));

                // VEHICLES & SPACESHIPS (15 products)
                productRepository.saveAll(Arrays.asList(
                        Product.builder().name("Phi thuyền Siêu tốc X-Wing")
                                .description("Phi thuyền chiến đấu tốc độ ánh sáng")
                                .price(new BigDecimal("599000")).discountPrice(new BigDecimal("499000"))
                                .category(vehicles).stock(30).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761207254/75355_6d31fb67-f9ca-47ab-a647-452df88ed1e2_hgv396.jpg").build(),
                        Product.builder().name("Xe ô tô điều khiển từ xa").description("Xe đua điều khiển tốc độ cao")
                                .price(new BigDecimal("399000")).category(vehicles).stock(45).featured(true)
                                .imageUrl("https://www.mykingdom.com.vn/cdn/shop/products/46300-orange_1.jpg?v=1706968339&width=1206").build(),
                        Product.builder().name("Tàu vũ trụ Apollo").description("Tàu vũ trụ Apollo mô hình chi tiết")
                                .price(new BigDecimal("799000")).category(vehicles).stock(20).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761208615/DS1059H-03-10_2_zcal00.jpg").build(),
                        Product.builder().name("Xe tăng chiến đấu").description("Xe tăng quân sự điều khiển")
                                .price(new BigDecimal("549000")).category(vehicles).stock(35).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761207423/xe-tang-chien-dau-1-siku-8319_1_e6cnur.png").build(),
                        Product.builder().name("Máy bay phản lực F-16").description("Máy bay chiến đấu F-16 mô hình")
                                .price(new BigDecimal("449000")).discountPrice(new BigDecimal("379000"))
                                .category(vehicles).stock(28).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761207518/MT15088_9804_1_grxp6j.jpg").build(),
                        Product.builder().name("Xe cứu hỏa siêu tốc").description("Xe cứu hỏa với thang cứu nạn")
                                .price(new BigDecimal("379000")).category(vehicles).stock(40).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761207588/do-choi-xe-cuu-hoa-dieu-khien-tu-xa-vecto-VT253B_1_deh1jl.png").build(),
                        Product.builder().name("Xe cảnh sát tuần tra").description("Xe cảnh sát với còi hú")
                                .price(new BigDecimal("359000")).category(vehicles).stock(38).featured(false)
                                .imageUrl("https://www.mykingdom.com.vn/cdn/shop/files/xe-canh-sat-range-rover-velar-kem-nhan-vien-canh-sat-bruder-bru02890_1.jpg?v=1743133685&width=1206").build(),
                        Product.builder().name("Xe đua F1 Lightning").description("Xe đua F1 tốc độ siêu nhanh")
                                .price(new BigDecimal("519000")).discountPrice(new BigDecimal("439000"))
                                .category(vehicles).stock(33).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761207857/xe-dua-f1-1-43-mclaren-f1-mcl38-2024-maisto-04-18-38214_5_emkkhh.jpg").build(),
                        Product.builder().name("Tên lửa Falcon Heavy").description("Tên lửa SpaceX Falcon Heavy")
                                .price(new BigDecimal("899000")).category(vehicles).stock(18).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761207910/cb6a24462e74829f08d8e5bfa57e9b1f_o4gs5k.jpg").build(),
                        Product.builder().name("Máy bay trực thăng").description("Trực thăng cứu hộ điều khiển")
                                .price(new BigDecimal("529000")).category(vehicles).stock(29).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761207964/vtyd-718_uxn6mx.jpg").build(),
                        Product.builder().name("Tàu hỏa cao tốc Bullet").description("Tàu hỏa siêu tốc Nhật Bản")
                                .price(new BigDecimal("699000")).discountPrice(new BigDecimal("599000"))
                                .category(vehicles).stock(22).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761208032/do-choi-lap-rap-duong-ray-va-tau-toc-hanh-dieu-khien-tu-xa-vecto-vt2811y_2_osks0q.png")
                                .build(),
                        Product.builder().name("Xe mô tô đua Ducati").description("Mô tô đua Ducati tốc độ")
                                .price(new BigDecimal("339000")).category(vehicles).stock(36).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761208102/1199_mgzdg1.png").build(),
                        Product.builder().name("Phi thuyền Millennium Falcon")
                                .description("Phi thuyền huyền thoại Star Wars")
                                .price(new BigDecimal("1299000")).discountPrice(new BigDecimal("1099000"))
                                .category(vehicles).stock(12).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761208154/75375copy1_so2ph1.jpg").build(),
                        Product.builder().name("Tàu cướp biển Caribbean").description("Tàu cướp biển với cờ đầu lâu")
                                .price(new BigDecimal("759000")).category(vehicles).stock(19).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761208271/bo-do-choi-thuyen-va-cuop-bien-caribe-9_rh81us.jpg").build(),
                        Product.builder().name("Set phương tiện cứu hộ").description("Bộ 5 xe cứu hộ khẩn cấp")
                                .price(new BigDecimal("699000")).discountPrice(new BigDecimal("579000"))
                                .category(vehicles).stock(24).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761208359/6326_v4ul4e.jpg")
                                .build()));

                // BUILDING & PUZZLES (12 products)
                productRepository.saveAll(Arrays.asList(
                        Product.builder().name("Lego City Trung tâm vũ trụ")
                                .description("Bộ xếp hình trung tâm vũ trụ NASA 1000 chi tiết")
                                .price(new BigDecimal("899000")).category(building).stock(20).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761208790/a7ae3ce309c270420d93b46940824e30_qkzhrf.jpg").build(),
                        Product.builder().name("Lego Technic siêu xe").description("Xếp hình siêu xe Lamborghini")
                                .price(new BigDecimal("1299000")).discountPrice(new BigDecimal("1099000"))
                                .category(building).stock(15).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761208881/do-choi-lap-rap-sieu-xe-ferrari-fxx-k-v29-lego-technic-42212-lg_1_cpfewt.jpg").build(),
                        Product.builder().name("Puzzle 1000 mảnh thiên hà")
                                .description("Tranh ghép hình thiên hà đẹp mắt")
                                .price(new BigDecimal("199000")).category(building).stock(50).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761208976/5904438104314_e37919521592443fbd018950f31d154b_grande_s3t6y8.jpg").build(),
                        Product.builder().name("Lego Friends công viên giải trí")
                                .description("Công viên vui chơi với nhiều trò chơi")
                                .price(new BigDecimal("799000")).category(building).stock(25).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761209029/2cf7a127a3fe9bc44d3fe83478dd5289_czzy0k.jpg").build(),
                        Product.builder().name("Minecraft thế giới khối vuông")
                                .description("Bộ xếp hình Minecraft 500 chi tiết")
                                .price(new BigDecimal("549000")).discountPrice(new BigDecimal("459000"))
                                .category(building).stock(35).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761209076/chuyen-tham-hiem-mo-nhim-bien-lego-minecraft-21269_2_lf9ei3.jpg").build(),
                        Product.builder().name("Lego Harry Potter lâu đài").description("Lâu đài Hogwarts huyền thoại")
                                .price(new BigDecimal("1799000")).category(building).stock(10).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761209169/do-choi-lap-rap-lau-dai-hogwarts-toa-thap-chinh-lego-harry-potter-76454_3-1_hf7dm6.jpg").build(),
                        Product.builder().name("Puzzle 3D tháp Eiffel")
                                .description("Puzzle 3D tháp Eiffel Paris 216 mảnh")
                                .price(new BigDecimal("349000")).category(building).stock(30).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761209215/1a3c8161a7e3db56be7c0ab9fa488c45_o0afbw.jpg").build(),
                        Product.builder().name("Rubik's Cube 3x3 tốc độ").description("Rubik cube tốc độ chuyên nghiệp")
                                .price(new BigDecimal("149000")).category(building).stock(80).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761209271/8852rb_1__1_qp9eh1.jpg").build(),
                        Product.builder().name("Lego Disney lâu đài công chúa")
                                .description("Lâu đài Disney Princess tuyệt đẹp")
                                .price(new BigDecimal("1299000")).category(building).stock(16).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761209324/do-choi-lap-rap-lau-dai-disney-lego-disney-princess-43222_3_yqehep.jpg").build(),
                        Product.builder().name("Khối nam châm Magformers")
                                .description("Bộ khối nam châm ghép hình 50 chi tiết")
                                .price(new BigDecimal("599000")).category(building).stock(28).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761209451/s-l1600_srtxh2.webp").build(),
                        Product.builder().name("Lego Jurassic World khủng long")
                                .description("Bộ xếp hình khủng long T-Rex")
                                .price(new BigDecimal("999000")).discountPrice(new BigDecimal("849000"))
                                .category(building).stock(19).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761209598/vn-11134207-7ras8-mav424dtasuk61_resize_w900_nl_zo657i.webp").build(),
                        Product.builder().name("Lego Duplo trang trại vui vẻ")
                                .description("Bộ xếp hình trang trại cho bé nhỏ")
                                .price(new BigDecimal("449000")).category(building).stock(32).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761209660/5702017567457-1_bpnmbs.png").build()));

                // SCIENCE & EXPERIMENTS (10 products)
                productRepository.saveAll(Arrays.asList(
                        Product.builder().name("Bộ thí nghiệm Vũ trụ 100 thí nghiệm")
                                .description("Khám phá 100 thí nghiệm khoa học tuyệt vời")
                                .price(new BigDecimal("459000")).category(science).stock(40).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761210168/do-choi-giao-duc-tot-nhat-cho-be-1_khgd2a.jpg").build(),
                        Product.builder().name("Kính thiên văn khám phá sao")
                                .description("Kính thiên văn chuyên nghiệp 70mm")
                                .price(new BigDecimal("899000")).discountPrice(new BigDecimal("749000"))
                                .category(science).stock(25).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761210242/10-kinh-thien-van-do-choi-ban-chay-nhat-8_tolw0n.jpg").build(),
                        Product.builder().name("Bộ hóa học nhỏ").description("Thí nghiệm hóa học an toàn cho trẻ em")
                                .price(new BigDecimal("389000")).category(science).stock(35).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761210300/nat-geo-bo-thi-nghiem-phan-ung-lam-lanh-steam-rtngchemcr_3_kai3qz.jpg").build(),
                        Product.builder().name("Kính hiển vi sinh học").description("Kính hiển vi học sinh 1200x")
                                .price(new BigDecimal("599000")).category(science).stock(30).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761210396/84564fgdfgdf45_j6zzka.jpg").build(),
                        Product.builder().name("Robot lập trình STEM").description("Robot học lập trình cho trẻ em")
                                .price(new BigDecimal("1299000")).discountPrice(new BigDecimal("999000"))
                                .category(science).stock(20).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761210521/makeblock-codey-rocky-english-version-robot-giao-duc-lap-trinh-1_w5utww.jpg").build(),
                        Product.builder().name("Bộ thí nghiệm núi lửa").description("Tạo núi lửa phun trào tại nhà")
                                .price(new BigDecimal("249000")).category(science).stock(50).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761210570/nat-geo-bo-thi-nghiem-nui-lua-phun-trao-steam-rtngvolcano2_5_bsocji.jpg").build(),
                        Product.builder().name("Mô hình hệ mặt trời").description("Hệ mặt trời quay tự động có đèn")
                                .price(new BigDecimal("549000")).category(science).stock(22).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_250,h_200,c_pad,dpr_2.0/v1761210651/s-l1600_fdvy3i.webp")
                                .build(),
                        Product.builder().name("Bộ thí nghiệm điện từ").description("Khám phá điện và từ trường")
                                .price(new BigDecimal("419000")).category(science).stock(32).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761210800/8b0d6f6315b2f8a0f586260f5247bef7_jyslpg.jpg").build(),
                        Product.builder().name("Bộ khai quật hóa thạch khủng long")
                                .description("Khám phá hóa thạch như nhà khảo cổ")
                                .price(new BigDecimal("279000")).category(science).stock(38).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761210785/nat-geo-bo-khao-co-truy-tim-xuong-khung-long-bao-chua-steam-rttrexdig_2_dksvrk.jpg").build(),
                        Product.builder().name("Kit Arduino cho trẻ em").description("Học lập trình điện tử cơ bản")
                                .price(new BigDecimal("799000")).discountPrice(new BigDecimal("679000"))
                                .category(science).stock(18).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761210878/91arrRYVW5L._AC_SL1500__eizqfl.jpg").build()));

                // OUTDOOR & SPORTS (12 products)
                productRepository.saveAll(Arrays.asList(
                        Product.builder().name("Bóng đá World Cup 2024")
                                .description("Bóng đá chính thức World Cup size 5")
                                .price(new BigDecimal("299000")).category(outdoor).stock(50).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_250,h_200,c_pad,dpr_2.0/v1761222342/578ab8e1d6714d56ba44878a1dafc43e_qlsayj.jpg").build(),
                        Product.builder().name("Xe đạp thể thao trẻ em").description("Xe đạp 16 inch cho bé 5-8 tuổi")
                                .price(new BigDecimal("1499000")).discountPrice(new BigDecimal("1299000"))
                                .category(outdoor).stock(15).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761222424/a4bf02ce5f2868372c64d9cb3278ab79_jggsms.jpg").build(),
                        Product.builder().name("Bóng rổ NBA Professional").description("Bóng rổ cao cấp size 7")
                                .price(new BigDecimal("349000")).category(outdoor).stock(40).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761222490/a8201_2_c1d2511c-ba30-4f4d-8051-6935eb5c27f5_bakvea.jpg").build(),
                        Product.builder().name("Ván trượt Skateboard Pro").description("Ván trượt chuyên nghiệp 7 lớp")
                                .price(new BigDecimal("599000")).category(outdoor).stock(25).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761222581/images_o2wvpq.jpg").build(),
                        Product.builder().name("Xe scooter 3 bánh").description("Xe scooter phát sáng cho bé")
                                .price(new BigDecimal("699000")).discountPrice(new BigDecimal("599000"))
                                .category(outdoor).stock(30).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761222742/089r_2_jzmccs.jpg").build(),
                        Product.builder().name("Bộ cầu lông gia đình").description("Set cầu lông 4 vợt kèm lưới")
                                .price(new BigDecimal("449000")).category(outdoor).stock(28).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761222864/vot-cau-long-bao-nhieu-tien-3_xu0jdo.png").build(),
                        Product.builder().name("Bóng tennis Wilson").description("Bộ 3 bóng tennis chuyên nghiệp")
                                .price(new BigDecimal("189000")).category(outdoor).stock(60).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761222924/bong-Tennis-Wilson-den_ywu2ef.jpg").build(),
                        Product.builder().name("Ván trượt patin Rollerblade").description("Giày trượt patin 8 bánh")
                                .price(new BigDecimal("899000")).category(outdoor).stock(20).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761222991/71UzKFW4jUL._AC_UL495_SR435_495__kqgyto.jpg")
                                .build(),
                        Product.builder().name("Bộ bóng bàn Di Động").description("Set bóng bàn gắn mọi bàn")
                                .price(new BigDecimal("329000")).category(outdoor).stock(32).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761223044/20211211_NfShbdEQvpyhXAH4LaBNpsyO_ak56mm.jpg")
                                .build(),
                        Product.builder().name("Bể bơi phao gia đình").description("Bể bơi phao 3m x 2m")
                                .price(new BigDecimal("799000")).category(outdoor).stock(18).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761223124/58ecbad28a5b1f4e18660c1b75299212_mm0nyf.jpg").build(),
                        Product.builder().name("Xe trượt Hoverboard").description("Xe điện cân bằng 2 bánh")
                                .price(new BigDecimal("2499000")).discountPrice(new BigDecimal("1999000"))
                                .category(outdoor).stock(12).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761223178/Wholesale-2-wheel-scooter-China-hoverboard-8-2_mp5nfk.jpg").build(),
                        Product.builder().name("Set bơi lội kính + ống thở").description("Bộ lặn snorkel cho trẻ em")
                                .price(new BigDecimal("249000")).category(outdoor).stock(38).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761223363/bo-kinh-boi-lan-ong-tho-chinh-hang-2_aux8af.jpg").build()));

                // ARTS & CRAFTS (13 products)
                productRepository.saveAll(Arrays.asList(
                        Product.builder().name("Bộ màu nước 36 màu").description("Màu nước chuyên nghiệp kèm cọ")
                                .price(new BigDecimal("189000")).category(arts).stock(60).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761223460/timthumb_e4c6f1da54404153ae0ecad88a412abc_kujjon.png").build(),
                        Product.builder().name("Bàn vẽ điện tử LCD").description("Bảng vẽ điện tử xóa được 8.5 inch")
                                .price(new BigDecimal("299000")).discountPrice(new BigDecimal("249000")).category(arts)
                                .stock(45).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761223527/z3528273938714-dd23f8a1baccbf3197080ad599e45f06_udbxui.jpg").build(),
                        Product.builder().name("Bộ sáp màu 48 màu").description("Sáp màu cao cấp Crayola")
                                .price(new BigDecimal("149000")).category(arts).stock(70).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761223586/but-sap-dau-mungyo-48-mau-mop-48-2_1714122484_anootv.jpg").build(),
                        Product.builder().name("Bộ đất sét Play-Doh 12 hộp").description("Đất nặn nhiều màu sắc")
                                .price(new BigDecimal("259000")).category(arts).stock(55).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761223639/bot-nan-12-mau-mua-xuan-nam-2024-playdoh-e4831-2024_3_kgx8uq.png").build(),
                        Product.builder().name("Máy chiếu vẽ Projector").description("Máy chiếu hình vẽ cho bé tập")
                                .price(new BigDecimal("399000")).category(arts).stock(30).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761223741/smart_sketche_1_4d594b663c374534ac41992949e1c64d_master_sqiryy.png").build(),
                        Product.builder().name("Bộ tạo vòng tay hạt").description("Set làm vòng tay 500 hạt màu")
                                .price(new BigDecimal("229000")).discountPrice(new BigDecimal("189000")).category(arts)
                                .stock(48).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761223891/00809456cdbf14af8ecf6989ef0f1bbf_nke3nm.jpg").build(),
                        Product.builder().name("Bộ vẽ tranh cát màu").description("Tranh cát 10 mẫu kèm cát màu")
                                .price(new BigDecimal("169000")).category(arts).stock(52).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761223968/0ac83224b108f58bf6c83778327e411a.jpg_720x720q80_urh29h.jpg").build(),
                        Product.builder().name("Bộ sơn dầu 24 màu").description("Màu sơn dầu chuyên nghiệp")
                                .price(new BigDecimal("449000")).category(arts).stock(28).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761224019/81VoeeT2sTL._SL1500__hcozbo.jpg").build(),
                        Product.builder().name("Máy móc giấy Origami").description("300 tờ giấy xếp hình màu")
                                .price(new BigDecimal("700000000")).category(arts).stock(65).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761224209/T2vHLfXnJXXXXXXXXX__836607623.jpg_600x600.jpg__a9xvyu.webp").build(),
                        Product.builder().name("Bộ làm slime galaxy").description("Kit tạo slime thiên hà lấp lánh")
                                .price(new BigDecimal("199000")).category(arts).stock(58).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761224431/bo-may-lam-slime-bong-benh-sang-tao-style4ever-ssc375_4_exc3uk.jpg").build(),
                        Product.builder().name("Bộ vẽ tranh số Paint by Numbers")
                                .description("Tranh tô theo số kèm màu")
                                .price(new BigDecimal("279000")).discountPrice(new BigDecimal("229000")).category(arts)
                                .stock(42).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761224599/tranh_to_mau_so_hoa_a3_paint_by_numbers___colormate_perast_a3_2_2023_09_21_16_10_40_jvhcyu.jpg")
                                .build(),
                        Product.builder().name("Bộ làm trang sức resin").description("Kit đổ resin làm trang sức")
                                .price(new BigDecimal("459000")).category(arts).stock(22).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761224786/68a0fed14752f8b8a462edaf77639e4e_tn_kjqazk.jpg").build(),
                        Product.builder().name("Bộ vẽ tranh 3D Pen").description("Bút vẽ 3D kèm 10 màu nhựa")
                                .price(new BigDecimal("599000")).discountPrice(new BigDecimal("499000")).category(arts)
                                .stock(20).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_250,c_pad,dpr_2.0/v1761225247/c59f8e0f04afb007ca31ae011c500549_eth1e4.jpg").build()));

                // ELECTRONIC & ROBOTS (13 products)
                productRepository.saveAll(Arrays.asList(
                        Product.builder().name("Robot AI thông minh Cozmo").description("Robot AI tương tác cảm xúc")
                                .price(new BigDecimal("2999000")).discountPrice(new BigDecimal("2499000"))
                                .category(electronic).stock(10).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761225613/61brzpaohm_rl6x6s.jpg").build(),
                        Product.builder().name("Drone camera 4K trẻ em").description("Drone điều khiển có camera")
                                .price(new BigDecimal("1899000")).category(electronic).stock(15).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761225767/S25-Mini-Drone-with-4K-Camera-Perfect-Gift-for-Children_tk9uhr.webp").build(),
                        Product.builder().name("Robot biến hình Transformer").description("Robot biến thành xe hơi")
                                .price(new BigDecimal("599000")).category(electronic).stock(35).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761225727/9021955181-513517156_w57byn.jpg").build(),
                        Product.builder().name("Đồng hồ thông minh trẻ em").description("Smartwatch GPS cho bé")
                                .price(new BigDecimal("799000")).discountPrice(new BigDecimal("649000"))
                                .category(electronic).stock(28).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761225903/dong-ho-thong-minh-dinh-vi-tre-em-Y63-1_pub21k.jpg").build(),
                        Product.builder().name("Robot khủng long điều khiển").description("Khủng long robot phun khói")
                                .price(new BigDecimal("899000")).category(electronic).stock(22).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761226006/do-choi-robot-khung-long-thong-thai-dieu-khien-tu-xa-vecto-vtg17_2_auo9u4.jpg").build(),
                        Product.builder().name("Bộ mạch Arduino Starter Kit").description("Kit học lập trình Arduino")
                                .price(new BigDecimal("699000")).category(electronic).stock(25).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761226144/uno-r3-starter-kit-cho-nguoi-moi-bat-dau-hoc-lap-trinh-8iby-1-600x600_hs0cmz.jpg").build(),
                        Product.builder().name("Robot lắp ráp Makeblock").description("Robot DIY lập trình được")
                                .price(new BigDecimal("1499000")).category(electronic).stock(18).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761226274/61MYsWILfdL._SX569__imeagi.jpg").build(),
                        Product.builder().name("Máy chơi game cầm tay retro").description("500 game kinh điển tích hợp")
                                .price(new BigDecimal("499000")).discountPrice(new BigDecimal("399000"))
                                .category(electronic).stock(32).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761226346/game-retro-trang-17-2_tmbxqu.jpg").build(),
                        Product.builder().name("Robot chó cảm biến").description("Chó robot biết đi, sủa, vẫy đuôi")
                                .price(new BigDecimal("1299000")).category(electronic).stock(16).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761226421/c7a7651269aca042641df9fc3232bfc8_sxt91r.jpg").build(),
                        Product.builder().name("Bộ mạch Raspberry Pi 4").description("Máy tính nhỏ học lập trình")
                                .price(new BigDecimal("1599000")).category(electronic).stock(12).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761226476/raspberry-pi-compute-module-IO-board-2-resize-1_plpytm.jpg")
                                .build(),
                        Product.builder().name("Bộ thí nghiệm điện tử 100in1")
                                .description("100 mạch điện tử thí nghiệm")
                                .price(new BigDecimal("549000")).discountPrice(new BigDecimal("459000"))
                                .category(electronic).stock(28).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761226553/Hop-bo-thi-nghiem-may-bien-dong-dien-MVCT_bjmycq.jpg").build(),
                        Product.builder().name("Robot biến hình 5in1").description("1 robot biến thành 5 hình")
                                .price(new BigDecimal("999000")).discountPrice(new BigDecimal("799000"))
                                .category(electronic).stock(18).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761226607/Do-choi-o-to-bien-hinh-robot-5-in-1-1_gtjp1z.png").build(),
                        Product.builder().name("Robot lắp ghép sáng tạo").description("500 chi tiết lắp tự do")
                                .price(new BigDecimal("749000")).category(electronic).stock(22).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761226670/do-choi-lap-rap-lloyd-va-chien-giap-jet-lego-ninjago-71845_1_ogjwgt.jpg")
                                .build()));

                // BOARD GAMES & PUZZLE (13 products)
                productRepository.saveAll(Arrays.asList(
                        Product.builder().name("Cờ tỷ phú Monopoly Việt Nam").description("Monopoly phiên bản Việt Nam")
                                .price(new BigDecimal("399000")).discountPrice(new BigDecimal("329000")).category(board)
                                .stock(40).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761226726/C1009_2_h1ebws.jpg").build(),
                        Product.builder().name("Uno cards phiên bản đặc biệt")
                                .description("Bài UNO 108 lá nhiều hiệu ứng")
                                .price(new BigDecimal("129000")).category(board).stock(80).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761226782/gxy78_2_nmltzb.jpg").build(),
                        Product.builder().name("Cờ vua nam châm cao cấp").description("Bàn cờ vua gỗ từ tính 32cm")
                                .price(new BigDecimal("299000")).category(board).stock(35).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761226885/bo-co-vua-nam-cham-cao-cap-u3-3810-12.jpg_jho5df.webp").build(),
                        Product.builder().name("Jenga tháp gỗ rút thanh").description("54 thanh gỗ thử thách")
                                .price(new BigDecimal("189000")).category(board).stock(55).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761226933/jenga_lh1jct.jpg").build(),
                        Product.builder().name("Scrabble ghép chữ tiếng Anh")
                                .description("Trò chơi ghép từ học Anh văn")
                                .price(new BigDecimal("349000")).category(board).stock(30).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761227109/do-choi-mattel-games-scrabble-tro-choi-ghep-chu-tieng-anh_timmn3.jpg").build(),
                        Product.builder().name("Cluedo phá án bí ẩn").description("Trò chơi trinh thám hấp dẫn")
                                .price(new BigDecimal("459000")).discountPrice(new BigDecimal("389000")).category(board)
                                .stock(25).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761227046/GUEST_22c5f872-a8e8-4f40-86a6-f170eb623798_minjua.jpg").build(),
                        Product.builder().name("Cờ cá ngựa 6 người chơi").description("Bàn cờ cá ngựa gia đình")
                                .price(new BigDecimal("149000")).category(board).stock(60).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761227176/f79bee30e12de4b70d4b76929fca8c11_uc11kc.jpg").build(),
                        Product.builder().name("Domino 100 quân gỗ màu").description("Domino gỗ xếp hình sáng tạo")
                                .price(new BigDecimal("199000")).category(board).stock(48).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/v1761227195/eb21ebf16ea41129dd1caa56d214e241_zt6kfv.jpg").build(),
                        Product.builder().name("Bài Poker cao cấp PVC").description("Bộ bài Poker chống nước")
                                .price(new BigDecimal("259000")).category(board).stock(42).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761227279/1034-bo-bai-tay-bang-nhua-pvc-mau-den_vxsgxg.jpg").build(),
                        Product.builder().name("Rubik's Cube 4x4 Revenge").description("Rubik 4x4 cao cấp tốc độ")
                                .price(new BigDecimal("199000")).discountPrice(new BigDecimal("169000")).category(board)
                                .stock(45).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761227328/rubiks-cube-4x4_ub6zvi.jpg").build(),
                        Product.builder().name("Mê cung 3D Perplexus").description("Bóng mê cung 3D 100 chướng ngại")
                                .price(new BigDecimal("449000")).category(board).stock(22).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761227374/0d358cfbb951452f547fc322fd445cdb_i3s6av.jpg").build(),
                        Product.builder().name("Catan Settlers of Catan").description("Trò chơi chiến lược phát triển")
                                .price(new BigDecimal("699000")).category(board).stock(18).featured(true)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761227520/catan3_ftwnlf.jpg").build(),
                        Product.builder().name("Bộ bài Tây 52 lá plastic").description("Bài nhựa cao cấp chống nước")
                                .price(new BigDecimal("99000")).category(board).stock(100).featured(false)
                                .imageUrl("https://res.cloudinary.com/t4m/image/upload/f_auto,q_auto,w_300,h_200,c_pad,dpr_2.0/v1761227559/3aa0aaeba3d7e95497f721fb63bf1470_oqbmle.jpg")
                                .build()));

                System.out.println("✅ Initialized 8 categories and 100 products successfully!");
            }
        };
    }
}