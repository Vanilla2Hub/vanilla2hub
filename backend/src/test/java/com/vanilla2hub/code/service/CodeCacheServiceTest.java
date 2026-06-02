package com.vanilla2hub.code.service;

import com.vanilla2hub.code.dto.CodeResponse;
import com.vanilla2hub.code.entity.Code;
import com.vanilla2hub.code.entity.CodeType;
import com.vanilla2hub.code.repository.CodeRepository;
import com.vanilla2hub.code.repository.CodeTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CodeCacheServiceTest {

    @Mock CodeRepository codeRepository;
    @Mock CodeTypeRepository codeTypeRepository;
    @Mock CacheManager cacheManager;
    @Mock Cache cache;

    @InjectMocks CodeCacheService codeCacheService;

    // --- helpers ---

    private CodeType makeCodeType(Long id, String code) {
        CodeType ct = CodeType.builder().code(code).name(code).sortOrder(0).build();
        ReflectionTestUtils.setField(ct, "id", id);
        return ct;
    }

    private Code makeCode(Long id, CodeType ct, String code) {
        Code c = Code.builder().codeType(ct).code(code).name(code).sortOrder(0).build();
        ReflectionTestUtils.setField(c, "id", id);
        ReflectionTestUtils.setField(c, "codeTypeId", ct.getId());
        return c;
    }

    // --- getByCodeTypeCode ---

    @Test
    void getByCodeTypeCode_cacheHit_returnsCachedValue() {
        List<CodeResponse> cached = List.of();
        Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);
        given(cacheManager.getCache(CodeCacheService.CACHE_NAME)).willReturn(cache);
        given(cache.get("GENDER")).willReturn(wrapper);
        given(wrapper.get()).willReturn(cached);

        List<CodeResponse> result = codeCacheService.getByCodeTypeCode("GENDER");

        assertThat(result).isSameAs(cached);
        then(codeTypeRepository).shouldHaveNoInteractions();
    }

    @Test
    void getByCodeTypeCode_cacheMiss_fallsBackToDb() {
        given(cacheManager.getCache(CodeCacheService.CACHE_NAME)).willReturn(cache);
        given(cache.get("GENDER")).willReturn(null);

        CodeType ct = makeCodeType(1L, "GENDER");
        Code code = makeCode(10L, ct, "M");
        given(codeTypeRepository.findByCodeAndDeletedFalse("GENDER")).willReturn(Optional.of(ct));
        given(codeRepository.findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(1L)).willReturn(List.of(code));

        List<CodeResponse> result = codeCacheService.getByCodeTypeCode("GENDER");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("M");
    }

    @Test
    void getByCodeTypeCode_nullWrapperValue_returnsEmpty() {
        Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);
        given(cacheManager.getCache(CodeCacheService.CACHE_NAME)).willReturn(cache);
        given(cache.get("GENDER")).willReturn(wrapper);
        given(wrapper.get()).willReturn(null);  // wrapper 존재하지만 내용이 null → DB fallback 없이 빈 리스트 반환

        List<CodeResponse> result = codeCacheService.getByCodeTypeCode("GENDER");

        assertThat(result).isEmpty();
        then(codeTypeRepository).shouldHaveNoInteractions();
    }

    @Test
    void getByCodeTypeCode_redisException_fallsBackToDb() {
        given(cacheManager.getCache(CodeCacheService.CACHE_NAME)).willReturn(cache);
        given(cache.get("GENDER")).willThrow(new RuntimeException("Redis 연결 실패"));

        CodeType ct = makeCodeType(1L, "GENDER");
        given(codeTypeRepository.findByCodeAndDeletedFalse("GENDER")).willReturn(Optional.of(ct));
        given(codeRepository.findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(1L)).willReturn(List.of());

        assertThatCode(() -> codeCacheService.getByCodeTypeCode("GENDER")).doesNotThrowAnyException();
    }

    @Test
    void getByCodeTypeCode_nullCache_fallsBackToDb() {
        given(cacheManager.getCache(CodeCacheService.CACHE_NAME)).willReturn(null);

        CodeType ct = makeCodeType(1L, "GENDER");
        given(codeTypeRepository.findByCodeAndDeletedFalse("GENDER")).willReturn(Optional.of(ct));
        given(codeRepository.findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(1L)).willReturn(List.of());

        List<CodeResponse> result = codeCacheService.getByCodeTypeCode("GENDER");

        assertThat(result).isEmpty();
    }

    @Test
    void getByCodeTypeCode_codeTypeNotFound_returnsEmpty() {
        given(cacheManager.getCache(CodeCacheService.CACHE_NAME)).willReturn(null);
        given(codeTypeRepository.findByCodeAndDeletedFalse("GHOST")).willReturn(Optional.empty());

        assertThat(codeCacheService.getByCodeTypeCode("GHOST")).isEmpty();
    }

    // --- refresh ---

    @Test
    void refresh_putsToCache() {
        CodeType ct = makeCodeType(1L, "GENDER");
        Code code = makeCode(10L, ct, "M");
        given(cacheManager.getCache(CodeCacheService.CACHE_NAME)).willReturn(cache);
        given(codeRepository.findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(1L)).willReturn(List.of(code));

        codeCacheService.refresh(1L, "GENDER");

        then(cache).should().put(eq("GENDER"), any());
    }

    @Test
    void refresh_redisException_doesNotThrow() {
        given(cacheManager.getCache(CodeCacheService.CACHE_NAME)).willReturn(cache);
        given(codeRepository.findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(1L)).willReturn(List.of());
        willThrow(new RuntimeException("Redis 연결 실패")).given(cache).put(any(), any());

        assertThatCode(() -> codeCacheService.refresh(1L, "GENDER")).doesNotThrowAnyException();
    }

    // --- evict ---

    @Test
    void evict_removesFromCache() {
        given(cacheManager.getCache(CodeCacheService.CACHE_NAME)).willReturn(cache);

        codeCacheService.evict("GENDER");

        then(cache).should().evict("GENDER");
    }

    @Test
    void evict_redisException_doesNotThrow() {
        given(cacheManager.getCache(CodeCacheService.CACHE_NAME)).willReturn(cache);
        willThrow(new RuntimeException("Redis 연결 실패")).given(cache).evict(any());

        assertThatCode(() -> codeCacheService.evict("GENDER")).doesNotThrowAnyException();
    }

    @Test
    void evict_nullCache_doesNotThrow() {
        given(cacheManager.getCache(CodeCacheService.CACHE_NAME)).willReturn(null);

        assertThatCode(() -> codeCacheService.evict("GENDER")).doesNotThrowAnyException();
    }
}
