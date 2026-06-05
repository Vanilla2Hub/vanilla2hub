package com.vanilla2hub.app.repository;

import com.vanilla2hub.app.entity.LogicalApp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LogicalAppRepository extends JpaRepository<LogicalApp, Long> {

    List<LogicalApp> findAllByDeletedFalseOrderByNameAsc();

    Optional<LogicalApp> findByIdAndDeletedFalse(Long id);

    boolean existsByParentIdAndDeletedFalse(Long parentId);
}
