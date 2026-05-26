package com.vanilla2hub.code.service;

import com.vanilla2hub.code.dto.CodeTypeRequest;
import com.vanilla2hub.code.dto.CodeTypeResponse;
import com.vanilla2hub.code.entity.CodeType;
import com.vanilla2hub.code.repository.CodeTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeTypeService {

    private final CodeTypeRepository codeTypeRepository;

    public List<CodeTypeResponse> getAll() {
        return codeTypeRepository.findAllByDeletedFalseOrderBySortOrderAsc()
                .stream().map(CodeTypeResponse::from).toList();
    }

    public CodeTypeResponse getById(Long id) {
        return CodeTypeResponse.from(findOrThrow(id));
    }

    @Transactional
    public CodeTypeResponse create(CodeTypeRequest request) {
        if (codeTypeRepository.existsByCodeAndDeletedFalse(request.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 코드타입입니다: " + request.code());
        }
        CodeType codeType = CodeType.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .sortOrder(request.sortOrder())
                .build();
        return CodeTypeResponse.from(codeTypeRepository.save(codeType));
    }

    @Transactional
    public CodeTypeResponse update(Long id, CodeTypeRequest request) {
        CodeType codeType = findOrThrow(id);
        codeType.update(request.name(), request.description(), request.sortOrder());
        return CodeTypeResponse.from(codeType);
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id).delete();
    }

    CodeType findOrThrow(Long id) {
        return codeTypeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "코드타입을 찾을 수 없습니다: " + id));
    }
}
