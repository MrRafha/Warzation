package com.warzation.realtime;

import com.warzation.domain.ApplicationMetadata;
import java.time.OffsetDateTime;

public record MetadataResponse(
        Long id,
        String key,
        String value,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static MetadataResponse from(ApplicationMetadata metadata) {
        return new MetadataResponse(
                metadata.getId(),
                metadata.getKey(),
                metadata.getValue(),
                metadata.getCreatedAt(),
                metadata.getUpdatedAt());
    }
}
