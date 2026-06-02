package com.vanilla2hub.code.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilla2hub.code.dto.AttributeField;
import com.vanilla2hub.code.dto.CodeRequest;
import com.vanilla2hub.code.dto.CodeResponse;
import com.vanilla2hub.code.entity.Code;
import com.vanilla2hub.code.entity.CodeType;
import com.vanilla2hub.code.repository.CodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CodeServiceTest {

    @Mock CodeRepository codeRepository;
    @Mock CodeTypeService codeTypeService;
    @Mock CodeCacheService codeCacheService;
    @Spy ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks CodeService codeService;

    // --- helpers ---

    private CodeType makeCodeType(Long id, String code, String schemaJson) {
        CodeType ct = CodeType.builder().code(code).name(code).sortOrder(0).attributeSchema(schemaJson).build();
        ReflectionTestUtils.setField(ct, "id", id);
        return ct;
    }

    private Code makeCode(Long id, CodeType codeType, String code, String extra) {
        Code c = Code.builder().codeType(codeType).code(code).name(code).sortOrder(0).extra(extra).build();
        ReflectionTestUtils.setField(c, "id", id);
        ReflectionTestUtils.setField(c, "codeTypeId", codeType.getId());
        return c;
    }

    private static void assertStatus(ThrowableAssert.ThrowingCallable callable, HttpStatus expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value())
                        .isEqualTo(expected.value()));
    }

    // --- getAllByCodeTypeId ---

    @Test
    void getAllByCodeTypeId_returnsCodesFromRepo() {
        CodeType ct = makeCodeType(1L, "GENDER", null);
        Code code = makeCode(10L, ct, "M", null);
        given(codeTypeService.findOrThrow(1L)).willReturn(ct);
        given(codeRepository.findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(1L)).willReturn(List.of(code));

        List<CodeResponse> result = codeService.getAllByCodeTypeId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("M");
    }

    // --- create ---

    @Test
    void create_duplicateCode_throws409() {
        CodeType ct = makeCodeType(1L, "GENDER", null);
        given(codeTypeService.findOrThrow(1L)).willReturn(ct);
        given(codeRepository.existsByCodeTypeIdAndCodeValueAndDeletedFalse(1L, "M")).willReturn(true);

        assertStatus(() -> codeService.create(1L, new CodeRequest("M", "남성", null, null, 0)),
                HttpStatus.CONFLICT);
    }

    @Test
    void create_noSchema_storesRawExtra() {
        CodeType ct = makeCodeType(1L, "GENDER", null);
        given(codeTypeService.findOrThrow(1L)).willReturn(ct);
        given(codeRepository.existsByCodeTypeIdAndCodeValueAndDeletedFalse(1L, "M")).willReturn(false);
        given(codeTypeService.parseSchema(null)).willReturn(List.of());
        Code saved = makeCode(10L, ct, "M", "{\"raw\":true}");
        given(codeRepository.save(any())).willReturn(saved);

        CodeResponse result = codeService.create(1L, new CodeRequest("M", "남성", null, "{\"raw\":true}", 0));

        assertThat(result.extra()).isEqualTo("{\"raw\":true}");
    }

    @Test
    void create_requiredFieldMissing_throws400() throws Exception {
        AttributeField required = new AttributeField("color", "색상", "text", true, null, true, List.of(), null);
        String schema = objectMapper.writeValueAsString(List.of(required));
        CodeType ct = makeCodeType(1L, "ITEM", schema);

        given(codeTypeService.findOrThrow(1L)).willReturn(ct);
        given(codeRepository.existsByCodeTypeIdAndCodeValueAndDeletedFalse(1L, "A")).willReturn(false);
        given(codeTypeService.parseSchema(schema)).willReturn(List.of(required));

        assertStatus(() -> codeService.create(1L, new CodeRequest("A", "아이템A", null, "{}", 0)),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_invalidSelectOption_throws400() throws Exception {
        AttributeField selectField = new AttributeField("size", "크기", "select", false, null, true, List.of("S", "M", "L"), null);
        String schema = objectMapper.writeValueAsString(List.of(selectField));
        CodeType ct = makeCodeType(1L, "ITEM", schema);

        given(codeTypeService.findOrThrow(1L)).willReturn(ct);
        given(codeRepository.existsByCodeTypeIdAndCodeValueAndDeletedFalse(1L, "A")).willReturn(false);
        given(codeTypeService.parseSchema(schema)).willReturn(List.of(selectField));

        assertStatus(() -> codeService.create(1L, new CodeRequest("A", "아이템A", null, "{\"size\":\"XL\"}", 0)),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_defaultValueApplied() throws Exception {
        AttributeField field = new AttributeField("size", "크기", "text", false, "M", true, List.of(), null);
        String schema = objectMapper.writeValueAsString(List.of(field));
        CodeType ct = makeCodeType(1L, "ITEM", schema);

        given(codeTypeService.findOrThrow(1L)).willReturn(ct);
        given(codeRepository.existsByCodeTypeIdAndCodeValueAndDeletedFalse(1L, "A")).willReturn(false);
        given(codeTypeService.parseSchema(schema)).willReturn(List.of(field));
        Code saved = makeCode(10L, ct, "A", "{\"size\":\"M\"}");
        given(codeRepository.save(any())).willReturn(saved);

        codeService.create(1L, new CodeRequest("A", "아이템A", null, "{}", 0));

        then(codeRepository).should().save(argThat(c -> c.getExtra().contains("\"size\":\"M\"")));
    }

    @Test
    void create_validSelectOption_succeeds() throws Exception {
        AttributeField selectField = new AttributeField("size", "크기", "select", false, null, true, List.of("S", "M", "L"), null);
        String schema = objectMapper.writeValueAsString(List.of(selectField));
        CodeType ct = makeCodeType(1L, "ITEM", schema);

        given(codeTypeService.findOrThrow(1L)).willReturn(ct);
        given(codeRepository.existsByCodeTypeIdAndCodeValueAndDeletedFalse(1L, "A")).willReturn(false);
        given(codeTypeService.parseSchema(schema)).willReturn(List.of(selectField));
        Code saved = makeCode(10L, ct, "A", "{\"size\":\"M\"}");
        given(codeRepository.save(any())).willReturn(saved);

        assertThatCode(() -> codeService.create(1L, new CodeRequest("A", "아이템A", null, "{\"size\":\"M\"}", 0)))
                .doesNotThrowAnyException();
    }

    // --- update ---

    @Test
    void update_systemDefault_throws403() {
        CodeType ct = makeCodeType(1L, "GENDER", null);
        Code code = makeCode(10L, ct, "M", null);
        ReflectionTestUtils.setField(code, "systemDefault", true);

        given(codeTypeService.findOrThrow(1L)).willReturn(ct);
        given(codeRepository.findByIdAndCodeTypeIdAndDeletedFalse(10L, 1L)).willReturn(Optional.of(code));

        assertStatus(() -> codeService.update(1L, 10L, new CodeRequest("M", "남성", null, null, 0)),
                HttpStatus.FORBIDDEN);
    }

    @Test
    void update_nonEditableField_preservedFromExisting() throws Exception {
        AttributeField field = new AttributeField("region", "지역", "text", false, null, false, List.of(), null);
        String schema = objectMapper.writeValueAsString(List.of(field));
        CodeType ct = makeCodeType(1L, "ITEM", schema);
        Code code = makeCode(10L, ct, "A", "{\"region\":\"KR\"}");

        given(codeTypeService.findOrThrow(1L)).willReturn(ct);
        given(codeRepository.findByIdAndCodeTypeIdAndDeletedFalse(10L, 1L)).willReturn(Optional.of(code));
        given(codeTypeService.parseSchema(schema)).willReturn(List.of(field));

        codeService.update(1L, 10L, new CodeRequest("A", "아이템A", null, "{\"region\":\"US\"}", 0));

        // editable=false 이므로 기존값 KR이 유지돼야 함
        assertThat(code.getExtra()).contains("\"region\":\"KR\"");
    }

    @Test
    void update_success() throws Exception {
        CodeType ct = makeCodeType(1L, "GENDER", null);
        Code code = makeCode(10L, ct, "M", null);

        given(codeTypeService.findOrThrow(1L)).willReturn(ct);
        given(codeRepository.findByIdAndCodeTypeIdAndDeletedFalse(10L, 1L)).willReturn(Optional.of(code));
        given(codeTypeService.parseSchema(null)).willReturn(List.of());

        codeService.update(1L, 10L, new CodeRequest("M", "남성(수정)", null, null, 1));

        assertThat(code.getName()).isEqualTo("남성(수정)");
        then(codeCacheService).should().refresh(1L, "GENDER");
    }

    // --- delete ---

    @Test
    void delete_systemDefault_throws403() {
        CodeType ct = makeCodeType(1L, "GENDER", null);
        Code code = makeCode(10L, ct, "M", null);
        ReflectionTestUtils.setField(code, "systemDefault", true);

        given(codeTypeService.findOrThrow(1L)).willReturn(ct);
        given(codeRepository.findByIdAndCodeTypeIdAndDeletedFalse(10L, 1L)).willReturn(Optional.of(code));

        assertStatus(() -> codeService.delete(1L, 10L), HttpStatus.FORBIDDEN);
    }

    @Test
    void delete_codeRefInUse_throws409() {
        CodeType ct = makeCodeType(1L, "TARGET", null);
        Code code = makeCode(10L, ct, "OKTA", null);

        given(codeTypeService.findOrThrow(1L)).willReturn(ct);
        given(codeRepository.findByIdAndCodeTypeIdAndDeletedFalse(10L, 1L)).willReturn(Optional.of(code));
        given(codeTypeService.findCodeReferences("TARGET", "OKTA")).willReturn(List.of("CONNECTOR > C1"));

        assertStatus(() -> codeService.delete(1L, 10L), HttpStatus.CONFLICT);
    }

    @Test
    void delete_success() {
        CodeType ct = makeCodeType(1L, "GENDER", null);
        Code code = makeCode(10L, ct, "M", null);

        given(codeTypeService.findOrThrow(1L)).willReturn(ct);
        given(codeRepository.findByIdAndCodeTypeIdAndDeletedFalse(10L, 1L)).willReturn(Optional.of(code));
        given(codeTypeService.findCodeReferences("GENDER", "M")).willReturn(List.of());

        codeService.delete(1L, 10L);

        assertThat(code.isDeleted()).isTrue();
        then(codeCacheService).should().refresh(1L, "GENDER");
    }

    @Test
    void findOrThrow_notFound_throws404() {
        CodeType ct = makeCodeType(1L, "GENDER", null);
        given(codeTypeService.findOrThrow(1L)).willReturn(ct);
        given(codeRepository.findByIdAndCodeTypeIdAndDeletedFalse(99L, 1L)).willReturn(Optional.empty());

        assertStatus(() -> codeService.update(1L, 99L, new CodeRequest("X", "X", null, null, 0)),
                HttpStatus.NOT_FOUND);
    }
}
