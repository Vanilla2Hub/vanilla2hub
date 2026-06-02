package com.vanilla2hub.code.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AttributeField(
        String key,
        String label,
        String type,
        boolean required,
        String defaultValue,
        boolean editable,
        List<String> options,
        String refCodeTypeCode   // type=code_ref 일 때만 사용
) {
    public static final String TYPE_TEXT     = "text";
    public static final String TYPE_NUMBER   = "number";
    public static final String TYPE_BOOLEAN  = "boolean";
    public static final String TYPE_SELECT   = "select";
    public static final String TYPE_CODE_REF = "code_ref";
}
