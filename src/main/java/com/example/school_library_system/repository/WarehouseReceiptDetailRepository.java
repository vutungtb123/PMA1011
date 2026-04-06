package com.example.school_library_system.repository;

import com.example.school_library_system.entity.WarehouseReceiptDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseReceiptDetailRepository extends JpaRepository<WarehouseReceiptDetail, Integer> {
    // Kiểm tra sách (đã có BookID) đã có trong phiếu nhập kho chưa
    boolean existsByBookBookId(Integer bookId);

    // Fallback: chỉ kiểm tra theo tên khi BookID = NULL (dữ liệu cũ không liên kết BookID)
    @Query("SELECT COUNT(d) > 0 FROM WarehouseReceiptDetail d WHERE d.book IS NULL AND LOWER(d.bookTitle) = LOWER(:title)")
    boolean existsByBookTitleIgnoreCase(@Param("title") String title);

    // Tổng số lượng sách đã nhập kho theo BookID
    @Query("SELECT COALESCE(SUM(d.quantity), 0) FROM WarehouseReceiptDetail d WHERE d.book.bookId = :bookId")
    int sumQuantityByBookId(@Param("bookId") Integer bookId);

    // Fallback: tổng số lượng theo tên sách (CHỈ khi BookID = NULL trong dữ liệu cũ)
    @Query("SELECT COALESCE(SUM(d.quantity), 0) FROM WarehouseReceiptDetail d WHERE d.book IS NULL AND LOWER(d.bookTitle) = LOWER(:title)")
    int sumQuantityByBookTitle(@Param("title") String title);
}
