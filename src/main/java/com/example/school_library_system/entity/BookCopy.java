package com.example.school_library_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "BookCopies")
@Getter
@Setter
@NoArgsConstructor
public class BookCopy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CopyID")
    private Integer copyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookID", nullable = false)
    @JsonIgnore
    private Book book;

    @Column(name = "Barcode", nullable = false, unique = true, length = 50)
    private String barcode;

    @Column(name = "PhysicalStatus", length = 50)
    private String physicalStatus = "Sẵn sàng"; 
    // Trạng thái: 'Sẵn sàng', 'Đang mượn', 'Bảo trì', 'Mất', 'Đã thanh lý', 'Đã đặt trước'
}
