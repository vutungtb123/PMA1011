package com.example.school_library_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Books")
@Getter
@Setter
@NoArgsConstructor
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BookID")
    private Integer bookId;

    @Column(name = "Title", nullable = false, length = 200)
    private String title;

    @Column(name = "Author", length = 100)
    private String author;

    @Column(name = "Publisher", length = 100)
    private String publisher;

    @Column(name = "CoverImage", length = 255)
    private String coverImage;

    @Column(name = "Summary", columnDefinition = "NVARCHAR(MAX)")
    private String summary;

    @Column(name = "SampleReadLink", columnDefinition = "NVARCHAR(MAX)")
    private String sampleReadLink;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
        name = "BookCategories",
        joinColumns = @JoinColumn(name = "BookID"),
        inverseJoinColumns = @JoinColumn(name = "CategoryID")
    )
    private List<Category> categories = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookSamplePage> samplePages = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookCopy> copies = new ArrayList<>();
}
