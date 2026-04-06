package com.example.school_library_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ReaderGroups")
@Getter
@Setter
@NoArgsConstructor
public class ReaderGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GroupID")
    private Integer groupId;

    @Column(name = "GroupName", nullable = false, length = 50)
    private String groupName;

    @Column(name = "MaxBorrowLimit", nullable = false)
    private Integer maxBorrowLimit;

    @Column(name = "MaxBorrowDays", nullable = false)
    private Integer maxBorrowDays;
}
