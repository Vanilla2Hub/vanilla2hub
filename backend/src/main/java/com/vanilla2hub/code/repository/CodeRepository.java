package com.vanilla2hub.code.repository;

import com.vanilla2hub.code.entity.Code;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodeRepository extends JpaRepository<Code, Long> {
    List<Code> findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(Long codeTypeId);
    Optional<Code> findByIdAndCodeTypeIdAndDeletedFalse(Long id, Long codeTypeId);
    boolean existsByCodeTypeIdAndCodeAndDeletedFalse(Long codeTypeId, String code);
}
