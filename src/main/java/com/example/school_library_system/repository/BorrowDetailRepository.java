package com.example.school_library_system.repository;

import com.example.school_library_system.entity.BorrowDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface BorrowDetailRepository extends JpaRepository<BorrowDetail, Integer> {
    List<BorrowDetail> findByBookCopyCopyIdAndBorrowRecordRecordStatus(Integer copyId, String recordStatus);

    // Lấy tất cả chi tiết có vi phạm (trả muộn hoặc sách hỏng)
    @Query("SELECT d FROM BorrowDetail d WHERE d.violationNote IS NOT NULL AND d.violationNote <> ''")
    List<BorrowDetail> findAllWithViolations();

    /**
     * Kiểm tra user có vi phạm chưa nộp phạt không (assessedFine > 0 và chưa thanh toán)
     */
    @Query("SELECT COUNT(d) > 0 FROM BorrowDetail d WHERE d.borrowRecord.user.userId = :userId " +
           "AND d.assessedFine > 0 AND (d.finePaid IS NULL OR d.finePaid = false)")
    boolean hasUnpaidViolation(@Param("userId") Integer userId);

    /**
     * Top N sách mượn nhiều nhất — tính cả phiếu đang mượn và đã trả.
     * Trả về Object[]{String title, Long borrowCount}
     */
    @Query("SELECT d.bookCopy.book.title, COUNT(d) AS cnt " +
           "FROM BorrowDetail d " +
           "WHERE d.borrowRecord.recordStatus IN (:statuses) " +
           "GROUP BY d.bookCopy.book.bookId, d.bookCopy.book.title " +
           "ORDER BY cnt DESC")
    List<Object[]> _findTopBorrowedBooks(Pageable pageable, @Param("statuses") List<String> statuses);

    default List<Object[]> findTopBorrowedBooks(Pageable pageable) {
        return _findTopBorrowedBooks(pageable, java.util.Arrays.asList("Đang mượn", "Đã trả"));
    }
}
