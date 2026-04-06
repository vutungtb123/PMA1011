package com.example.school_library_system.service.impl;

import com.example.school_library_system.entity.User;
import com.example.school_library_system.repository.UserRepository;
import com.example.school_library_system.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Override
    @Transactional
    public void sendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống."));

        // Sinh OTP 6 chữ số
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtpCode(otp);
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        userRepository.save(user);

        // Gửi email
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("[Thư Viện Trường] Mã OTP Đặt Lại Mật Khẩu");
            message.setText(
                "Xin chào " + user.getFullName() + ",\n\n" +
                "Mã OTP đặt lại mật khẩu của bạn là: " + otp + "\n\n" +
                "Mã có hiệu lực trong " + otpExpiryMinutes + " phút.\n" +
                "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.\n\n" +
                "Thư Viện Trường"
            );
            mailSender.send(message);
            log.info("Đã gửi OTP đến email: {}", email);
        } catch (Exception e) {
            log.error("Lỗi gửi email OTP đến {}: {}", email, e.getMessage());
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }

    @Override
    public boolean verifyOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống."));

        if (user.getOtpCode() == null || user.getOtpExpiredAt() == null) {
            return false;
        }

        boolean isExpired = LocalDateTime.now().isAfter(user.getOtpExpiredAt());
        if (isExpired) {
            return false;
        }

        return user.getOtpCode().equals(otpCode.trim());
    }

    @Override
    @Transactional
    public void resetPassword(String email, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp.");
        }
        if (newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Xóa OTP sau khi đặt lại mật khẩu thành công
        user.setOtpCode(null);
        user.setOtpExpiredAt(null);
        userRepository.save(user);

        log.info("Đã đặt lại mật khẩu thành công cho: {}", email);
    }
}
