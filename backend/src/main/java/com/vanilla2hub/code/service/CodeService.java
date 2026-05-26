package com.vanilla2hub.code.service;

import com.vanilla2hub.code.dto.CodeRequest;
import com.vanilla2hub.code.dto.CodeResponse;
import com.vanilla2hub.code.entity.Code;
import com.vanilla2hub.code.repository.CodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeService {

    private final CodeRepository codeRepository;
    private final CodeTypeService codeTypeService;

    public List<CodeResponse> getAllByCodeTypeId(Long codeTypeId) {
        codeTypeService.findOrThrow(codeTypeId);
        return codeRepository.findAllByCodeTypeIdAndDeletedFalseOrderBySortOrderAsc(codeTypeId)
                .stream().map(CodeResponse::from).toList();
    }

    @Transactional
    public CodeResponse create(Long codeTypeId, CodeRequest request) {
        var codeType = codeTypeService.findOrThrow(codeTypeId);
        if (codeRepository.existsByCodeTypeIdAndCodeAndDeletedFalse(codeTypeId, request.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 코드입니다: " + request.code());
        }
        Code code = Code.builder()
                .codeType(codeType)
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .extra(request.extra())
                .sortOrder(request.sortOrder())
                .build();
        return CodeResponse.from(codeRepository.save(code));
    }

    @Transactional
    public CodeResponse update(Long codeTypeId, Long codeId, CodeRequest request) {
        codeTypeService.findOrThrow(codeTypeId);
        Code code = findOrThrow(codeId, codeTypeId);
        code.update(request.name(), request.description(), request.extra(), request.sortOrder());
        return CodeResponse.from(code);
    }

    @Transactional
    public void delete(Long codeTypeId, Long codeId) {
        codeTypeService.findOrThrow(codeTypeId);
        findOrThrow(codeId, codeTypeId).delete();
    }

    private Code findOrThrow(Long codeId, Long codeTypeId) {
        return codeRepository.findByIdAndCodeTypeIdAndDeletedFalse(codeId, codeTypeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "코드를 찾을 수 없습니다: " + codeId));
    }
}
