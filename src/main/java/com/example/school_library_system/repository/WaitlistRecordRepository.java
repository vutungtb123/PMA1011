package com.example.school_library_system.repository;

import com.example.school_library_system.entity.WaitlistRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaitlistRecordRepository extends JpaRepository<WaitlistRecord, Integer> {
    
    // Tìm các đơn đăng ký của người dùng
    List<WaitlistRecord> findByUserUserIdOrderByRegisterDateDesc(Integer userId);
    
    // Kiểm tra xem người dùng đã đăng ký chờ cuốn sách này chưa
    long countByUserUserIdAndBookBookIdAndStatus(Integer userId, Integer bookId, String status);

    // Tìm những người đang chờ cuốn sách này, xếp ưu tiên theo thời gian đăng ký (Cũ nhất lên đầu)
    List<WaitlistRecord> findByBookBookIdAndStatusOrderByRegisterDateAsc(Integer bookId, String status);
}
