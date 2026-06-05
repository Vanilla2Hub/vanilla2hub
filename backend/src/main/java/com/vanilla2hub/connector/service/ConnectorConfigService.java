package com.vanilla2hub.connector.service;

import com.vanilla2hub.connector.dto.ConnectorConfigRequest;
import com.vanilla2hub.connector.dto.ConnectorConfigResponse;
import com.vanilla2hub.connector.entity.ConnectorConfig;
import com.vanilla2hub.connector.repository.ConnectorConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConnectorConfigService {

    private final ConnectorConfigRepository connectorConfigRepository;

    public List<ConnectorConfigResponse> getAll() {
        return connectorConfigRepository.findAllByDeletedFalseOrderByNameAsc()
                .stream().map(ConnectorConfigResponse::from).toList();
    }

    public ConnectorConfigResponse getById(Long id) {
        return ConnectorConfigResponse.from(findOrThrow(id));
    }

    @Transactional
    public ConnectorConfigResponse create(ConnectorConfigRequest request) {
        if (connectorConfigRepository.existsByNameAndDeletedFalse(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 커넥터 이름입니다: " + request.name());
        }
        ConnectorConfig config = ConnectorConfig.builder()
                .name(request.name())
                .baseUrl(request.baseUrl())
                .authType(request.authType())
                .vaultSecretPath(request.vaultSecretPath())
                .timeoutMs(request.timeoutMs())
                .retryCount(request.retryCount())
                .enabled(request.enabled())
                .build();
        return ConnectorConfigResponse.from(connectorConfigRepository.save(config));
    }

    @Transactional
    public ConnectorConfigResponse update(Long id, ConnectorConfigRequest request) {
        ConnectorConfig config = findOrThrow(id);
        if (connectorConfigRepository.existsByNameAndIdNotAndDeletedFalse(request.name(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 커넥터 이름입니다: " + request.name());
        }
        config.update(request.name(), request.baseUrl(), request.authType(), request.vaultSecretPath(),
                request.timeoutMs(), request.retryCount(), request.enabled());
        return ConnectorConfigResponse.from(config);
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id).delete();
    }

    private ConnectorConfig findOrThrow(Long id) {
        return connectorConfigRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "커넥터를 찾을 수 없습니다: " + id));
    }
}
