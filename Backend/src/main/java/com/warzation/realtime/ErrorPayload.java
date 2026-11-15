package com.warzation.realtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorPayload(String code, String message, Map<String, String> fields) {

    public static ErrorPayload of(String code, String message) {
        return new ErrorPayload(code, message, Map.of());
    }
}
