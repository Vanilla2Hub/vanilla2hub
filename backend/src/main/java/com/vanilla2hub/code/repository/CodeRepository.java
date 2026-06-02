package com.vanilla2hub.code.repository;

import com.vanilla2hub.code.entity.Code;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CodeRepository extends JpaRepository<Code, Long> {
    List<Code> findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(Long codeTypeId);
    List<Code> findAllByCodeTypeIdAndDeletedFalse(Long codeTypeId);
    Optional<Code> findByIdAndCodeTypeIdAndDeletedFalse(Long id, Long codeTypeId);
    boolean existsByCodeTypeIdAndCodeValueAndDeletedFalse(Long codeTypeId, String code);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE code SET extra = JSON_REMOVE(extra, CONCAT('$.', :key)) WHERE code_type_id = :codeTypeId AND deleted = 0", nativeQuery = true)
    void removeExtraKeyByCodeTypeId(@Param("codeTypeId") Long codeTypeId, @Param("key") String key);
}
