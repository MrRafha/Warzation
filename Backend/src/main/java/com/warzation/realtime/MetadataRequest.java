package com.warzation.realtime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MetadataRequest(
        @NotBlank @Size(max = 100) String key,
        @NotBlank @Size(max = 500) String value) {
}
