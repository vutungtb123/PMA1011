package com.example.school_library_system.repository;

import com.example.school_library_system.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    Page<Book> findByCategories_CategoryIdAndTitleContainingIgnoreCase(Integer categoryId, String title, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Book> searchByTitleOrAuthor(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT b FROM Book b JOIN b.categories c WHERE c.categoryId = :categoryId AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Book> searchByCategoryAndTitleOrAuthor(@Param("categoryId") Integer categoryId, @Param("keyword") String keyword, Pageable pageable);
}
