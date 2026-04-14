# School Library System

Dự án Hệ thống Quản lý Thư viện Trường học (School Library System) là một ứng dụng web giúp tin học hóa quy trình quản lý sách, độc giả, và hoạt động mượn/trả sách trong thư viện. 

## 💻 Ngôn ngữ & Công nghệ sử dụng

- **Ngôn ngữ:** Java 17
- **Framework backend:** Spring Boot 3.5.x
- **Bảo mật:** Spring Security (Hỗ trợ xác thực, phân quyền Admin/User, OTP)
- **Truy cập cơ sở dữ liệu:** Spring Data JPA / Hibernate
- **Cơ sở dữ liệu:** Microsoft SQL Server
- **View Engine:** Thymeleaf (+ HTML/CSS/JavaScript cơ bản)
- **Quản lý dependencies & build:** Gradle
- **Tiện ích:** Lombok, JavaMailSender (Gửi email OTP)

## 📁 Cấu trúc thư mục (Directory Structure)

Cấu trúc thư mục tuân theo mô hình MVC (Model-View-Controller) tiêu chuẩn của Spring Boot:

```text
school-library-system/
├── src/main/java/com/example/school_library_system/
│   ├── config/      # Các lớp cấu hình cho hệ thống (Security, MVC...)
│   ├── controller/  # Tiếp nhận các HTTP Request từ client và trả về View tương ứng
│   ├── dto/         # Data Transfer Objects, các đối tượng dùng để truyền tải dữ liệu
│   ├── entity/      # Các thực thể ánh xạ tới các bảng trong cơ sở dữ liệu
│   ├── exception/   # Quản lý và xử lý các ngoại lệ (Exception) toàn cục
│   ├── repository/  # Interface giao tiếp với cơ sở dữ liệu (Spring Data JPA)
│   ├── security/    # Các thành phần cốt lõi xử lý xác thực (Authentication)
│   └── service/     # Lớp nghiệp vụ (Business logic), xử lý các quy tắc của hệ thống
├── src/main/resources/
│   ├── static/      # Chứa tài nguyên tĩnh như CSS, JS, Images
│   ├── templates/   # Chứa các file giao diện Thymeleaf (.html)
│   └── application.properties # File thiết lập thông số kết nối CSDL, mail, v.v.
└── build.gradle     # File cấu hình thư viện và build project của Gradle
```

## 🛠 Chức năng chính của hệ thống

Ứng dụng được chia ra làm 2 luồng chức năng chính tương ứng với quyền của người tương tác là **Người dùng (Độc giả - Học sinh/Giáo viên)** và **Quản trị viên (Thủ thư/Admin)**.

### 1. Dành cho Người dùng (Độc giả)
- **Xác thực:** Đăng nhập, Đăng ký tài khoản, Quên mật khẩu (khôi phục qua mã OTP gửi về Email).
- **Tra cứu sách:** Xem danh sách các đầu sách, tra cứu tìm kiếm và phân loại sách theo danh mục.
- **Xem chi tiết sách:** Đọc thông tin mô tả chi tiết, tình trạng khả dụng của các bản sao sách trong kho.
- **Quản lý mượn/trả cá nhân (Profile):** Xem danh sách các cuốn sách đang mượn (Active Borrows), và lịch sử đã mượn/trả sách trước đây.
- **Danh sách chờ sách (Waitlist):** Đăng ký mượn đối với các đầu sách đã hết, nhận thông báo khi có sách trả lại.

### 2. Dành cho Quản trị viên (Admin / Thủ thư)
- **Bảng điều khiển (Dashboard):** Xem các số liệu thống kê tổng quát (Tổng sách, Số lượt mượn, Sách quá hạn...).
- **Quản lý Kho (Warehouse):** Nhập sách vào kho thông qua các Biên bản nhập/Phiếu nhập kho (Warehouse receipts). Hệ thống có chứng năng xác thực để đối soát số lượng sách thật/giả tránh dư thừa (ghost inventory).
- **Quản lý Sách và Danh mục:** Thêm, sửa, xóa các đầu sách và danh mục sách. Phân bổ và quản lý các cuốn sách vật lý (Book copies) trong hệ thống.
- **Quản lý Mượn/Trả:** Xử lý thủ tục cho mượn và nhận trả sách. Đặc biệt, hỗ trợ hệ thống **Quét mã QR (QR-Based Library Checkout)** trên thẻ sách/người dùng để thực hiện mượn trả nhanh chóng.
- **Quản lý Độc giả (User):** Phân quyền, khóa/mở khóa tài khoản người dùng, thiết lập các Nhóm người đọc (Reader Groups).
