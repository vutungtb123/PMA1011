package com.example.school_library_system.repository;

import com.example.school_library_system.entity.BookSamplePage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookSamplePageRepository extends JpaRepository<BookSamplePage, Integer> {
    List<BookSamplePage> findByBookBookIdOrderByPageNumberAsc(Integer bookId);
}
