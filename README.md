# Bài Tập JWT Với Spring Boot 3 & Spring Security 6 (Thư Viện Nimbus JOSE + JWT)

Dự án này triển khai bài tập xác thực **JWT (JSON Web Token)** theo kiến trúc **Stateless** sử dụng **Spring Boot 3.3.4**, **Spring Security 6**, và thư viện **Nimbus JOSE + JWT** (`com.nimbusds:nimbus-jose-jwt`) thay thế cho thư viện JJWT (`io.jsonwebtoken`) theo đúng bài giảng `04_JWT.pdf`.

---

## 📌 1. Danh Sách Yêu Cầu & Kết Quả Đối Chiếu Nguồn Code (04_JWT.pdf, trang 15–34)

| Trang PDF | Mô Tả Yêu Cầu Trong Slide | Trạng Thái & Lớp Triển Khai Trong Source Code |
| :--- | :--- | :--- |
| **Trang 15** | Thêm Dependency JWT | **Hoàn thành (Nâng cấp)**: Sử dụng `com.nimbusds:nimbus-jose-jwt:9.37.3` trong `pom.xml`, loại bỏ toàn bộ library JJWT (`io.jsonwebtoken`). |
| **Trang 16** | Tạo Entity `User` | **Hoàn thành**: Lớp `vn.iotstar.entity.User` triển khai `UserDetails`, có `@CreationTimestamp`, `@UpdateTimestamp` và ẩn password hash khi serialize JSON bằng `@JsonProperty(access = WRITE_ONLY)`. |
| **Trang 17** | Tạo các DTO Models | **Hoàn thành**: `LoginUserModel`, `RegisterUserModel`, `LoginResponse` trong package `vn.iotstar.models`. |
| **Trang 18** | Tạo `UserRepository` & `UserService` | **Hoàn thành**: `vn.iotstar.repository.UserRepository` (có `findByEmail`) và `vn.iotstar.service.UserService`. |
| **Trang 19** | Tạo `AuthenticationService` | **Hoàn thành**: `vn.iotstar.service.AuthenticationService` thực hiện mã hóa BCrypt khi `signup()` và xác thực tài khoản/mật khẩu khi `authenticate()`. |
| **Trang 20** | Tạo `JwtService` | **Hoàn thành (Nâng cấp Nimbus)**: `vn.iotstar.service.JwtService` dùng `SignedJWT`, `MACSigner`, `MACVerifier` của Nimbus. Kiểm tra chữ ký bí mật, thuật toán **HS256**, thời gian hết hạn (`exp`) và chủ thể (`sub`). |
| **Trang 21** | File `application.properties` | **Hoàn thành**: Tách thông tin kết nối MySQL và JWT Secret Key ra biến môi trường. Tạo file mẫu `application.properties.example`. |
| **Trang 22** | Tạo `ApplicationConfiguration` | **Hoàn thành**: Định nghĩa 4 Beans (`UserDetailsService`, `BCryptPasswordEncoder`, `AuthenticationManager`, `AuthenticationProvider`) trong `vn.iotstar.config.ApplicationConfiguration`. |
| **Trang 23** | Tạo `JwtAuthenticationFilter` | **Hoàn thành**: `vn.iotstar.filter.JwtAuthenticationFilter` kế thừa `OncePerRequestFilter`, trích xuất `Authorization: Bearer <token>` và tích hợp `HandlerExceptionResolver`. |
| **Trang 24–25**| Tạo `SecurityConfiguration` | **Hoàn thành**: `vn.iotstar.config.SecurityConfiguration` cấu hình Stateless, CORS, phân quyền đường dẫn công khai/bảo vệ, bổ sung custom `AuthenticationEntryPoint` (401) và `AccessDeniedHandler` (403). |
| **Trang 26–27**| Tạo RestControllers | **Hoàn thành**: `AuthenticationController` (`/auth/signup`, `/auth/login`) và `UserController` (`/users/me`, `/users`). |
| **Trang 28–29**| Kiểm thử API `/users/me` và `/users` | **Hoàn thành**: Kiểm thử tự động 100% qua integration tests và file Postman collection `JWT_SpringBoot3.postman_collection.json`. |
| **Trang 30–31**| Bắt ngoại lệ & HTTP Status Code | **Hoàn thành**: `GlobalExceptionHandler` bắt ngoại lệ `BadCredentialsException` (401), `AccessDeniedException` (403), `AccountStatusException` (403), Token lỗi/hết hạn (401). |
| **Trang 32–34**| Giao diện AJAX (`login.html`, `profile.html`, `mainjs.js`) | **Hoàn thành**: Views Thymeleaf `login.html`, `profile.html`, Controller `AuthController.java` (`/login`, `/user/profile`), và file static `mainjs.js` xử lý AJAX & `localStorage`. |

---

## 🧪 2. Chi Tiết 13 Tình Huống Kiểm Thử Tự Động (Unit & Integration Tests)

Dự án được xây dựng bộ test tự động gồm **13 test cases** phân chia thành **Unit Tests** và **Integration Tests**:

### A. Bộ Unit Test `JwtServiceTest` (5 tình huống - Chạy trên Java Memory)
1. **`testGenerateAndValidateToken_Success`**: Kiểm tra sinh token Nimbus HS256 từ User, trích xuất chính xác `subject` email và kiểm tra token hợp lệ.
2. **`testValidateToken_InvalidUser_ReturnsFalse`**: Kiểm tra từ chối token khi thông tin người dùng truyền vào không khớp với `subject` trong token.
3. **`testValidateToken_TamperedToken_ReturnsFalse`**: Kiểm tra từ chối token khi chữ ký (signature) bị thay đổi hoặc giả mạo.
4. **`testValidateToken_ExpiredToken_ReturnsFalse`**: Kiểm tra từ chối token khi thời gian hết hạn (`exp`) đã qua trong quá khứ.
5. **`testExtractUsername_InvalidToken_ReturnsNull`**: Kiểm tra xử lý an toàn trả về `null` khi chuỗi token bị lỗi cú pháp.

### B. Bộ Integration Test `AuthenticationAndUserControllerTest` (8 tình huống - Dùng MockMvc + H2 Database chế độ tương thích MySQL)
6. **`testSignup_Success_PasswordNotReturnedAndEncryptedInDB`**: Kiểm tra đăng ký người dùng mới. Xác nhận mật khẩu lưu trong DB được **BCrypt mã hóa**, đồng thời API response KHÔNG chứa hash mật khẩu.
7. **`testLogin_Success_ReturnsJwtToken`**: Đăng nhập đúng email & password, trả về HTTP 200 OK chứa `token` và `expiresIn` = 3600000ms.
8. **`testLogin_InvalidCredentials_Returns401`**: Đăng nhập sai mật khẩu trả về HTTP 401 Unauthorized kèm thông báo lỗi JSON rõ ràng.
9. **`testGetMe_NoToken_Returns401`**: Truy cập API `/users/me` khi không gửi kèm Bearer Token trả về HTTP 401 Unauthorized.
10. **`testGetMe_ValidToken_ReturnsUserProfile`**: Truy cập `/users/me` với Bearer Token hợp lệ trả về HTTP 200 OK và thông tin profile (không có password).
11. **`testGetMe_TamperedToken_Returns401`**: Truy cập `/users/me` với Token bị sai chữ ký trả về HTTP 401 Unauthorized.
12. **`testGetAllUsers_ValidToken_ReturnsUsersList`**: Truy cập `/users` với Bearer Token hợp lệ trả về danh sách người dùng.
13. **`testGetAllUsers_NoToken_Returns401`**: Truy cập `/users` khi thiếu Token trả về HTTP 401 Unauthorized.

---

## 🔒 3. An Toàn Bảo Mật & Đóng Gói (Build Verification)

1. **Loại bỏ JJWT hoàn toàn**: Đã quét toàn bộ dự án (`Get-ChildItem -Recurse | Select-String`), xác nhận không còn bất kỳ dependency hay import nào từ `io.jsonwebtoken`.
2. **Bảo vệ Mật Khẩu**: Đã cấu hình `@JsonProperty(access = WRITE_ONLY)` ở Entity `User`. Không có API nào trả về mật khẩu hay hash mật khẩu.
3. **Quản lý Cấu hình & Mật khẩu**: File `.gitignore` đã chặn `target/`, `.idea/`, `*.log`, `scratch/`. Mật khẩu DB thật và JWT Secret Key không bị hardcode trong Git.
4. **Kết quả Build Thực tế (`mvn clean package`)**:
   * **Test execution**: 13/13 tests PASSED (0 Failures, 0 Errors).
   * **Artifact**: Đã đóng gói thành công file thi hành `target/JWT_springboot3-0.0.1-SNAPSHOT.jar` (**BUILD SUCCESS**).

---

## 🚀 4. Hướng Dẫn Chi Tiết Để Mở Và Chạy Ứng Dụng

### Bước 1: Chuẩn bị Cơ sở dữ liệu MySQL
1. Khởi động MySQL Server (Port 3306).
2. Tạo Database `jwt_springboot3`:
   ```sql
   CREATE DATABASE IF NOT EXISTS jwt_springboot3 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

### Bước 2: Cấu hình biến môi trường hoặc file `application.properties`
Mở file `src/main/resources/application.properties` hoặc thiết lập các biến môi trường trước khi chạy:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/jwt_springboot3?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD

security.jwt.secret-key=3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b
security.jwt.expiration-time=3600000
```

### Bước 3: Khởi động Ứng dụng
Mở Terminal tại thư mục gốc `d:\JWT` và gõ lệnh:
```bash
mvn spring-boot:run
```
Ứng dụng sẽ khởi động tại cổng **`8005`**.

### Bước 4: Mở Trình Duyệt Kiểm Trực Tiếp (Web AJAX UI)
1. **Đăng nhập:** Mở trình duyệt truy cập: [http://localhost:8005/login](http://localhost:8005/login)
2. **Đăng ký tài khoản thử nghiệm (qua Postman hoặc cURL):**
   ```bash
   curl -X POST http://localhost:8005/auth/signup \
     -H "Content-Type: application/json" \
     -d "{\"email\":\"student@example.com\",\"password\":\"Password123!\",\"fullName\":\"Nguyen Van A\",\"images\":\"avatar1.jpg\"}"
   ```
3. **Đăng nhập trên giao diện:** Nhập email `student@example.com` và password `Password123!`, bấm nút **Login**.
4. **Xem Profile:** Đăng nhập thành công, hệ thống tự động lưu Token vào `localStorage` và chuyển sang trang [http://localhost:8005/user/profile](http://localhost:8005/user/profile), gọi API `/users/me` bằng Bearer Token và hiển thị thông tin người dùng.
5. **Đăng xuất:** Bấm nút **Logout** để xóa Token và quay về lại trang `/login`.

---

## 📁 5. File Kiểm Thử Postman Collection
Dự án đính kèm file **`JWT_SpringBoot3.postman_collection.json`** tại thư mục gốc. Bạn có thể import trực tiếp vào Postman để test đủ 4 API (`/auth/signup`, `/auth/login`, `/users/me`, `/users`).
