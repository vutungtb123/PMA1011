package com.example.school_library_system.controller;

import com.example.school_library_system.entity.ReaderGroup;
import com.example.school_library_system.entity.User;
import com.example.school_library_system.repository.ReaderGroupRepository;
import com.example.school_library_system.repository.UserRepository;
import com.example.school_library_system.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReaderGroupRepository readerGroupRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageService fileStorageService;

    private static final String DEFAULT_PASSWORD = "123456";

    // ============================================================
    // LIST - Danh sách thẻ thư viện
    // ============================================================
    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "") String search,
            Model model) {
        List<User> users;
        if (search == null || search.isBlank()) {
            users = userRepository.findByRoleOrderByCreatedAtDesc("ROLE_USER");
        } else {
            users = userRepository.findByRoleAndSearchKeyword("ROLE_USER", search.trim());
        }
        model.addAttribute("users", users);
        model.addAttribute("search", search);
        model.addAttribute("totalCount", users.size());
        return "admin/users/list";
    }

    // ============================================================
    // CREATE FORM - Trang tạo thẻ
    // ============================================================
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        List<ReaderGroup> groups = readerGroupRepository.findAll();
        model.addAttribute("groups", groups);
        // Auto-generate student ID gợi ý
        long count = userRepository.countByRole("ROLE_USER");
        String suggestedId = String.format("HS%04d", count + 1);
        model.addAttribute("suggestedStudentId", suggestedId);
        return "admin/users/create";
    }

    // ============================================================
    // SAVE - Lưu thẻ + tạo tài khoản
    // ============================================================
    @PostMapping("/create")
    public String createUser(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String studentId,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            RedirectAttributes redirectAttributes) {

        // Validate email
        if (userRepository.existsByEmail(email.trim())) {
            redirectAttributes.addFlashAttribute("error", "❌ Email \"" + email + "\" đã tồn tại trong hệ thống!");
            redirectAttributes.addFlashAttribute("prevFullName", fullName);
            redirectAttributes.addFlashAttribute("prevEmail", email);
            redirectAttributes.addFlashAttribute("prevStudentId", studentId);
            return "redirect:/admin/users/create";
        }

        // Validate studentId trùng
        if (userRepository.existsByStudentId(studentId.trim())) {
            redirectAttributes.addFlashAttribute("error", "❌ Mã học sinh \"" + studentId + "\" đã tồn tại!");
            redirectAttributes.addFlashAttribute("prevFullName", fullName);
            redirectAttributes.addFlashAttribute("prevEmail", email);
            redirectAttributes.addFlashAttribute("prevStudentId", studentId);
            return "redirect:/admin/users/create";
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setStudentId(studentId.trim());
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRole("ROLE_USER");
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());

        // Nhóm độc giả
        if (groupId != null) {
            readerGroupRepository.findById(groupId).ifPresent(user::setReaderGroup);
        }

        // Upload ảnh
        if (photoFile != null && !photoFile.isEmpty()) {
            String photoUrl = fileStorageService.storeFile(photoFile, "users");
            user.setPhotoUrl(photoUrl);
        }

        // Lưu lần đầu để có userId
        User saved = userRepository.save(user);

        // Tạo QR data: LIB-USR-{userId}-{studentId}
        String qrData = "LIB-USR-" + saved.getUserId() + "-" + saved.getStudentId();
        saved.setCardQrData(qrData);
        userRepository.save(saved);

        redirectAttributes.addFlashAttribute("message",
            "✅ Tạo thẻ thành công cho \"" + fullName + "\"! Mật khẩu mặc định: " + DEFAULT_PASSWORD);
        return "redirect:/admin/users/" + saved.getUserId() + "/card";
    }

    // ============================================================
    // CARD DETAIL - Xem thẻ + QR
    // ============================================================
    @GetMapping("/{id}/card")
    public String viewCard(@PathVariable int id, Model model) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return "redirect:/admin/users";
        model.addAttribute("user", user);
        model.addAttribute("groups", readerGroupRepository.findAll());
        return "admin/users/card";
    }

    // ============================================================
    // EDIT - Cập nhật thông tin thẻ
    // ============================================================
    @PostMapping("/{id}/edit")
    public String editUser(
            @PathVariable int id,
            @RequestParam String fullName,
            @RequestParam String studentId,
            @RequestParam String email,
            @RequestParam(required = false) Integer groupId,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            RedirectAttributes ra) {

        User user = userRepository.findById(id).orElse(null);
        if (user == null) return "redirect:/admin/users";

        // Kiểm tra email trùng (bỏ qua chính nó)
        if (!user.getEmail().equalsIgnoreCase(email.trim())) {
            if (userRepository.existsByEmail(email.trim())) {
                ra.addFlashAttribute("error", "❌ Email \"" + email + "\" đã tồn tại trong hệ thống!");
                return "redirect:/admin/users/" + id + "/card";
            }
        }

        // Kiểm tra mã học sinh trùng (bỏ qua chính nó)
        if (!studentId.trim().equalsIgnoreCase(user.getStudentId())) {
            if (userRepository.existsByStudentId(studentId.trim())) {
                ra.addFlashAttribute("error", "❌ Mã học sinh \"" + studentId + "\" đã tồn tại!");
                return "redirect:/admin/users/" + id + "/card";
            }
        }

        user.setFullName(fullName.trim());
        user.setStudentId(studentId.trim());
        user.setEmail(email.trim().toLowerCase());

        // Cập nhật nhóm
        if (groupId != null) {
            readerGroupRepository.findById(groupId).ifPresent(user::setReaderGroup);
        } else {
            user.setReaderGroup(null);
        }

        // Upload ảnh mới nếu có
        if (photoFile != null && !photoFile.isEmpty()) {
            if (user.getPhotoUrl() != null) fileStorageService.deleteFile(user.getPhotoUrl());
            String photoUrl = fileStorageService.storeFile(photoFile, "users");
            user.setPhotoUrl(photoUrl);
        }

        // Cập nhật QR data nếu studentId thay đổi
        user.setCardQrData("LIB-USR-" + id + "-" + user.getStudentId());

        userRepository.save(user);
        ra.addFlashAttribute("message", "✅ Đã cập nhật thông tin thẻ cho \"" + user.getFullName() + "\"!");
        return "redirect:/admin/users/" + id + "/card";
    }

    // ============================================================
    // TOGGLE ACTIVE - Vô hiệu / Kích hoạt tài khoản
    // ============================================================
    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable int id, RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(u -> {
            u.setIsActive(!Boolean.TRUE.equals(u.getIsActive()));
            userRepository.save(u);
        });
        ra.addFlashAttribute("message", "Đã cập nhật trạng thái tài khoản.");
        return "redirect:/admin/users";
    }

    // ============================================================
    // RESET PASSWORD - Reset về 123456
    // ============================================================
    @PostMapping("/{id}/reset-password")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@PathVariable int id) {
        Map<String, Object> resp = new HashMap<>();
        userRepository.findById(id).ifPresent(u -> {
            u.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
            userRepository.save(u);
        });
        resp.put("success", true);
        resp.put("message", "Đã reset mật khẩu về 123456!");
        return ResponseEntity.ok(resp);
    }

    // ============================================================
    // CHECK STUDENT ID - Kiểm tra mã học sinh có trùng không (AJAX)
    // ============================================================
    @GetMapping("/check-student-id")
    @ResponseBody
    public ResponseEntity<?> checkStudentId(@RequestParam String studentId) {
        boolean exists = userRepository.existsByStudentId(studentId.trim());
        Map<String, Object> resp = new HashMap<>();
        resp.put("exists", exists);
        return ResponseEntity.ok(resp);
    }

    // ============================================================
    // DELETE - Xóa thẻ
    // ============================================================
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable int id, RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(u -> {
            if (u.getPhotoUrl() != null) fileStorageService.deleteFile(u.getPhotoUrl());
            userRepository.delete(u);
        });
        ra.addFlashAttribute("message", "Đã xóa thẻ thư viện.");
        return "redirect:/admin/users";
    }
}
