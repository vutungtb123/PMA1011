package com.example.school_library_system.controller;

import com.example.school_library_system.dto.ForgotPasswordDto;
import com.example.school_library_system.dto.OtpVerifyDto;
import com.example.school_library_system.dto.ResetPasswordDto;
import com.example.school_library_system.entity.User;
import com.example.school_library_system.repository.UserRepository;
import com.example.school_library_system.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    // =========================================================
    // LOGIN
    // =========================================================

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        // Lấy 1 admin và 2 reader để hiển thị quick-fill trên trang login
        List<User> admins = userRepository.findByRole("ROLE_ADMIN", PageRequest.of(0, 1));
        List<User> readers = userRepository.findByRole("ROLE_USER", PageRequest.of(0, 2));

        List<User> demoAccounts = new ArrayList<>();
        demoAccounts.addAll(admins);
        demoAccounts.addAll(readers);

        model.addAttribute("demoAccounts", demoAccounts);
        return "login";
    }


    // =========================================================
    // FORGOT PASSWORD - BƯỚC 1: Nhập Email
    // =========================================================

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        model.addAttribute("forgotPasswordDto", new ForgotPasswordDto());
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @ModelAttribute ForgotPasswordDto forgotPasswordDto,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String email = forgotPasswordDto.getEmail();
        if (email == null || email.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập email.");
            return "redirect:/forgot-password";
        }

        try {
            authService.sendOtp(email);
            // Lưu email vào session để dùng ở các bước tiếp theo
            session.setAttribute("resetEmail", email);
            redirectAttributes.addFlashAttribute("successMessage",
                "Mã OTP đã được gửi về email " + email + ". Vui lòng kiểm tra hộp thư.");
            return "redirect:/verify-otp";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/forgot-password";
        }
    }

    // =========================================================
    // FORGOT PASSWORD - BƯỚC 2: Nhập OTP
    // =========================================================

    @GetMapping("/verify-otp")
    public String showVerifyOtpPage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("email", email);
        model.addAttribute("otpVerifyDto", new OtpVerifyDto());
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String handleVerifyOtp(
            @ModelAttribute OtpVerifyDto otpVerifyDto,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            return "redirect:/forgot-password";
        }

        boolean isValid = authService.verifyOtp(email, otpVerifyDto.getOtpCode());
        if (!isValid) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Mã OTP không đúng hoặc đã hết hạn. Vui lòng thử lại.");
            return "redirect:/verify-otp";
        }

        // Đánh dấu OTP đã xác thực thành công
        session.setAttribute("otpVerified", true);
        return "redirect:/reset-password";
    }

    // =========================================================
    // FORGOT PASSWORD - BƯỚC 3: Đặt lại mật khẩu
    // =========================================================

    @GetMapping("/reset-password")
    public String showResetPasswordPage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmail");
        Boolean otpVerified = (Boolean) session.getAttribute("otpVerified");

        if (email == null || !Boolean.TRUE.equals(otpVerified)) {
            return "redirect:/forgot-password";
        }

        model.addAttribute("email", email);
        model.addAttribute("resetPasswordDto", new ResetPasswordDto());
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(
            @ModelAttribute ResetPasswordDto resetPasswordDto,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String email = (String) session.getAttribute("resetEmail");
        Boolean otpVerified = (Boolean) session.getAttribute("otpVerified");

        if (email == null || !Boolean.TRUE.equals(otpVerified)) {
            return "redirect:/forgot-password";
        }

        try {
            authService.resetPassword(email, resetPasswordDto.getNewPassword(),
                    resetPasswordDto.getConfirmPassword());

            // Xóa session sau khi hoàn tất
            session.removeAttribute("resetEmail");
            session.removeAttribute("otpVerified");

            redirectAttributes.addFlashAttribute("successMessage",
                "Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.");
            return "redirect:/login";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reset-password";
        }
    }
}
