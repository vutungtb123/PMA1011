package com.example.school_library_system.repository;

import com.example.school_library_system.entity.ReaderGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReaderGroupRepository extends JpaRepository<ReaderGroup, Integer> {
}
