package com.example.school_library_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GroupID")
    private ReaderGroup readerGroup;

    @Column(name = "FullName", nullable = false, length = 100)
    private String fullName;

    @Column(name = "Email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "PasswordHash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Phân quyền người dùng: "ROLE_USER" (độc giả) hoặc "ROLE_ADMIN" (quản trị viên)
     */
    @Column(name = "Role", nullable = false, length = 20)
    private String role = "ROLE_USER";

    @Column(name = "OTPCode", length = 10)
    private String otpCode;

    @Column(name = "OTPExpiredAt")
    private LocalDateTime otpExpiredAt;

    @Column(name = "UIPreferences", columnDefinition = "NVARCHAR(MAX)")
    private String uiPreferences;

    @Column(name = "IsActive")
    private Boolean isActive = true;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    /** Mã học sinh / Mã thẻ thư viện (duy nhất) */
    @Column(name = "StudentId", unique = true, length = 50)
    private String studentId;

    /** URL ảnh thẻ học sinh */
    @Column(name = "PhotoUrl", length = 255)
    private String photoUrl;

    /** Dữ liệu nội dung QR của thẻ (dạng LIB-USR-{userId}-{studentId}) */
    @Column(name = "CardQrData", length = 255)
    private String cardQrData;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.role == null) {
            this.role = "ROLE_USER";
        }
    }
}
