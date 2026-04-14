package com.example.school_library_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "BorrowDetails")
@Getter
@Setter
@NoArgsConstructor
public class BorrowDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DetailID")
    private Integer detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BorrowID", nullable = false)
    @JsonIgnore
    private BorrowRecord borrowRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CopyID", nullable = false)
    private BookCopy bookCopy;

    @Column(name = "ReturnDate")
    private LocalDateTime returnDate;

    @Column(name = "ReturnPhysicalState", length = 50)
    private String returnPhysicalState;

    @Column(name = "ViolationNote", length = 500)
    private String violationNote;

    @Column(name = "AssessedFine", precision = 18, scale = 2)
    private BigDecimal assessedFine = BigDecimal.ZERO;

    /**
     * Đánh dấu vi phạm đã được nộp phạt (admin xác nhận).
     * Giữ nguyên assessedFine để lưu lịch sử tiền phạt, chỉ dùng flag này để phân biệt tab.
     */
    @Column(name = "FinePaid")
    private Boolean finePaid = false;
}
