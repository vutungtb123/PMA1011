package com.example.school_library_system.repository;

import com.example.school_library_system.entity.BookCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Integer> {
    List<BookCopy> findByBookBookId(Integer bookId);
    long countByBookBookIdAndPhysicalStatus(Integer bookId, String physicalStatus);
    long countByBookBookId(Integer bookId); // Tổng bản sao mọi trạng thái
    boolean existsByBarcode(String barcode); // Kiểm tra trùng mã vạch

    /** Tra cứu bản sao sách theo mã barcode */
    java.util.Optional<BookCopy> findByBarcode(String barcode);
}
