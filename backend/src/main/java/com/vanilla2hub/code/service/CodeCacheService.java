package com.vanilla2hub.code.service;

import com.vanilla2hub.code.dto.CodeResponse;
import com.vanilla2hub.code.repository.CodeRepository;
import com.vanilla2hub.code.repository.CodeTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeCacheService {

    static final String CACHE_NAME = "codes";

    private final CodeRepository codeRepository;
    private final CodeTypeRepository codeTypeRepository;
    private final CacheManager cacheManager;

    /**
     * codeTypeCode 기준으로 Redis에서 코드 목록 조회. Redis miss/오류 시 DB fallback.
     */
    @Transactional(readOnly = true)
    public List<CodeResponse> getByCodeTypeCode(String codeTypeCode) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            try {
                Cache.ValueWrapper wrapper = cache.get(codeTypeCode);
                if (wrapper != null) {
                    @SuppressWarnings("unchecked")
                    List<CodeResponse> cached = (List<CodeResponse>) wrapper.get();
                    return cached != null ? cached : List.of();
                }
            } catch (Exception e) {
                log.warn("Redis read failed for {}::{}, falling back to DB", CACHE_NAME, codeTypeCode, e);
            }
        }
        return loadFromDb(codeTypeCode);
    }

    /**
     * 해당 codeType의 코드 목록을 DB에서 읽어 Redis에 적재.
     * DB 커밋 후 또는 서버 기동 시 warm-up 용도.
     */
    @Transactional(readOnly = true)
    public void refresh(Long codeTypeId, String codeTypeCode) {
        List<CodeResponse> codes = codeRepository
                .findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(codeTypeId)
                .stream().map(CodeResponse::from).toList();
        putToCache(codeTypeCode, codes);
    }

    /**
     * CodeType soft delete 시 캐시 키 제거.
     */
    public void evict(String codeTypeCode) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            try {
                cache.evict(codeTypeCode);
            } catch (Exception e) {
                log.warn("Redis evict failed for {}::{}", CACHE_NAME, codeTypeCode, e);
            }
        }
    }

    private List<CodeResponse> loadFromDb(String codeTypeCode) {
        return codeTypeRepository.findByCodeAndDeletedFalse(codeTypeCode)
                .map(ct -> codeRepository.findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(ct.getId())
                        .stream().map(CodeResponse::from).toList())
                .orElse(List.of());
    }

    private void putToCache(String codeTypeCode, List<CodeResponse> codes) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            try {
                cache.put(codeTypeCode, codes);
            } catch (Exception e) {
                log.warn("Redis write failed for {}::{}", CACHE_NAME, codeTypeCode, e);
            }
        }
    }
}
