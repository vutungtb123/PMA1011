package com.example.school_library_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "WaitlistRecords")
@Getter
@Setter
@NoArgsConstructor
public class WaitlistRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WaitlistID")
    private Integer waitlistId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookID", nullable = false)
    private Book book;

    @Column(name = "RegisterDate", nullable = false)
    private LocalDateTime registerDate = LocalDateTime.now();

    @Column(name = "Status", length = 50)
    private String status = "Đang chờ"; // "Đang chờ", "Đã nhận sách", "Đã hủy"
}
