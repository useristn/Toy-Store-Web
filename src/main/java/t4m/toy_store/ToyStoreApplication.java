package t4m.toy_store;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
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
public class ToyStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToyStoreApplication.class, args);
    }

    @Bean
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
    public ApplicationRunner initAdminUser(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@toystore.com";
            
            // Check if admin user already exists
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                // Get ADMIN role
                Role adminRole = roleRepository.findByRname("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found. Please ensure initRoles runs first."));
                
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
    public ApplicationRunner initCategories(CategoryRepository categoryRepository, ProductRepository productRepository) {
        return args -> {
            if (categoryRepository.count() == 0) {
                // Create categories
                Category dolls = Category.builder().name("Búp bê & Công chúa").description("Búp bê xinh đẹp").icon("👸").build();
                Category vehicles = Category.builder().name("Xe & Phi thuyền").description("Phương tiện vũ trụ").icon("🚀").build();
                Category building = Category.builder().name("Xếp hình & Ghép").description("Đồ chơi sáng tạo").icon("🧩").build();
                Category science = Category.builder().name("Khoa học & Thí nghiệm").description("Học tập vui vẻ").icon("🔬").build();
                Category outdoor = Category.builder().name("Ngoài trời & Thể thao").description("Vận động ngoài trời").icon("⚽").build();
                Category arts = Category.builder().name("Nghệ thuật & Sáng tạo").description("Phát triển nghệ thuật").icon("🎨").build();
                Category electronic = Category.builder().name("Điện tử & Robot").description("Công nghệ hiện đại").icon("🤖").build();
                Category board = Category.builder().name("Board Game & Trí tuệ").description("Trò chơi trí tuệ").icon("🎲").build();
                
                categoryRepository.saveAll(Arrays.asList(dolls, vehicles, building, science, outdoor, arts, electronic, board));
                
                // Create sample products - DOLLS & PRINCESSES (20 products)
                productRepository.saveAll(Arrays.asList(
                    Product.builder().name("Búp bê Công chúa Elsa").description("Công chúa băng giá xinh đẹp với bộ váy lung linh")
                        .price(new BigDecimal("299000")).discountPrice(new BigDecimal("249000")).category(dolls).stock(50).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/e91e63/FFFFFF?text=Elsa").build(),
                    Product.builder().name("Búp bê Anna cổ tích").description("Công chúa dũng cảm với trang phục đẹp mắt")
                        .price(new BigDecimal("289000")).category(dolls).stock(45).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ff4081/FFFFFF?text=Anna").build(),
                    Product.builder().name("Búp bê Barbie Dream House").description("Búp bê Barbie sang trọng với ngôi nhà mơ ước")
                        .price(new BigDecimal("1299000")).discountPrice(new BigDecimal("999000")).category(dolls).stock(20).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/ec407a/FFFFFF?text=Barbie").build(),
                    Product.builder().name("Búp bê Rapunzel tóc dài").description("Công chúa tóc dài thần kỳ 30cm")
                        .price(new BigDecimal("349000")).category(dolls).stock(35).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/f06292/FFFFFF?text=Rapunzel").build(),
                    Product.builder().name("Búp bê Ariel nàng tiên cá").description("Nàng tiên cá xinh đẹp với đuôi cá lấp lánh")
                        .price(new BigDecimal("329000")).category(dolls).stock(40).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=Ariel").build(),
                    Product.builder().name("Búp bê Belle người đẹp").description("Công chúa Belle yêu đọc sách")
                        .price(new BigDecimal("319000")).category(dolls).stock(38).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ffc107/FFFFFF?text=Belle").build(),
                    Product.builder().name("Búp bê Jasmine công chúa").description("Công chúa Jasmine với trang phục Ả Rập")
                        .price(new BigDecimal("309000")).discountPrice(new BigDecimal("269000")).category(dolls).stock(42).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/00acc1/FFFFFF?text=Jasmine").build(),
                    Product.builder().name("Búp bê Moana dũng cảm").description("Công chúa Moana phiêu lưu đại dương")
                        .price(new BigDecimal("339000")).category(dolls).stock(33).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/26a69a/FFFFFF?text=Moana").build(),
                    Product.builder().name("Búp bê Sofia nhí nhảnh").description("Công chúa Sofia nhỏ đáng yêu")
                        .price(new BigDecimal("279000")).category(dolls).stock(55).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/9c27b0/FFFFFF?text=Sofia").build(),
                    Product.builder().name("Búp bê Cinderella lọ lem").description("Công chúa Lọ Lem với giày thủy tinh")
                        .price(new BigDecimal("299000")).category(dolls).stock(47).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/3f51b5/FFFFFF?text=Cinderella").build(),
                    Product.builder().name("Set búp bê gia đình hạnh phúc").description("Bộ búp bê gia đình 4 người")
                        .price(new BigDecimal("599000")).discountPrice(new BigDecimal("499000")).category(dolls).stock(25).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/e91e63/FFFFFF?text=Family").build(),
                    Product.builder().name("Búp bê baby doll").description("Em bé búp bê biết khóc, cười")
                        .price(new BigDecimal("459000")).category(dolls).stock(30).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ff4081/FFFFFF?text=Baby").build(),
                    Product.builder().name("Búp bê LOL Surprise").description("Búp bê bất ngờ với nhiều phụ kiện")
                        .price(new BigDecimal("199000")).category(dolls).stock(60).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/ec407a/FFFFFF?text=LOL").build(),
                    Product.builder().name("Búp bê Encanto Mirabel").description("Cô gái kỳ diệu từ Encanto")
                        .price(new BigDecimal("329000")).category(dolls).stock(28).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/f06292/FFFFFF?text=Mirabel").build(),
                    Product.builder().name("Búp bê Mulan chiến binh").description("Nữ chiến binh dũng cảm Mulan")
                        .price(new BigDecimal("339000")).category(dolls).stock(32).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/e91e63/FFFFFF?text=Mulan").build(),
                    Product.builder().name("Búp bê Merida công chúa tóc đỏ").description("Công chúa cung thủ dũng cảm")
                        .price(new BigDecimal("349000")).category(dolls).stock(27).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ff5722/FFFFFF?text=Merida").build(),
                    Product.builder().name("Búp bê Tiana và ếch").description("Công chúa Tiana với người bạn ếch")
                        .price(new BigDecimal("319000")).category(dolls).stock(31).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/4caf50/FFFFFF?text=Tiana").build(),
                    Product.builder().name("Búp bê Pocahontas thổ dân").description("Công chúa Pocahontas và thiên nhiên")
                        .price(new BigDecimal("309000")).category(dolls).stock(29).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/795548/FFFFFF?text=Pocahontas").build(),
                    Product.builder().name("Set búp bê Disney Princess").description("Bộ 5 công chúa Disney")
                        .price(new BigDecimal("899000")).discountPrice(new BigDecimal("749000")).category(dolls).stock(15).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/e91e63/FFFFFF?text=Disney+Set").build(),
                    Product.builder().name("Búp bê Aurora ngủ trong rừng").description("Công chúa ngủ trong rừng xinh đẹp")
                        .price(new BigDecimal("329000")).category(dolls).stock(34).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/f8bbd0/FFFFFF?text=Aurora").build()
                ));
                
                // VEHICLES & SPACESHIPS (25 products)
                productRepository.saveAll(Arrays.asList(
                    Product.builder().name("Phi thuyền Siêu tốc X-Wing").description("Phi thuyền chiến đấu tốc độ ánh sáng")
                        .price(new BigDecimal("599000")).discountPrice(new BigDecimal("499000")).category(vehicles).stock(30).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/1a237e/FFFFFF?text=X-Wing").build(),
                    Product.builder().name("Xe ô tô điều khiển từ xa").description("Xe đua điều khiển tốc độ cao")
                        .price(new BigDecimal("399000")).category(vehicles).stock(45).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/3f51b5/FFFFFF?text=RC+Car").build(),
                    Product.builder().name("Tàu vũ trụ Apollo").description("Tàu vũ trụ Apollo mô hình chi tiết")
                        .price(new BigDecimal("799000")).category(vehicles).stock(20).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/283593/FFFFFF?text=Apollo").build(),
                    Product.builder().name("Xe tăng chiến đấu").description("Xe tăng quân sự điều khiển")
                        .price(new BigDecimal("549000")).category(vehicles).stock(35).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/424242/FFFFFF?text=Tank").build(),
                    Product.builder().name("Máy bay phản lực F-16").description("Máy bay chiến đấu F-16 mô hình")
                        .price(new BigDecimal("449000")).discountPrice(new BigDecimal("379000")).category(vehicles).stock(28).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/1976d2/FFFFFF?text=F16").build(),
                    Product.builder().name("Tàu ngầm thám hiểm").description("Tàu ngầm thám hiểm đại dương")
                        .price(new BigDecimal("429000")).category(vehicles).stock(32).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/006064/FFFFFF?text=Submarine").build(),
                    Product.builder().name("Xe cứu hỏa siêu tốc").description("Xe cứu hỏa với thang cứu nạn")
                        .price(new BigDecimal("379000")).category(vehicles).stock(40).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/d32f2f/FFFFFF?text=Fire+Truck").build(),
                    Product.builder().name("Xe cảnh sát tuần tra").description("Xe cảnh sát với còi hú")
                        .price(new BigDecimal("359000")).category(vehicles).stock(38).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/1976d2/FFFFFF?text=Police").build(),
                    Product.builder().name("Xe cẩu công trình").description("Xe cẩu xây dựng lớn")
                        .price(new BigDecimal("469000")).category(vehicles).stock(25).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ff9800/FFFFFF?text=Crane").build(),
                    Product.builder().name("Xe đua F1 Lightning").description("Xe đua F1 tốc độ siêu nhanh")
                        .price(new BigDecimal("519000")).discountPrice(new BigDecimal("439000")).category(vehicles).stock(33).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/f44336/FFFFFF?text=F1").build(),
                    Product.builder().name("Tên lửa Falcon Heavy").description("Tên lửa SpaceX Falcon Heavy")
                        .price(new BigDecimal("899000")).category(vehicles).stock(18).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/212121/FFFFFF?text=Falcon").build(),
                    Product.builder().name("Xe jeep địa hình").description("Xe jeep leo núi vượt địa hình")
                        .price(new BigDecimal("489000")).category(vehicles).stock(27).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/795548/FFFFFF?text=Jeep").build(),
                    Product.builder().name("Máy bay trực thăng").description("Trực thăng cứu hộ điều khiển")
                        .price(new BigDecimal("529000")).category(vehicles).stock(29).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ff5722/FFFFFF?text=Helicopter").build(),
                    Product.builder().name("Tàu hỏa cao tốc Bullet").description("Tàu hỏa siêu tốc Nhật Bản")
                        .price(new BigDecimal("699000")).discountPrice(new BigDecimal("599000")).category(vehicles).stock(22).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/2196f3/FFFFFF?text=Bullet+Train").build(),
                    Product.builder().name("Xe bus du lịch").description("Xe bus 2 tầng du lịch London")
                        .price(new BigDecimal("419000")).category(vehicles).stock(31).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/e53935/FFFFFF?text=Bus").build(),
                    Product.builder().name("Tàu chiến USS Missouri").description("Tàu chiến hải quân mô hình")
                        .price(new BigDecimal("999000")).category(vehicles).stock(15).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/37474f/FFFFFF?text=Battleship").build(),
                    Product.builder().name("Xe mô tô đua Ducati").description("Mô tô đua Ducati tốc độ")
                        .price(new BigDecimal("339000")).category(vehicles).stock(36).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/d32f2f/FFFFFF?text=Ducati").build(),
                    Product.builder().name("Phi thuyền Millennium Falcon").description("Phi thuyền huyền thoại Star Wars")
                        .price(new BigDecimal("1299000")).discountPrice(new BigDecimal("1099000")).category(vehicles).stock(12).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/424242/FFFFFF?text=Falcon").build(),
                    Product.builder().name("Xe tải container").description("Xe tải chở container lớn")
                        .price(new BigDecimal("449000")).category(vehicles).stock(26).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/607d8b/FFFFFF?text=Truck").build(),
                    Product.builder().name("Tàu cướp biển Caribbean").description("Tàu cướp biển với cờ đầu lâu")
                        .price(new BigDecimal("759000")).category(vehicles).stock(19).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/4e342e/FFFFFF?text=Pirate+Ship").build(),
                    Product.builder().name("Xe công nông xúc đất").description("Xe xúc công trình lớn")
                        .price(new BigDecimal("399000")).category(vehicles).stock(34).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ffa000/FFFFFF?text=Excavator").build(),
                    Product.builder().name("UFO đĩa bay bí ẩn").description("Đĩa bay người ngoài hành tinh phát sáng")
                        .price(new BigDecimal("279000")).discountPrice(new BigDecimal("229000")).category(vehicles).stock(42).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/00e676/FFFFFF?text=UFO").build(),
                    Product.builder().name("Xe tải rác thông minh").description("Xe thu gom rác tự động")
                        .price(new BigDecimal("369000")).category(vehicles).stock(28).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/689f38/FFFFFF?text=Garbage+Truck").build(),
                    Product.builder().name("Tên lửa Saturn V NASA").description("Tên lửa đưa người lên mặt trăng")
                        .price(new BigDecimal("1199000")).category(vehicles).stock(10).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/1a237e/FFFFFF?text=Saturn+V").build(),
                    Product.builder().name("Set phương tiện cứu hộ").description("Bộ 5 xe cứu hộ khẩn cấp")
                        .price(new BigDecimal("699000")).discountPrice(new BigDecimal("579000")).category(vehicles).stock(24).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/ff5722/FFFFFF?text=Rescue+Set").build()
                ));
                
                // BUILDING & PUZZLES (20 products)
                productRepository.saveAll(Arrays.asList(
                    Product.builder().name("Lego City Trung tâm vũ trụ").description("Bộ xếp hình trung tâm vũ trụ NASA 1000 chi tiết")
                        .price(new BigDecimal("899000")).category(building).stock(20).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=Lego+City").build(),
                    Product.builder().name("Lego Technic siêu xe").description("Xếp hình siêu xe Lamborghini")
                        .price(new BigDecimal("1299000")).discountPrice(new BigDecimal("1099000")).category(building).stock(15).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/ffc107/FFFFFF?text=Technic").build(),
                    Product.builder().name("Lego Star Wars AT-AT").description("Bộ xếp hình AT-AT Walker khổng lồ")
                        .price(new BigDecimal("1599000")).category(building).stock(12).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/424242/FFFFFF?text=AT-AT").build(),
                    Product.builder().name("Puzzle 1000 mảnh thiên hà").description("Tranh ghép hình thiên hà đẹp mắt")
                        .price(new BigDecimal("199000")).category(building).stock(50).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/673ab7/FFFFFF?text=Puzzle+1000").build(),
                    Product.builder().name("Lego Friends công viên giải trí").description("Công viên vui chơi với nhiều trò chơi")
                        .price(new BigDecimal("799000")).category(building).stock(25).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ec407a/FFFFFF?text=Friends").build(),
                    Product.builder().name("Minecraft thế giới khối vuông").description("Bộ xếp hình Minecraft 500 chi tiết")
                        .price(new BigDecimal("549000")).discountPrice(new BigDecimal("459000")).category(building).stock(35).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/689f38/FFFFFF?text=Minecraft").build(),
                    Product.builder().name("Lego Harry Potter lâu đài").description("Lâu đài Hogwarts huyền thoại")
                        .price(new BigDecimal("1799000")).category(building).stock(10).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/3f51b5/FFFFFF?text=Hogwarts").build(),
                    Product.builder().name("Puzzle 3D tháp Eiffel").description("Puzzle 3D tháp Eiffel Paris 216 mảnh")
                        .price(new BigDecimal("349000")).category(building).stock(30).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/795548/FFFFFF?text=Eiffel").build(),
                    Product.builder().name("Lego Ninjago rồng thần").description("Rồng thần ninja với 800 chi tiết")
                        .price(new BigDecimal("699000")).category(building).stock(22).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/d32f2f/FFFFFF?text=Ninjago").build(),
                    Product.builder().name("Rubik's Cube 3x3 tốc độ").description("Rubik cube tốc độ chuyên nghiệp")
                        .price(new BigDecimal("149000")).category(building).stock(80).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/ff9800/FFFFFF?text=Rubik").build(),
                    Product.builder().name("Lego Architecture Big Ben").description("Mô hình Big Ben London chi tiết")
                        .price(new BigDecimal("899000")).category(building).stock(18).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/795548/FFFFFF?text=Big+Ben").build(),
                    Product.builder().name("Puzzle 2000 mảnh thế giới").description("Bản đồ thế giới tranh ghép lớn")
                        .price(new BigDecimal("279000")).discountPrice(new BigDecimal("229000")).category(building).stock(40).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/00796b/FFFFFF?text=World+Map").build(),
                    Product.builder().name("Lego Disney lâu đài công chúa").description("Lâu đài Disney Princess tuyệt đẹp")
                        .price(new BigDecimal("1299000")).category(building).stock(16).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/e91e63/FFFFFF?text=Castle").build(),
                    Product.builder().name("Khối nam châm Magformers").description("Bộ khối nam châm ghép hình 50 chi tiết")
                        .price(new BigDecimal("599000")).category(building).stock(28).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/9c27b0/FFFFFF?text=Magformers").build(),
                    Product.builder().name("Puzzle 500 mảnh động vật").description("Tranh ghép động vật hoang dã")
                        .price(new BigDecimal("159000")).category(building).stock(55).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/4caf50/FFFFFF?text=Animals").build(),
                    Product.builder().name("Lego Creator ngôi nhà bãi biển").description("Ngôi nhà nghỉ dưỡng bên bờ biển")
                        .price(new BigDecimal("749000")).category(building).stock(24).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=Beach+House").build(),
                    Product.builder().name("Domino Rally 200 quân").description("Bộ domino 200 quân màu sắc")
                        .price(new BigDecimal("189000")).category(building).stock(45).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/f44336/FFFFFF?text=Domino").build(),
                    Product.builder().name("Lego Jurassic World khủng long").description("Bộ xếp hình khủng long T-Rex")
                        .price(new BigDecimal("999000")).discountPrice(new BigDecimal("849000")).category(building).stock(19).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/689f38/FFFFFF?text=T-Rex").build(),
                    Product.builder().name("Puzzle kim cương 5D").description("Tranh ghép kim cương lấp lánh")
                        .price(new BigDecimal("249000")).category(building).stock(38).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/e91e63/FFFFFF?text=Diamond").build(),
                    Product.builder().name("Lego Duplo trang trại vui vẻ").description("Bộ xếp hình trang trại cho bé nhỏ")
                        .price(new BigDecimal("449000")).category(building).stock(32).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/8bc34a/FFFFFF?text=Farm").build()
                ));
                
                // SCIENCE & EXPERIMENTS (15 products)
                productRepository.saveAll(Arrays.asList(
                    Product.builder().name("Bộ thí nghiệm Vũ trụ 100 thí nghiệm").description("Khám phá 100 thí nghiệm khoa học tuyệt vời")
                        .price(new BigDecimal("459000")).category(science).stock(40).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/ffc107/FFFFFF?text=Science+100").build(),
                    Product.builder().name("Kính thiên văn khám phá sao").description("Kính thiên văn chuyên nghiệp 70mm")
                        .price(new BigDecimal("899000")).discountPrice(new BigDecimal("749000")).category(science).stock(25).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/1a237e/FFFFFF?text=Telescope").build(),
                    Product.builder().name("Bộ hóa học nhỏ").description("Thí nghiệm hóa học an toàn cho trẻ em")
                        .price(new BigDecimal("389000")).category(science).stock(35).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/4caf50/FFFFFF?text=Chemistry").build(),
                    Product.builder().name("Kính hiển vi sinh học").description("Kính hiển vi học sinh 1200x")
                        .price(new BigDecimal("599000")).category(science).stock(30).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=Microscope").build(),
                    Product.builder().name("Robot lập trình STEM").description("Robot học lập trình cho trẻ em")
                        .price(new BigDecimal("1299000")).discountPrice(new BigDecimal("999000")).category(science).stock(20).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/ff5722/FFFFFF?text=Robot+STEM").build(),
                    Product.builder().name("Bộ thí nghiệm núi lửa").description("Tạo núi lửa phun trào tại nhà")
                        .price(new BigDecimal("249000")).category(science).stock(50).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/d84315/FFFFFF?text=Volcano").build(),
                    Product.builder().name("Bộ trồng cây thủy canh").description("Học cách trồng cây không cần đất")
                        .price(new BigDecimal("329000")).category(science).stock(28).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/8bc34a/FFFFFF?text=Hydroponic").build(),
                    Product.builder().name("Mô hình hệ mặt trời").description("Hệ mặt trời quay tự động có đèn")
                        .price(new BigDecimal("549000")).category(science).stock(22).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/1976d2/FFFFFF?text=Solar+System").build(),
                    Product.builder().name("Bộ thí nghiệm điện từ").description("Khám phá điện và từ trường")
                        .price(new BigDecimal("419000")).category(science).stock(32).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ffc107/FFFFFF?text=Electricity").build(),
                    Product.builder().name("Kính lúp khoa học 10x").description("Kính lúp cầm tay phóng đại 10 lần")
                        .price(new BigDecimal("159000")).category(science).stock(60).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/673ab7/FFFFFF?text=Magnifier").build(),
                    Product.builder().name("Bộ thí nghiệm slimy slime").description("Tạo chất nhờn ma thuật nhiều màu")
                        .price(new BigDecimal("189000")).discountPrice(new BigDecimal("149000")).category(science).stock(55).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/4caf50/FFFFFF?text=Slime").build(),
                    Product.builder().name("Bộ khai quật hóa thạch khủng long").description("Khám phá hóa thạch như nhà khảo cổ")
                        .price(new BigDecimal("279000")).category(science).stock(38).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/795548/FFFFFF?text=Fossil").build(),
                    Product.builder().name("Máy phát điện gió mini").description("Học về năng lượng tái tạo")
                        .price(new BigDecimal("369000")).category(science).stock(26).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=Wind+Power").build(),
                    Product.builder().name("Bộ thí nghiệm pin Lemon").description("Tạo điện từ trái cây")
                        .price(new BigDecimal("219000")).category(science).stock(45).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/cddc39/FFFFFF?text=Lemon+Battery").build(),
                    Product.builder().name("Kit Arduino cho trẻ em").description("Học lập trình điện tử cơ bản")
                        .price(new BigDecimal("799000")).discountPrice(new BigDecimal("679000")).category(science).stock(18).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=Arduino").build()
                ));

                System.out.println("Initialized 8 categories and 100+ sample products");
                
                // OUTDOOR & SPORTS (20 products)
                productRepository.saveAll(Arrays.asList(
                    Product.builder().name("Bóng đá World Cup 2024").description("Bóng đá chính thức World Cup size 5")
                        .price(new BigDecimal("299000")).category(outdoor).stock(50).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/00c853/FFFFFF?text=Football").build(),
                    Product.builder().name("Xe đạp thể thao trẻ em").description("Xe đạp 16 inch cho bé 5-8 tuổi")
                        .price(new BigDecimal("1499000")).discountPrice(new BigDecimal("1299000")).category(outdoor).stock(15).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/2196f3/FFFFFF?text=Bicycle").build(),
                    Product.builder().name("Bóng rổ NBA Professional").description("Bóng rổ cao cấp size 7")
                        .price(new BigDecimal("349000")).category(outdoor).stock(40).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ff9800/FFFFFF?text=Basketball").build(),
                    Product.builder().name("Ván trượt Skateboard Pro").description("Ván trượt chuyên nghiệp 7 lớp")
                        .price(new BigDecimal("599000")).category(outdoor).stock(25).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/673ab7/FFFFFF?text=Skateboard").build(),
                    Product.builder().name("Bóng chuyền Mikasa").description("Bóng chuyền thi đấu chính hãng")
                        .price(new BigDecimal("279000")).category(outdoor).stock(35).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ffc107/FFFFFF?text=Volleyball").build(),
                    Product.builder().name("Xe scooter 3 bánh").description("Xe scooter phát sáng cho bé")
                        .price(new BigDecimal("699000")).discountPrice(new BigDecimal("599000")).category(outdoor).stock(30).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/e91e63/FFFFFF?text=Scooter").build(),
                    Product.builder().name("Bộ cầu lông gia đình").description("Set cầu lông 4 vợt kèm lưới")
                        .price(new BigDecimal("449000")).category(outdoor).stock(28).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/4caf50/FFFFFF?text=Badminton").build(),
                    Product.builder().name("Bóng tennis Wilson").description("Bộ 3 bóng tennis chuyên nghiệp")
                        .price(new BigDecimal("189000")).category(outdoor).stock(60).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/cddc39/FFFFFF?text=Tennis").build(),
                    Product.builder().name("Ván trượt patin Rollerblade").description("Giày trượt patin 8 bánh")
                        .price(new BigDecimal("899000")).category(outdoor).stock(20).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/9c27b0/FFFFFF?text=Rollerblades").build(),
                    Product.builder().name("Bộ bóng bàn Di Động").description("Set bóng bàn gắn mọi bàn")
                        .price(new BigDecimal("329000")).category(outdoor).stock(32).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=Table+Tennis").build(),
                    Product.builder().name("Dù bay Paraglider mini").description("Dù bay điều khiển ngoài trời")
                        .price(new BigDecimal("259000")).category(outdoor).stock(45).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/03a9f4/FFFFFF?text=Kite").build(),
                    Product.builder().name("Bóng ném Dodgeball").description("Set 6 bóng ném mềm an toàn")
                        .price(new BigDecimal("399000")).discountPrice(new BigDecimal("329000")).category(outdoor).stock(28).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/f44336/FFFFFF?text=Dodgeball").build(),
                    Product.builder().name("Nhà bóng kèm 100 bóng").description("Nhà bóng di động có thể gấp gọn")
                        .price(new BigDecimal("549000")).category(outdoor).stock(22).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ff5722/FFFFFF?text=Ball+Pit").build(),
                    Product.builder().name("Bể bơi phao gia đình").description("Bể bơi phao 3m x 2m")
                        .price(new BigDecimal("799000")).category(outdoor).stock(18).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=Pool").build(),
                    Product.builder().name("Trampoline mini 100cm").description("Bạt nhún tập thể dục tại nhà")
                        .price(new BigDecimal("649000")).category(outdoor).stock(24).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/8bc34a/FFFFFF?text=Trampoline").build(),
                    Product.builder().name("Cung tên Archery Set").description("Bộ cung tên an toàn cho trẻ")
                        .price(new BigDecimal("379000")).category(outdoor).stock(30).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/795548/FFFFFF?text=Archery").build(),
                    Product.builder().name("Frisbee đĩa bay phát sáng").description("Đĩa bay Frisbee ban đêm")
                        .price(new BigDecimal("159000")).category(outdoor).stock(55).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/00e676/FFFFFF?text=Frisbee").build(),
                    Product.builder().name("Bộ golf mini trẻ em").description("Set golf 3 gậy cho bé")
                        .price(new BigDecimal("489000")).category(outdoor).stock(26).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/4caf50/FFFFFF?text=Golf").build(),
                    Product.builder().name("Xe trượt Hoverboard").description("Xe điện cân bằng 2 bánh")
                        .price(new BigDecimal("2499000")).discountPrice(new BigDecimal("1999000")).category(outdoor).stock(12).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/9c27b0/FFFFFF?text=Hoverboard").build(),
                    Product.builder().name("Set bơi lội kính + ống thở").description("Bộ lặn snorkel cho trẻ em")
                        .price(new BigDecimal("249000")).category(outdoor).stock(38).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=Snorkel").build()
                ));
                
                // ARTS & CRAFTS (20 products)
                productRepository.saveAll(Arrays.asList(
                    Product.builder().name("Bộ màu nước 36 màu").description("Màu nước chuyên nghiệp kèm cọ")
                        .price(new BigDecimal("189000")).category(arts).stock(60).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/e91e63/FFFFFF?text=Watercolor").build(),
                    Product.builder().name("Bàn vẽ điện tử LCD").description("Bảng vẽ điện tử xóa được 8.5 inch")
                        .price(new BigDecimal("299000")).discountPrice(new BigDecimal("249000")).category(arts).stock(45).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=LCD+Tablet").build(),
                    Product.builder().name("Bộ sáp màu 48 màu").description("Sáp màu cao cấp Crayola")
                        .price(new BigDecimal("149000")).category(arts).stock(70).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ffc107/FFFFFF?text=Crayons").build(),
                    Product.builder().name("Bộ đất sét Play-Doh 12 hộp").description("Đất nặn nhiều màu sắc")
                        .price(new BigDecimal("259000")).category(arts).stock(55).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/ff5722/FFFFFF?text=Play-Doh").build(),
                    Product.builder().name("Máy chiếu vẽ Projector").description("Máy chiếu hình vẽ cho bé tập")
                        .price(new BigDecimal("399000")).category(arts).stock(30).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/9c27b0/FFFFFF?text=Projector").build(),
                    Product.builder().name("Bộ thêu chữ thập").description("Kit thêu tranh hoa đào")
                        .price(new BigDecimal("179000")).category(arts).stock(40).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/e91e63/FFFFFF?text=Cross+Stitch").build(),
                    Product.builder().name("Bộ tạo vòng tay hạt").description("Set làm vòng tay 500 hạt màu")
                        .price(new BigDecimal("229000")).discountPrice(new BigDecimal("189000")).category(arts).stock(48).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ec407a/FFFFFF?text=Beads").build(),
                    Product.builder().name("Bộ vẽ tranh cát màu").description("Tranh cát 10 mẫu kèm cát màu")
                        .price(new BigDecimal("169000")).category(arts).stock(52).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ffa726/FFFFFF?text=Sand+Art").build(),
                    Product.builder().name("Bộ tô tượng thạch cao").description("12 tượng động vật tô màu")
                        .price(new BigDecimal("299000")).category(arts).stock(35).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/8d6e63/FFFFFF?text=Plaster").build(),
                    Product.builder().name("Bộ sơn dầu 24 màu").description("Màu sơn dầu chuyên nghiệp")
                        .price(new BigDecimal("449000")).category(arts).stock(28).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/795548/FFFFFF?text=Oil+Paint").build(),
                    Product.builder().name("Máy móc giấy Origami").description("300 tờ giấy xếp hình màu")
                        .price(new BigDecimal("129000")).category(arts).stock(65).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=Origami").build(),
                    Product.builder().name("Bộ làm slime galaxy").description("Kit tạo slime thiên hà lấp lánh")
                        .price(new BigDecimal("199000")).category(arts).stock(58).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/673ab7/FFFFFF?text=Slime+Kit").build(),
                    Product.builder().name("Bộ vẽ tranh số Paint by Numbers").description("Tranh tô theo số kèm màu")
                        .price(new BigDecimal("279000")).discountPrice(new BigDecimal("229000")).category(arts).stock(42).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/4caf50/FFFFFF?text=Paint+Numbers").build(),
                    Product.builder().name("Bộ làm nến thơm").description("Kit làm nến thơm tại nhà")
                        .price(new BigDecimal("329000")).category(arts).stock(32).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ff9800/FFFFFF?text=Candle+Making").build(),
                    Product.builder().name("Bộ thiết kế thời trang").description("Tập vẽ thiết kế 50 mẫu váy")
                        .price(new BigDecimal("249000")).category(arts).stock(38).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/e91e63/FFFFFF?text=Fashion+Design").build(),
                    Product.builder().name("Bộ làm xà phòng handmade").description("Kit làm xà phòng thiên nhiên")
                        .price(new BigDecimal("289000")).category(arts).stock(36).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/9c27b0/FFFFFF?text=Soap+Making").build(),
                    Product.builder().name("Bộ vẽ graffiti bằng bút xịt").description("12 bút xịt màu không độc hại")
                        .price(new BigDecimal("399000")).category(arts).stock(25).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/212121/FFFFFF?text=Spray+Paint").build(),
                    Product.builder().name("Bộ làm trang sức resin").description("Kit đổ resin làm trang sức")
                        .price(new BigDecimal("459000")).category(arts).stock(22).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=Resin").build(),
                    Product.builder().name("Bộ vẽ tranh 3D Pen").description("Bút vẽ 3D kèm 10 màu nhựa")
                        .price(new BigDecimal("599000")).discountPrice(new BigDecimal("499000")).category(arts).stock(20).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/673ab7/FFFFFF?text=3D+Pen").build(),
                    Product.builder().name("Bộ làm hoa giấy khổng lồ").description("Kit làm 20 bông hoa giấy")
                        .price(new BigDecimal("219000")).category(arts).stock(44).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/e91e63/FFFFFF?text=Paper+Flowers").build()
                ));
                
                // ELECTRONIC & ROBOTS (20 products)
                productRepository.saveAll(Arrays.asList(
                    Product.builder().name("Robot AI thông minh Cozmo").description("Robot AI tương tác cảm xúc")
                        .price(new BigDecimal("2999000")).discountPrice(new BigDecimal("2499000")).category(electronic).stock(10).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=Cozmo").build(),
                    Product.builder().name("Drone camera 4K trẻ em").description("Drone điều khiển có camera")
                        .price(new BigDecimal("1899000")).category(electronic).stock(15).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/2196f3/FFFFFF?text=Drone").build(),
                    Product.builder().name("Robot biến hình Transformer").description("Robot biến thành xe hơi")
                        .price(new BigDecimal("599000")).category(electronic).stock(35).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/ffc107/FFFFFF?text=Transformer").build(),
                    Product.builder().name("Đồng hồ thông minh trẻ em").description("Smartwatch GPS cho bé")
                        .price(new BigDecimal("799000")).discountPrice(new BigDecimal("649000")).category(electronic).stock(28).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/4caf50/FFFFFF?text=Smartwatch").build(),
                    Product.builder().name("Robot khủng long điều khiển").description("Khủng long robot phun khói")
                        .price(new BigDecimal("899000")).category(electronic).stock(22).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/8bc34a/FFFFFF?text=Dino+Robot").build(),
                    Product.builder().name("Bộ mạch Arduino Starter Kit").description("Kit học lập trình Arduino")
                        .price(new BigDecimal("699000")).category(electronic).stock(25).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=Arduino").build(),
                    Product.builder().name("Robot lắp ráp Makeblock").description("Robot DIY lập trình được")
                        .price(new BigDecimal("1499000")).category(electronic).stock(18).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ff5722/FFFFFF?text=Makeblock").build(),
                    Product.builder().name("Máy chơi game cầm tay retro").description("500 game kinh điển tích hợp")
                        .price(new BigDecimal("499000")).discountPrice(new BigDecimal("399000")).category(electronic).stock(32).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/9c27b0/FFFFFF?text=Retro+Game").build(),
                    Product.builder().name("Robot chó cảm biến").description("Chó robot biết đi, sủa, vẫy đuôi")
                        .price(new BigDecimal("1299000")).category(electronic).stock(16).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/795548/FFFFFF?text=Robot+Dog").build(),
                    Product.builder().name("Bộ mạch Raspberry Pi 4").description("Máy tính nhỏ học lập trình")
                        .price(new BigDecimal("1599000")).category(electronic).stock(12).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/c62828/FFFFFF?text=Raspberry+Pi").build(),
                    Product.builder().name("Robot humanoid Nao mini").description("Robot hình người nhảy múa")
                        .price(new BigDecimal("3999000")).category(electronic).stock(8).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/607d8b/FFFFFF?text=Humanoid").build(),
                    Product.builder().name("Xe robot tank chiến đấu").description("Tank robot điều khiển từ xa")
                        .price(new BigDecimal("849000")).category(electronic).stock(24).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/424242/FFFFFF?text=Robot+Tank").build(),
                    Product.builder().name("Bộ thí nghiệm điện tử 100in1").description("100 mạch điện tử thí nghiệm")
                        .price(new BigDecimal("549000")).discountPrice(new BigDecimal("459000")).category(electronic).stock(28).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/ffc107/FFFFFF?text=100in1").build(),
                    Product.builder().name("Robot bọ cạp điều khiển").description("Bọ cạp robot leo tường")
                        .price(new BigDecimal("399000")).category(electronic).stock(30).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ff5722/FFFFFF?text=Scorpion").build(),
                    Product.builder().name("Bộ máy phát điện năng lượng mặt trời").description("Học về năng lượng xanh")
                        .price(new BigDecimal("459000")).category(electronic).stock(26).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/cddc39/FFFFFF?text=Solar+Power").build(),
                    Product.builder().name("Robot nhện 8 chân").description("Nhện robot leo tường phát sáng")
                        .price(new BigDecimal("649000")).category(electronic).stock(20).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/212121/FFFFFF?text=Spider+Robot").build(),
                    Product.builder().name("Bộ lắp ráp mạch LED").description("Kit LED 50 hiệu ứng ánh sáng")
                        .price(new BigDecimal("289000")).category(electronic).stock(38).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/00e676/FFFFFF?text=LED+Kit").build(),
                    Product.builder().name("Robot biến hình 5in1").description("1 robot biến thành 5 hình")
                        .price(new BigDecimal("999000")).discountPrice(new BigDecimal("799000")).category(electronic).stock(18).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/2196f3/FFFFFF?text=5in1+Robot").build(),
                    Product.builder().name("Máy bay RC điện động cơ").description("Máy bay điều khiển từ xa bay cao")
                        .price(new BigDecimal("1199000")).category(electronic).stock(14).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/1976d2/FFFFFF?text=RC+Plane").build(),
                    Product.builder().name("Robot lắp ghép sáng tạo").description("500 chi tiết lắp tự do")
                        .price(new BigDecimal("749000")).category(electronic).stock(22).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ff9800/FFFFFF?text=Build+Robot").build()
                ));
                
                // BOARD GAMES & PUZZLE (20 products)
                productRepository.saveAll(Arrays.asList(
                    Product.builder().name("Cờ tỷ phú Monopoly Việt Nam").description("Monopoly phiên bản Việt Nam")
                        .price(new BigDecimal("399000")).discountPrice(new BigDecimal("329000")).category(board).stock(40).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/00695c/FFFFFF?text=Monopoly").build(),
                    Product.builder().name("Uno cards phiên bản đặc biệt").description("Bài UNO 108 lá nhiều hiệu ứng")
                        .price(new BigDecimal("129000")).category(board).stock(80).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/d32f2f/FFFFFF?text=UNO").build(),
                    Product.builder().name("Cờ vua nam châm cao cấp").description("Bàn cờ vua gỗ từ tính 32cm")
                        .price(new BigDecimal("299000")).category(board).stock(35).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/795548/FFFFFF?text=Chess").build(),
                    Product.builder().name("Jenga tháp gỗ rút thanh").description("54 thanh gỗ thử thách")
                        .price(new BigDecimal("189000")).category(board).stock(55).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/8d6e63/FFFFFF?text=Jenga").build(),
                    Product.builder().name("Scrabble ghép chữ tiếng Anh").description("Trò chơi ghép từ học Anh văn")
                        .price(new BigDecimal("349000")).category(board).stock(30).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/1976d2/FFFFFF?text=Scrabble").build(),
                    Product.builder().name("Cluedo phá án bí ẩn").description("Trò chơi trinh thám hấp dẫn")
                        .price(new BigDecimal("459000")).discountPrice(new BigDecimal("389000")).category(board).stock(25).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/512da8/FFFFFF?text=Cluedo").build(),
                    Product.builder().name("Cờ cá ngựa 6 người chơi").description("Bàn cờ cá ngựa gia đình")
                        .price(new BigDecimal("149000")).category(board).stock(60).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ff5722/FFFFFF?text=Ludo").build(),
                    Product.builder().name("Domino 100 quân gỗ màu").description("Domino gỗ xếp hình sáng tạo")
                        .price(new BigDecimal("199000")).category(board).stock(48).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/f57c00/FFFFFF?text=Domino").build(),
                    Product.builder().name("Cờ vây Go Baduk chuyên nghiệp").description("Bàn cờ vây 19x19 đá thủy tinh")
                        .price(new BigDecimal("599000")).category(board).stock(20).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/424242/FFFFFF?text=Go").build(),
                    Product.builder().name("Bài Poker cao cấp PVC").description("Bộ bài Poker chống nước")
                        .price(new BigDecimal("259000")).category(board).stock(42).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/d32f2f/FFFFFF?text=Poker").build(),
                    Product.builder().name("Rubik's Cube 4x4 Revenge").description("Rubik 4x4 cao cấp tốc độ")
                        .price(new BigDecimal("199000")).discountPrice(new BigDecimal("169000")).category(board).stock(45).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/ffc107/FFFFFF?text=Rubik+4x4").build(),
                    Product.builder().name("Cờ tướng nam châm di động").description("Cờ tướng gấp gọn 25cm")
                        .price(new BigDecimal("129000")).category(board).stock(65).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/d84315/FFFFFF?text=Chinese+Chess").build(),
                    Product.builder().name("Exploding Kittens bài mèo nổ").description("Trò chơi bài Mỹ vui nhộn")
                        .price(new BigDecimal("279000")).category(board).stock(38).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ff5722/FFFFFF?text=Exploding+Kittens").build(),
                    Product.builder().name("Cờ Caro 5 in a row").description("Bàn cờ Gomoku gỗ 15x15")
                        .price(new BigDecimal("169000")).category(board).stock(50).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/5d4037/FFFFFF?text=Gomoku").build(),
                    Product.builder().name("Mê cung 3D Perplexus").description("Bóng mê cung 3D 100 chướng ngại")
                        .price(new BigDecimal("449000")).category(board).stock(22).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/00bcd4/FFFFFF?text=Perplexus").build(),
                    Product.builder().name("Sequence bài kết hợp cờ").description("Trò chơi bài + cờ 2-12 người")
                        .price(new BigDecimal("389000")).discountPrice(new BigDecimal("329000")).category(board).stock(28).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/4caf50/FFFFFF?text=Sequence").build(),
                    Product.builder().name("Bingo trò chơi lô tô").description("Bộ Bingo 48 thẻ số")
                        .price(new BigDecimal("159000")).category(board).stock(55).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/ffc107/FFFFFF?text=Bingo").build(),
                    Product.builder().name("Rubik Mirror Cube gương").description("Rubik khối gương độc đáo")
                        .price(new BigDecimal("179000")).category(board).stock(40).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/9e9e9e/FFFFFF?text=Mirror+Cube").build(),
                    Product.builder().name("Catan Settlers of Catan").description("Trò chơi chiến lược phát triển")
                        .price(new BigDecimal("699000")).category(board).stock(18).featured(true)
                        .imageUrl("https://via.placeholder.com/300x200/ff9800/FFFFFF?text=Catan").build(),
                    Product.builder().name("Bộ bài Tây 52 lá plastic").description("Bài nhựa cao cấp chống nước")
                        .price(new BigDecimal("99000")).category(board).stock(100).featured(false)
                        .imageUrl("https://via.placeholder.com/300x200/1976d2/FFFFFF?text=Playing+Cards").build()
                ));

                System.out.println("✅ Initialized 8 categories and 160+ products successfully!");
            }
        };
    }
}