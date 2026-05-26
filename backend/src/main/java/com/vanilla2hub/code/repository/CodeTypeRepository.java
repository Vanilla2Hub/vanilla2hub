package com.vanilla2hub.code.repository;

import com.vanilla2hub.code.entity.CodeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodeTypeRepository extends JpaRepository<CodeType, Long> {
    List<CodeType> findAllByDeletedFalseOrderBySortOrderAsc();
    Optional<CodeType> findByIdAndDeletedFalse(Long id);
    boolean existsByCodeAndDeletedFalse(String code);
}
