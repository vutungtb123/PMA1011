# THÔNG TIN DỰ ÁN: SCHOOL LIBRARY MANAGEMENT SYSTEM
- Tech Stack: Java, Spring Boot, Spring Data JPA, Spring Security, SQL Server, Gradle.
- Architecture: MVC Pattern (Model - View - Controller), Layered Architecture.
- Template Engine: Giao diện render trực tiếp bằng HTML (Thymeleaf/JSP) tại Server, KHÔNG phải là RESTful API thuần trả về JSON cho Frontend riêng biệt.

# CẤU TRÚC THƯ MỤC VÀ NHIỆM VỤ CỦA TỪNG PACKAGE (QUY TẮC BẮT BUỘC)

## 1. /config (Cấu hình hệ thống)
- Chứa các class đánh dấu `@Configuration`.
- Nơi thiết lập Spring Security (`SecurityConfig`): Cấu hình Form Login, phân quyền URL (`/admin/**`, `/user/**`), cấu hình mã hóa mật khẩu (`BCryptPasswordEncoder`).

## 2. /security (Bảo mật & Xác thực)
- Chứa logic Authentication của Spring Security.
- Bao gồm `CustomUserDetails` (implement `UserDetails`) và `CustomUserDetailsService` (implement `UserDetailsService`) để query thông tin từ bảng `Readers` trong SQL Server.

## 3. /controller (Xử lý Request & Trả về Giao diện)
- Chứa các class đánh dấu `@Controller` (KHÔNG dùng `@RestController` trừ khi viết API nội bộ gọi bằng AJAX).
- Nhiệm vụ: Tiếp nhận HTTP Request, gọi tầng `service` để lấy dữ liệu, đưa dữ liệu vào Model và `return` về tên file giao diện HTML trong thư mục `templates`.
- Tuyệt đối KHÔNG viết logic nghiệp vụ (if/else tính toán phức tạp) hay gọi thẳng `repository` tại đây.

## 4. /service & /service/impl (Tầng Logic Nghiệp vụ)
- `/service`: Chứa các Interface định nghĩa các hành động (VD: `BorrowService`).
- `/service/impl`: Chứa class implement các Interface trên, đánh dấu `@Service`.
- Nhiệm vụ: Xử lý toàn bộ logic tính toán (tính tiền phạt, check hàng đợi, check giới hạn mượn sách). Đây là nơi duy nhất được quyền gọi `repository`.

## 5. /repository (Tầng Tương tác Dữ liệu)
- Chứa các Interface extends `JpaRepository`.
- Nhiệm vụ: Thực hiện các câu lệnh truy vấn tới SQL Server. Ưu tiên dùng Query Method tự sinh của Spring Data JPA hoặc `@Query` cho các câu phức tạp.

## 6. /entity (Bản đồ Cơ sở dữ liệu)
- Chứa các class đánh dấu `@Entity`, `@Table`.
- Mỗi class map trực tiếp 1-1 với 1 bảng trong SQL Server.
- Khai báo rõ ràng các quan hệ `@OneToMany`, `@ManyToOne`...

## 7. /dto (Data Transfer Object)
- Chứa các class Record hoặc POJO dùng để luân chuyển dữ liệu.
- Quy tắc: Controller nhận DTO từ HTML Form -> Chuyển xuống Service -> Service map DTO thành Entity để lưu DB. Khi đọc dữ liệu: Service lấy Entity từ DB -> Map sang DTO -> Trả lên Controller để hiển thị ra HTML. (Tuyệt đối không đẩy trực tiếp Entity chứa thông tin nhạy cảm ra giao diện).

## 8. /exception (Xử lý Lỗi Tập trung)
- Chứa class đánh dấu `@ControllerAdvice`.
- Bắt các Exception ném ra từ tầng Service (VD: `BookNotFoundException`) và điều hướng người dùng về trang `error.html` kèm thông báo thân thiện.

## 9. /resources/templates & /resources/static (Giao diện)
- `/templates`: Chứa các file `.html`. Phân rã thư mục rõ ràng: `/admin`, `/user`, `login.html`.
- `/static`: Chứa các tài nguyên tĩnh: `.css`, `.js`, hình ảnh.

---

# QUY TẮC NGHIỆP VỤ CỐT LÕI (DOMAIN RULES - AI CẦN ĐỌC KỸ TRƯỚC KHI CODE)

1. **Tài khoản Độc giả (Reader):** KHÔNG có chức năng đăng ký (Register). Admin tự tạo tài khoản. Độc giả chỉ có thể "Quên mật khẩu" thông qua xác thực mã OTP gửi về Email.
2. **Kệ sách (Shelf):** Hệ thống KHÔNG quản lý vị trí kệ sách vật lý.
3. **Quy trình Trả sách & Phạt nợ:** Được chia làm 2 phase độc lập:
    - Thu hồi vật lý: Cập nhật tình trạng sách, sách hỏng/mất sẽ đổi trạng thái và không cho mượn tiếp.
    - Thu hồi tài chính: Nếu có vi phạm, sinh ra bản ghi nợ trong bảng `Debts`. Độc giả có thể trả nợ nhiều lần (trả góp), lịch sử trả lưu vào `DebtPaymentHistory`.
4. **Hàng đợi (Book Queue):** Không có tính năng "Đặt trước" đối với sách đang còn sẵn. Nếu số lượng `Sẵn sàng` = 0, nút mượn sẽ biến thành "Vào hàng đợi". Khi có người trả sách, hệ thống giữ sách cho người đứng đầu Queue trong vòng 24 giờ.