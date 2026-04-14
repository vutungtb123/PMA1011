package com.example.school_library_system.repository;

import com.example.school_library_system.entity.BorrowRecord;
import org.springframework.data.domain.Pageable;
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
           "AND br.recordStatus IN (:statuses)")
    long _countActiveByReaderId(@Param("userId") Integer userId, @Param("statuses") List<String> statuses);

    default long countActiveByReaderId(Integer userId) {
        return _countActiveByReaderId(userId, java.util.Arrays.asList("Đang mượn", "Đã đặt chỗ"));
    }

    /**
     * Đếm TỔNG SỐ CUỐN SÁCH (tails) đang mượn/đặt chỗ của 1 user
     */
    @Query("SELECT COUNT(d) FROM BorrowRecord br JOIN br.details d WHERE br.user.userId = :userId " +
           "AND br.recordStatus IN (:statuses)")
    long _countActiveBooksByReaderId(@Param("userId") Integer userId, @Param("statuses") List<String> statuses);

    default long countActiveBooksByReaderId(Integer userId) {
        return _countActiveBooksByReaderId(userId, java.util.Arrays.asList("Đang mượn", "Đã đặt chỗ"));
    }

    /**
     * Kiểm tra user đã mượn 1 đầu sách cụ thể chưa (đang mượn hoặc đã đặt chỗ)
     */
    @Query("SELECT COUNT(br) FROM BorrowRecord br JOIN br.details d " +
           "WHERE br.user.userId = :userId " +
           "AND d.bookCopy.book.bookId = :bookId " +
           "AND br.recordStatus IN (:statuses)")
    long _countActiveByReaderIdAndBookId(@Param("userId") Integer userId, @Param("bookId") Integer bookId, @Param("statuses") List<String> statuses);

    default long countActiveByReaderIdAndBookId(Integer userId, Integer bookId) {
        return _countActiveByReaderIdAndBookId(userId, bookId, java.util.Arrays.asList("Đang mượn", "Đã đặt chỗ"));
    }

    /**
     * Lấy trạng thái cụ thể của user với 1 cuốn sách (để phân biệt Đã đặt chỗ / Đang mượn)
     */
    @Query("SELECT br.recordStatus FROM BorrowRecord br JOIN br.details d " +
           "WHERE br.user.userId = :userId " +
           "AND d.bookCopy.book.bookId = :bookId " +
           "AND br.recordStatus IN (:statuses) " +
           "ORDER BY br.borrowId DESC")
    List<String> _findActiveStatusByReaderIdAndBookId(@Param("userId") Integer userId, @Param("bookId") Integer bookId, @Param("statuses") List<String> statuses);

    default List<String> findActiveStatusByReaderIdAndBookId(Integer userId, Integer bookId) {
        return _findActiveStatusByReaderIdAndBookId(userId, bookId, java.util.Arrays.asList("Đang mượn", "Đã đặt chỗ"));
    }

    /**
     * Lấy danh sách bookId mà user đang mượn/đặt chỗ (để hiển thị badge trên trang home)
     */
    @Query("SELECT DISTINCT d.bookCopy.book.bookId FROM BorrowRecord br JOIN br.details d " +
           "WHERE br.user.userId = :userId " +
           "AND br.recordStatus IN (:statuses)")
    List<Integer> _findActiveBorrowedBookIdsByUserId(@Param("userId") Integer userId, @Param("statuses") List<String> statuses);

    default List<Integer> findActiveBorrowedBookIdsByUserId(Integer userId) {
        return _findActiveBorrowedBookIdsByUserId(userId, java.util.Arrays.asList("Đang mượn", "Đã đặt chỗ"));
    }

    /**
     * Tìm phiếu "Đã đặt chỗ" (chưa xác nhận) của user để gộp sách mới vào.
     */
    @Query("SELECT br FROM BorrowRecord br WHERE br.user.userId = :userId " +
           "AND br.recordStatus = :status " +
           "AND br.borrowDate IS NULL " +
           "ORDER BY br.borrowId DESC")
    List<BorrowRecord> _findPendingByUserId(@Param("userId") Integer userId, @Param("status") String status);

    default List<BorrowRecord> findPendingByUserId(Integer userId) {
        return _findPendingByUserId(userId, "Đã đặt chỗ");
    }

    /**
     * Lấy danh sách phiếu mượn của 1 user
     */
    List<BorrowRecord> findByUserUserIdOrderByBorrowIdDesc(Integer userId);

    /**
     * Thống kê lượt mượn theo tháng — tính cả phiếu đang mượn và đã trả.
     * Trả về Object[]{Integer year, Integer month, Long count}
     */
    @Query("SELECT YEAR(br.borrowDate), MONTH(br.borrowDate), COUNT(br) " +
           "FROM BorrowRecord br " +
           "WHERE br.recordStatus IN (:statuses) " +
           "GROUP BY YEAR(br.borrowDate), MONTH(br.borrowDate) " +
           "ORDER BY YEAR(br.borrowDate), MONTH(br.borrowDate)")
    List<Object[]> _findMonthlyBorrowStats(@Param("statuses") List<String> statuses);

    default List<Object[]> findMonthlyBorrowStats() {
        return _findMonthlyBorrowStats(java.util.Arrays.asList("Đang mượn", "Đã trả"));
    }

    /**
     * Top N độc giả mượn nhiều nhất — tính cả phiếu đang mượn và đã trả.
     * Trả về Object[]{String fullName, Long borrowCount}
     */
    @Query("SELECT br.user.fullName, COUNT(br) AS cnt " +
           "FROM BorrowRecord br " +
           "WHERE br.recordStatus IN (:statuses) " +
           "GROUP BY br.user.userId, br.user.fullName " +
           "ORDER BY cnt DESC")
    List<Object[]> _findTopReaders(Pageable pageable, @Param("statuses") List<String> statuses);

    default List<Object[]> findTopReaders(Pageable pageable) {
        return _findTopReaders(pageable, java.util.Arrays.asList("Đang mượn", "Đã trả"));
    }
}
