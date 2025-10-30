#  ĐỒ ÁN MÔN HỌC LẬP TRÌNH WEB - Website Bán Đồ Chơi T4M

> Đồ án môn học: Lập trình Web - Học kỳ 1
> Đơn vị đào tạo: Trường Đại Học Sư Phạm Kỹ Thuật TP.HCM
> Giảng viên hướng dẫn: Nguyễn Hữu Trung
> Nhóm sinh viên thực hiện: Nhóm 9 - T4M

---

##  Giới thiệu Chung về Dự án

**T4M** là website bán hàng được phát triển bằng công nghệ Spring Boot kết hợp Thymeleaf và Bootstrap 5. Dự án là một ứng dụng e-commerce hoàn chỉnh hỗ trợ ba vai trò chính: khách hàng (customer), quản trị viên (admin), và nhân viên giao hàng (shipper). Dự án không chỉ cung cấp các tính năng bán hàng cơ bản mà còn tập trung vào việc tối ưu hóa trải nghiệm người dùng (UX) thông qua các nghiệp vụ giao dịch tiên tiến, hệ thống quản trị chuyên sâu, và tích hợp công nghệ hiện đại như AI Chatbot, thanh toán điện tử, và giao tiếp thời gian thực.

###  Mục Tiêu Học Tập & Đóng Góp Kỹ Thuật

| Mục tiêu | Mô tả chi tiết | Kỹ thuật áp dụng |
| :--- | :--- | :--- |
| **Full-Stack Mastery** | Xây dựng hệ thống hoàn chỉnh từ Backend (API, Logic) đến Frontend (UI, UX) theo mô hình MVC. | Spring Boot, Thymeleaf, Spring Data JPA. |
| **Tối ưu Hóa Giao Dịch** | Đảm bảo tính nhất quán và thời gian thực khi thực hiện các giao dịch quan trọng. | AJAX (cho Voucher & Đăng ký tin), JWT (Authentication). |
| **Giao tiếp Thời gian thực** | Thiết lập kênh giao tiếp hai chiều giữa Client và Admin. | WebSocket (cho tính năng Chat). |
| **Thiết kế Chuyên nghiệp** | Xây dựng giao diện hiện đại, đồng bộ thương hiệu và thân thiện với mọi thiết bị. | Bootstrap 5, Responsive Design, Email HTML Templating. |
| **Quản lý Nghiệp vụ Sâu** | Triển khai các module quản trị phức tạp, hỗ trợ ra quyết định kinh doanh. | Module Quản lý Đánh giá, Quản lý Voucher. |

---

##  Thành viên nhóm

| Họ và Tên | Mã Sinh Viên | Liên hệ (Email) |
| :--- | :--- | :--- |
| Nguyễn Thanh Nhật | 23162072 | thanhnhatcyber@gmail.com |
| Ngô Tuấn Phát | 23162075 | [Email Thành viên 2] |
| Trương Xuân Nhật | 23162073 | [Email Thành viên 3] |
| Lê Văn Ánh Thông | 23162096 | [Email Thành viên 4] |

---

##  Tính năng Chi Tiết (Feature Breakdown)

### A. Trải nghiệm Người dùng (Client-Side)

1. **Mua sắm & Thanh toán:**
   - Trang chủ hiển thị các sản phẩm nổi bật, danh mục sản phẩm, và các banner quảng cáo hấp dẫn.
   - Tìm kiếm sản phẩm theo nhiều tiêu chí: tên sản phẩm, loại, giá cả, với các bộ lọc nâng cao.
   - Trang chi tiết sản phẩm với hình ảnh đa dạng được lưu trữ trên Cloudinary, mô tả chi tiết, giá, kích cỡ, và đánh giá từ người dùng.
   - Giỏ hàng cho phép quản lý sản phẩm đã chọn, cập nhật số lượng, xóa sản phẩm, và tính tổng tiền.
   - Quy trình thanh toán gồm 3 bước: nhập thông tin giao hàng, chọn phương thức thanh toán, xác nhận đơn hàng.
   - Tích hợp cổng thanh toán VNPay cho phép thanh toán trực tuyến an toàn.

2. **Khuyến mãi & Tương tác:**
   - Hệ thống mã giảm giá (Voucher): nhập mã voucher với phản hồi nhanh, hỗ trợ nhiều loại giảm giá.
   - Đánh giá sản phẩm: xếp hạng (1-5 sao).
   - Giao tiếp trực tuyến: chat trực tiếp với admin thông qua WebSocket để nhận hỗ trợ nhanh chóng.
   - Đăng ký nhận tin: form tại footer sử dụng để đăng ký nhận tin khuyến mại.
   - Yêu thích sản phẩm: lưu các sản phẩm yêu thích để mua sau.

3. **Tài khoản và Hồ sơ:**
   - Đăng ký và đăng nhập với xác thực OTP qua email.
   - Đặt lại mật khẩu thông qua email.
   - Quản lý hồ sơ cá nhân, xem lịch sử đơn hàng, quản lý địa chỉ giao hàng.

### B. Khu vực Quản Trị (Admin Dashboard)

Giao diện Admin được thiết kế theo phong cách hiện đại, trực quan và đồng bộ về UI/UX với các thành phần của Bootstrap 5, sử dụng sidebar để điều hướng dễ dàng.

1. **Quản lý Hệ thống (CRUD):**
   - Quản lý sản phẩm: thêm, sửa, xóa sản phẩm, upload hình ảnh lên Cloudinary, quản lý danh mục và thương hiệu.
   - Quản lý đơn hàng: xem danh sách, cập nhật trạng thái (chờ xử lý, đang giao, đã giao, bị hủy,...).
   - Quản lý tài khoản người dùng: xem thông tin khách hàng, khóa/mở tài khoản, xem lịch sử đơn hàng.

2. **Module Quản Lý Đánh Giá (Review Management):**
   - Bảng danh sách chi tiết các đánh giá từ khách hàng.
   - Bộ lọc chuyên sâu: lọc theo khoảng số sao, trạng thái, và thời gian.
   - Xóa đánh giá yêu cầu xác nhận bằng modal cảnh báo.
   - Thống kê đánh giá để phân tích chất lượng sản phẩm.

3. **Module Quản Lý Mã Giảm Giá (Voucher Management):**
   - Loại hình giảm giá: giảm theo phần trăm, giảm số tiền cố định, miễn phí vận chuyển.
   - Điều kiện: giá trị đơn hàng tối thiểu, giới hạn số lần sử dụng, ngày bắt đầu và kết thúc.
   - Tạo và quản lý các chiến dịch khuyến mại.

4. **Hỗ trợ và Chat:**
   - Giao diện chat với khách hàng thời gian thực.
   - Quản lý các tin nhắn và hỗ trợ khách hàng hiệu quả.

### C. Khu vực Shipper (Người Giao Hàng)

Shipper là vai trò chuyên giao đơn hàng từ admin đến khách hàng, có giao diện riêng để quản lý công việc hàng ngày.

1. **Quản lý Đơn hàng Giao:**
   - Xem danh sách các đơn hàng có thể nhận, với thông tin chi tiết về địa chỉ giao hàng, sản phẩm, tổng tiền.
   - Cập nhật trạng thái giao hàng: đang giao, đã giao thành công, giao thất bại.
   - Xem chi tiết đơn hàng trước khi giao.
   - Xem được vị trí giao hàng trên google maps

2. **Báo cáo và Thống kê:**
   - Xem số lượng đơn hàng đã giao trong ngày/tháng.
   - Thống kê hiệu suất công việc.

### D. Hệ Thống Bảo mật & Giao tiếp (Core Systems)

- **Bảo mật:** Spring Security kết hợp JWT cho cả Client, Admin, và Shipper. Xác thực dựa trên token.
- **Mã hóa mật khẩu:** Sử dụng BCrypt để mã hóa mật khẩu an toàn.
- **Email Chuyên nghiệp:** Tất cả email quan trọng (OTP, đặt lại mật khẩu, xác nhận) sử dụng HTML template.
- **Chatbot AI:** Tích hợp Google Gemini AI để trả lời câu hỏi thường gặp.
- **Upload Ảnh:** Sử dụng Cloudinary để lưu trữ và tối ưu hóa ảnh.

---

## Công nghệ sử dụng & Cấu hình

| Thành phần | Công nghệ | Phiên bản | Mô tả chi tiết |
| :--- | :--- | :--- | :--- |
| **Ngôn ngữ** | Java | 24 | Backend chính |
| **Framework** | Spring Boot | 3.5.5 | Kiến trúc MVC |
| **Template** | Thymeleaf | 3.1.2 | Kết xuất trang web |
| **UI** | Bootstrap 5 | 5.x | Responsive design |
| **Database** | MySQL | 8.0+ | Lưu trữ dữ liệu |
| **ORM** | Spring Data JPA | 3.5.5 | Repository pattern |
| **Bảo mật** | Spring Security | 6.x | Xác thực, phân quyền |
| **JWT** | JJWT | 0.9.1 | Token authentication |
| **Upload** | Cloudinary | 1.39.0 | Lưu trữ ảnh |
| **WebSocket** | Spring Messaging | 3.5.5 | Chat thời gian thực |
| **Thanh toán** | VNPay | Sandbox | Cổng thanh toán |
| **AI** | Google Gemini | Latest | Chatbot |
| **Email** | Spring Mail | 3.5.5 | Gửi email |
| **Build** | Maven | 3.6+ | Quản lý dự án |

---

## Cấu trúc Thư mục

```bash
Toy-Store-Web/
├── src/
│   ├── main/
│   │   ├── java/t4m/toy_store/
│   │   │   ├── ToyStoreApplication.java
│   │   │   ├── admin/                    # Module admin
│   │   │   ├── auth/                     # Module xác thực
│   │   │   ├── cart/                     # Module giỏ hàng
│   │   │   ├── chatbot/                  # Module AI chatbot
│   │   │   ├── config/                   # Cấu hình Spring
│   │   │   ├── favorite/                 # Module yêu thích
│   │   │   ├── image/                    # Module upload ảnh
│   │   │   ├── main/                     # Module trang chủ
│   │   │   ├── order/                    # Module đơn hàng
│   │   │   ├── payment/                  # Module thanh toán
│   │   │   ├── product/                  # Module sản phẩm
│   │   │   ├── rating/                   # Module đánh giá
│   │   │   ├── shipper/                  # Module shipper
│   │   │   ├── support/                  # Module chat hỗ trợ
│   │   │   └── voucher/                  # Module mã giảm giá
│   │   ├── resources/
│   │   │   ├── templates/
│   │   │   │   ├── client files
│   │   │   │   ├── admin/
│   │   │   │   ├── shipper/
│   │   │   │   ├── email/
│   │   │   │   ├── fragments/
│   │   │   │   └── policies/
│   │   │   ├── static/
│   │   │   │   ├── css/
│   │   │   │   └── js/
│   │   │   └── application.properties
│   └── test/
├── pom.xml
├── mvnw / mvnw.cmd
└── README.md
```

---

## Cài đặt và Chạy Dự án

### Yêu cầu Hệ thống

- Java Development Kit (JDK) 24
- Apache Maven 3.6+
- MySQL Server 8.0+
- Git
- IDE: IntelliJ IDEA, Eclipse hoặc VS Code

### Hướng Dẫn Cài Đặt

**Bước 1: Clone Repository**

```bash
git clone https://github.com/useristn/Toy-Store-Web.git
cd Toy-Store-Web
```

**Bước 2: Cấu hình Database**

```sql
CREATE DATABASE toy_store;
CREATE USER 'toyuser'@'localhost' IDENTIFIED BY 'toypass123';
GRANT ALL PRIVILEGES ON toy_store.* TO 'toyuser'@'localhost';
FLUSH PRIVILEGES;
```

**Bước 3: Cấu hình Application Properties**

Chỉnh sửa `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/toy_store
spring.datasource.username=toyuser
spring.datasource.password=toypass123

spring.jpa.hibernate.ddl-auto=update

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password

cloudinary.cloud-name=your_cloud_name
cloudinary.api-key=your_api_key
cloudinary.api-secret=your_api_secret

vnpay.tmn-code=YOUR_TMN_CODE
vnpay.hash-secret=YOUR_HASH_SECRET

gemini.api.key=your_gemini_api_key
```

**Bước 4: Chạy Ứng dụng**

```bash
mvn clean install
mvn spring-boot:run
```

Hoặc chạy từ IDE: Run ToyStoreApplication.java

**Bước 5: Truy cập Ứng dụng**

- Client: http://localhost:8080
- Admin: http://localhost:8080/admin
- Shipper: http://localhost:8080/shipper

### Tài khoản Demo

- Admin: `admin` / `admin123`
- Shipper: `shipper` / `shipper123`
- Customer: Đăng ký tài khoản mới

---

## Tính năng Chính

- Mua sắm sản phẩm với tìm kiếm, lọc, đánh giá
- Giỏ hàng và thanh toán VNPay
- Mã giảm giá động
- Chat thời gian thực với AI Chatbot
- Quản lý admin đầy đủ
- Quản lý shipper giao hàng
- Email xác thực OTP
- Upload ảnh Cloudinary
- Bảo mật JWT

---

## Vấn Đề Thường Gặp & Giải Pháp

**Lỗi kết nối MySQL:**
- Kiểm tra MySQL Service chạy
- Kiểm tra username, password
- Kiểm tra database `toy_store` được tạo

**Lỗi WebSocket:**
- Kiểm tra firewall cho phép port 8080
- Kiểm tra trình duyệt hỗ trợ WebSocket

**Lỗi upload ảnh:**
- Kiểm tra Cloudinary credentials
- Kiểm tra kết nối internet

**Lỗi email:**
- Sử dụng Gmail App Password
- Kiểm tra SMTP settings

---

## Liên hệ

- Giảng viên: Nguyễn Hữu Trung
- Nhóm T4M: [Danh sách thành viên ở trên]
- Repository: https://github.com/useristn/Toy-Store-Web

---

*Đồ án Lập trình Web - Trường Đại Học Sư Phạm Kỹ Thuật TP.HCM*
