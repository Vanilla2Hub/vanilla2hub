package com.vanilla2hub.code.service;

import com.vanilla2hub.code.repository.CodeTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class CacheWarmupListener implements ApplicationListener<ApplicationReadyEvent> {

    private final CodeTypeRepository codeTypeRepository;
    private final CodeCacheService codeCacheService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("코드 캐시 warm-up 시작");
        int count = 0;
        for (var ct : codeTypeRepository.findAllByDeletedFalseOrderBySortOrderAsc()) {
            try {
                codeCacheService.refresh(ct.getId(), ct.getCode());
                count++;
            } catch (Exception e) {
                log.warn("코드 캐시 warm-up 실패: codeType={}", ct.getCode(), e);
            }
        }
        log.info("코드 캐시 warm-up 완료: {}개 코드타입", count);
    }
}
