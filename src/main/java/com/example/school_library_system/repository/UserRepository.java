package com.example.school_library_system.repository;

import com.example.school_library_system.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);

    long countByRole(String role);

    List<User> findByRole(String role, Pageable pageable);

    /** Tất cả user theo role, sắp xếp mới nhất trước */
    List<User> findByRoleOrderByCreatedAtDesc(String role);

    /** Tìm kiếm theo tên hoặc email hoặc studentId */
    @Query("SELECT u FROM User u WHERE u.role = :role AND (" +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
           "LOWER(u.studentId) LIKE LOWER(CONCAT('%', :kw, '%')))")
    List<User> findByRoleAndSearchKeyword(@Param("role") String role, @Param("kw") String keyword);

    /** Tra cứu user theo dữ liệu QR thẻ thư viện */
    Optional<User> findByCardQrData(String cardQrData);
}
