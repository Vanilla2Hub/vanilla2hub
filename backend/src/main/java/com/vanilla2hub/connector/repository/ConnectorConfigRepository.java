package com.vanilla2hub.connector.repository;

import com.vanilla2hub.connector.entity.ConnectorConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectorConfigRepository extends JpaRepository<ConnectorConfig, Long> {

    List<ConnectorConfig> findAllByDeletedFalseOrderByNameAsc();

    Optional<ConnectorConfig> findByIdAndDeletedFalse(Long id);

    boolean existsByNameAndDeletedFalse(String name);

    boolean existsByNameAndIdNotAndDeletedFalse(String name, Long id);
}
