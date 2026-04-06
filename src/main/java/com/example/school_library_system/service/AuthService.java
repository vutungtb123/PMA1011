package com.example.school_library_system.service;

public interface AuthService {

    /**
     * Tạo OTP 6 số, lưu vào DB, gửi email cho Reader.
     * @throws RuntimeException nếu email không tồn tại
     */
    void sendOtp(String email);

    /**
     * Kiểm tra OTP người dùng nhập có khớp và chưa hết hạn không.
     * @return true nếu hợp lệ
     */
    boolean verifyOtp(String email, String otpCode);

    /**
     * Đặt lại mật khẩu mới (BCrypt encode) và xóa OTP.
     * @throws RuntimeException nếu 2 mật khẩu không khớp hoặc email không tồn tại
     */
    void resetPassword(String email, String newPassword, String confirmPassword);
}
