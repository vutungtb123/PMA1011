package com.example.school_library_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "BorrowRecords")
@Getter
@Setter
@NoArgsConstructor
public class BorrowRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BorrowID")
    private Integer borrowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;

    @Column(name = "BorrowDate")
    private LocalDateTime borrowDate; // Null khi "Đã đặt chỗ", set khi admin xác nhận giao sách

    @Column(name = "DueDate", nullable = true)
    private LocalDateTime dueDate;   // Null khi "Đã đặt chỗ", set khi admin xác nhận giao sách

    @Column(name = "RecordStatus", length = 50)
    private String recordStatus = "Đã đặt chỗ"; // Default là Đã đặt chỗ, admin mới chuyển sang Đang mượn
    // Status can be: 'Đã đặt chỗ', 'Đang mượn', 'Đã trả', 'Đã hủy'

    @Column(name = "CancellationReason", length = 500)
    private String cancellationReason;

    @Column(name = "IsReminded")
    private Boolean isReminded = false;

    @OneToMany(mappedBy = "borrowRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BorrowDetail> details = new ArrayList<>();
}
