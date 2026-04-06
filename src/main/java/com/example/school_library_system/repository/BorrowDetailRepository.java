package com.example.school_library_system.repository;

import com.example.school_library_system.entity.BorrowDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowDetailRepository extends JpaRepository<BorrowDetail, Integer> {
    List<BorrowDetail> findByBookCopyCopyIdAndBorrowRecordRecordStatus(Integer copyId, String recordStatus);
}
