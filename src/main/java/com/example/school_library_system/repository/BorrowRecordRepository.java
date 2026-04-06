package com.example.school_library_system.repository;

import com.example.school_library_system.entity.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Integer> {
    List<BorrowRecord> findByRecordStatus(String recordStatus);

    /**
     * Đếm số phiếu mượn đang hoạt động (Đang mượn / Đã đặt chỗ) của 1 user
     */
    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.user.userId = :userId " +
           "AND br.recordStatus IN ('Đang mượn', 'Đã đặt chỗ')")
    long countActiveByReaderId(@Param("userId") Integer userId);

    /**
     * Kiểm tra user đã mượn 1 đầu sách cụ thể chưa (đang mượn hoặc đã đặt chỗ)
     */
    @Query("SELECT COUNT(br) FROM BorrowRecord br JOIN br.details d " +
           "WHERE br.user.userId = :userId " +
           "AND d.bookCopy.book.bookId = :bookId " +
           "AND br.recordStatus IN ('Đang mượn', 'Đã đặt chỗ')")
    long countActiveByReaderIdAndBookId(@Param("userId") Integer userId, @Param("bookId") Integer bookId);

    /**
     * Lấy danh sách phiếu mượn của 1 user
     */
    List<BorrowRecord> findByUserUserIdOrderByBorrowIdDesc(Integer userId);
}
