package com.vanilla2hub.app.service;

import com.vanilla2hub.app.dto.LogicalAppRequest;
import com.vanilla2hub.app.dto.LogicalAppResponse;
import com.vanilla2hub.app.entity.LogicalApp;
import com.vanilla2hub.app.repository.LogicalAppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogicalAppService {

    private final LogicalAppRepository logicalAppRepository;

    public List<LogicalAppResponse> getAll() {
        return logicalAppRepository.findAllByDeletedFalseOrderByNameAsc()
                .stream().map(LogicalAppResponse::from).toList();
    }

    public LogicalAppResponse getById(Long id) {
        return LogicalAppResponse.from(findOrThrow(id));
    }

    @Transactional
    public LogicalAppResponse create(LogicalAppRequest request) {
        LogicalApp parent = resolveParent(request.parentAppId(), null);
        LogicalApp app = LogicalApp.builder()
                .parent(parent)
                .name(request.name())
                .description(request.description())
                .owner(request.owner())
                .statusCode(request.statusCode())
                .appTypeCode(request.appTypeCode())
                .extra(request.extra())
                .build();
        return LogicalAppResponse.from(logicalAppRepository.save(app));
    }

    @Transactional
    public LogicalAppResponse update(Long id, LogicalAppRequest request) {
        LogicalApp app = findOrThrow(id);
        LogicalApp parent = resolveParent(request.parentAppId(), id);
        app.update(parent, request.name(), request.description(), request.owner(),
                request.statusCode(), request.appTypeCode(), request.extra());
        return LogicalAppResponse.from(app);
    }

    @Transactional
    public void delete(Long id) {
        LogicalApp app = findOrThrow(id);
        if (logicalAppRepository.existsByParentIdAndDeletedFalse(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "하위 앱이 존재하여 삭제할 수 없습니다.");
        }
        app.delete();
    }

    private LogicalApp findOrThrow(Long id) {
        return logicalAppRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "앱을 찾을 수 없습니다: " + id));
    }

    private LogicalApp resolveParent(Long parentAppId, Long selfId) {
        if (parentAppId == null) return null;
        if (parentAppId.equals(selfId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신을 부모로 설정할 수 없습니다.");
        }
        return findOrThrow(parentAppId);
    }
}
