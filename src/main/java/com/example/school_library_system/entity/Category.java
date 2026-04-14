package com.example.school_library_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "Categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CategoryID")
    private Integer categoryId;

    @Column(name = "CategoryName", nullable = false, unique = true, length = 100)
    private String categoryName;

    @Column(name = "Description", length = 255)
    private String description;
    
    // ManyToMany relationship can be mapped here later
    // For now we map it from Book side usually
}
