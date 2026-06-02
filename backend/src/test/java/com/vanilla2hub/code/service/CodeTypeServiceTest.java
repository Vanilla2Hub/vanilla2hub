package com.vanilla2hub.code.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanilla2hub.code.dto.AttributeField;
import com.vanilla2hub.code.dto.CodeTypeRequest;
import com.vanilla2hub.code.dto.CodeTypeResponse;
import com.vanilla2hub.code.entity.Code;
import com.vanilla2hub.code.entity.CodeType;
import com.vanilla2hub.code.repository.CodeRepository;
import com.vanilla2hub.code.repository.CodeTypeRepository;
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
class CodeTypeServiceTest {

    @Mock CodeTypeRepository codeTypeRepository;
    @Mock CodeRepository codeRepository;
    @Mock CodeCacheService codeCacheService;
    @Spy ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks CodeTypeService codeTypeService;

    // --- helpers ---

    private CodeType makeCodeType(Long id, String code, String name) {
        CodeType ct = CodeType.builder().code(code).name(name).sortOrder(0).build();
        ReflectionTestUtils.setField(ct, "id", id);
        return ct;
    }

    private CodeType makeCodeTypeWithSchema(Long id, String code, String name, String schemaJson) {
        CodeType ct = makeCodeType(id, code, name);
        ReflectionTestUtils.setField(ct, "attributeSchema", schemaJson);
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

    // --- getAll ---

    @Test
    void getAll_returnsMappedResponses() {
        given(codeTypeRepository.findAllByDeletedFalseOrderBySortOrderAsc())
                .willReturn(List.of(makeCodeType(1L, "GENDER", "성별")));

        List<CodeTypeResponse> result = codeTypeService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("GENDER");
    }

    // --- create ---

    @Test
    void create_success() {
        given(codeTypeRepository.existsByCodeAndDeletedFalse("GENDER")).willReturn(false);
        given(codeTypeRepository.save(any())).willReturn(makeCodeType(1L, "GENDER", "성별"));

        CodeTypeResponse result = codeTypeService.create(new CodeTypeRequest("GENDER", "성별", null, List.of(), 0));

        assertThat(result.code()).isEqualTo("GENDER");
    }

    @Test
    void create_duplicateCode_throws409() {
        given(codeTypeRepository.existsByCodeAndDeletedFalse("GENDER")).willReturn(true);

        assertStatus(() -> codeTypeService.create(new CodeTypeRequest("GENDER", "성별", null, List.of(), 0)),
                HttpStatus.CONFLICT);
    }

    // --- update ---

    @Test
    void update_systemDefault_throws403() {
        CodeType ct = makeCodeType(1L, "GENDER", "성별");
        ReflectionTestUtils.setField(ct, "systemDefault", true);
        given(codeTypeRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(ct));

        assertStatus(() -> codeTypeService.update(1L, new CodeTypeRequest("GENDER", "성별", null, List.of(), 0)),
                HttpStatus.FORBIDDEN);
    }

    @Test
    void update_requiredTransition_blockedWhenCodesMissingValue() throws Exception {
        AttributeField oldField = new AttributeField("color", "색상", "text", false, null, true, List.of(), null);
        String oldSchema = objectMapper.writeValueAsString(List.of(oldField));
        AttributeField newField = new AttributeField("color", "색상", "text", true, null, true, List.of(), null);

        CodeType ct = makeCodeTypeWithSchema(1L, "ITEM", "아이템", oldSchema);
        given(codeTypeRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(ct));

        Code existingCode = makeCode(10L, ct, "A", null);  // color 값 없음
        given(codeRepository.findAllByCodeTypeIdAndDeletedFalse(1L)).willReturn(List.of(existingCode));

        assertStatus(() -> codeTypeService.update(1L, new CodeTypeRequest("ITEM", "아이템", null, List.of(newField), 0)),
                HttpStatus.CONFLICT);
    }

    @Test
    void update_requiredTransition_allowedWhenAllCodesHaveValue() throws Exception {
        AttributeField oldField = new AttributeField("color", "색상", "text", false, null, true, List.of(), null);
        String oldSchema = objectMapper.writeValueAsString(List.of(oldField));
        AttributeField newField = new AttributeField("color", "색상", "text", true, null, true, List.of(), null);

        CodeType ct = makeCodeTypeWithSchema(1L, "ITEM", "아이템", oldSchema);
        given(codeTypeRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(ct));

        Code existingCode = makeCode(10L, ct, "A", "{\"color\":\"red\"}");
        given(codeRepository.findAllByCodeTypeIdAndDeletedFalse(1L)).willReturn(List.of(existingCode));

        assertThatCode(() -> codeTypeService.update(1L, new CodeTypeRequest("ITEM", "아이템", null, List.of(newField), 0)))
                .doesNotThrowAnyException();
    }

    @Test
    void update_deletedKey_triggersExtraCleanup() throws Exception {
        AttributeField oldField = new AttributeField("color", "색상", "text", false, null, true, List.of(), null);
        String oldSchema = objectMapper.writeValueAsString(List.of(oldField));

        CodeType ct = makeCodeTypeWithSchema(1L, "ITEM", "아이템", oldSchema);
        given(codeTypeRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(ct));

        // newSchema가 비어있으므로 required 전환 루프가 실행되지 않아 findAllByCodeTypeIdAndDeletedFalse 호출 없음
        codeTypeService.update(1L, new CodeTypeRequest("ITEM", "아이템 수정", null, List.of(), 0));

        then(codeRepository).should().removeExtraKeyByCodeTypeId(1L, "color");
    }

    @Test
    void update_success() {
        CodeType ct = makeCodeType(1L, "GENDER", "성별");
        given(codeTypeRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(ct));

        CodeTypeResponse result = codeTypeService.update(1L, new CodeTypeRequest("GENDER", "성별 수정", null, List.of(), 0));

        assertThat(result.name()).isEqualTo("성별 수정");
        then(codeCacheService).should().refresh(1L, "GENDER");
    }

    // --- delete ---

    @Test
    void delete_systemDefault_throws403() {
        CodeType ct = makeCodeType(1L, "GENDER", "성별");
        ReflectionTestUtils.setField(ct, "systemDefault", true);
        given(codeTypeRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(ct));

        assertStatus(() -> codeTypeService.delete(1L), HttpStatus.FORBIDDEN);
    }

    @Test
    void delete_codeRefInUse_throws409() throws Exception {
        AttributeField refField = new AttributeField("ref", "참조", "code_ref", false, null, true, List.of(), "TARGET");
        String schema = objectMapper.writeValueAsString(List.of(refField));

        CodeType target = makeCodeType(2L, "TARGET", "대상");
        CodeType connector = makeCodeTypeWithSchema(1L, "CONNECTOR", "커넥터", schema);
        Code usingCode = makeCode(10L, connector, "OKT", "{\"ref\":\"SOME_VAL\"}");

        given(codeTypeRepository.findByIdAndDeletedFalse(2L)).willReturn(Optional.of(target));
        given(codeTypeRepository.findAllByDeletedFalseOrderBySortOrderAsc()).willReturn(List.of(connector, target));
        given(codeRepository.findAllByCodeTypeIdAndDeletedFalse(1L)).willReturn(List.of(usingCode));

        assertStatus(() -> codeTypeService.delete(2L), HttpStatus.CONFLICT);
    }

    @Test
    void delete_success() {
        CodeType ct = makeCodeType(1L, "GENDER", "성별");
        given(codeTypeRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(ct));
        given(codeTypeRepository.findAllByDeletedFalseOrderBySortOrderAsc()).willReturn(List.of(ct));

        codeTypeService.delete(1L);

        assertThat(ct.isDeleted()).isTrue();
        then(codeCacheService).should().evict("GENDER");
    }

    // --- findCodeReferences ---

    @Test
    void findCodeReferences_noMatchingField_returnsEmpty() throws Exception {
        AttributeField textField = new AttributeField("size", "크기", "text", false, null, true, List.of(), null);
        CodeType ct = makeCodeTypeWithSchema(1L, "ITEM", "아이템", objectMapper.writeValueAsString(List.of(textField)));
        given(codeTypeRepository.findAllByDeletedFalseOrderBySortOrderAsc()).willReturn(List.of(ct));

        assertThat(codeTypeService.findCodeReferences("TARGET", null)).isEmpty();
    }

    @Test
    void findCodeReferences_nullTargetValue_matchesAllNonBlank() throws Exception {
        AttributeField refField = new AttributeField("ref", "참조", "code_ref", false, null, true, List.of(), "TARGET");
        CodeType ct = makeCodeTypeWithSchema(1L, "CONNECTOR", "커넥터", objectMapper.writeValueAsString(List.of(refField)));
        Code code1 = makeCode(10L, ct, "A", "{\"ref\":\"VAL1\"}");
        Code code2 = makeCode(11L, ct, "B", "{\"ref\":\"VAL2\"}");

        given(codeTypeRepository.findAllByDeletedFalseOrderBySortOrderAsc()).willReturn(List.of(ct));
        given(codeRepository.findAllByCodeTypeIdAndDeletedFalse(1L)).willReturn(List.of(code1, code2));

        assertThat(codeTypeService.findCodeReferences("TARGET", null))
                .containsExactlyInAnyOrder("CONNECTOR > A", "CONNECTOR > B");
    }

    @Test
    void findCodeReferences_specificTargetValue_matchesOnly() throws Exception {
        AttributeField refField = new AttributeField("ref", "참조", "code_ref", false, null, true, List.of(), "TARGET");
        CodeType ct = makeCodeTypeWithSchema(1L, "CONNECTOR", "커넥터", objectMapper.writeValueAsString(List.of(refField)));
        Code code1 = makeCode(10L, ct, "A", "{\"ref\":\"OKTA\"}");
        Code code2 = makeCode(11L, ct, "B", "{\"ref\":\"SAVIYNT\"}");

        given(codeTypeRepository.findAllByDeletedFalseOrderBySortOrderAsc()).willReturn(List.of(ct));
        given(codeRepository.findAllByCodeTypeIdAndDeletedFalse(1L)).willReturn(List.of(code1, code2));

        assertThat(codeTypeService.findCodeReferences("TARGET", "OKTA"))
                .containsExactly("CONNECTOR > A");
    }

    @Test
    void findCodeReferences_blankExtraValue_notMatched() throws Exception {
        AttributeField refField = new AttributeField("ref", "참조", "code_ref", false, null, true, List.of(), "TARGET");
        CodeType ct = makeCodeTypeWithSchema(1L, "CONNECTOR", "커넥터", objectMapper.writeValueAsString(List.of(refField)));
        Code code = makeCode(10L, ct, "A", "{\"ref\":\"\"}");  // 빈 값

        given(codeTypeRepository.findAllByDeletedFalseOrderBySortOrderAsc()).willReturn(List.of(ct));
        given(codeRepository.findAllByCodeTypeIdAndDeletedFalse(1L)).willReturn(List.of(code));

        assertThat(codeTypeService.findCodeReferences("TARGET", null)).isEmpty();
    }

    // --- parseSchema ---

    @Test
    void parseSchema_validJson_returnsList() throws Exception {
        AttributeField field = new AttributeField("k", "K", "text", false, null, true, List.of(), null);
        List<AttributeField> result = codeTypeService.parseSchema(objectMapper.writeValueAsString(List.of(field)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).key()).isEqualTo("k");
    }

    @Test
    void parseSchema_invalidJson_returnsEmpty() {
        assertThat(codeTypeService.parseSchema("not-json")).isEmpty();
    }

    @Test
    void parseSchema_null_returnsEmpty() {
        assertThat(codeTypeService.parseSchema(null)).isEmpty();
    }

    @Test
    void parseSchema_blank_returnsEmpty() {
        assertThat(codeTypeService.parseSchema("  ")).isEmpty();
    }

    // --- findOrThrow ---

    @Test
    void findOrThrow_notFound_throws404() {
        given(codeTypeRepository.findByIdAndDeletedFalse(99L)).willReturn(Optional.empty());

        assertStatus(() -> codeTypeService.findOrThrow(99L), HttpStatus.NOT_FOUND);
    }
}
