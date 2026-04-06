package com.example.school_library_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "BookSamplePages")
@Getter
@Setter
@NoArgsConstructor
public class BookSamplePage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PageID")
    private Integer pageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookID", nullable = false)
    @JsonIgnore
    private Book book;

    @Column(name = "PageNumber", nullable = false)
    private Integer pageNumber; // 1 to 10

    @Column(name = "IsImage", nullable = false)
    private Boolean isImage = false;

    @Column(name = "Content", columnDefinition = "NVARCHAR(MAX)")
    private String content; // Text or Image URL
}
