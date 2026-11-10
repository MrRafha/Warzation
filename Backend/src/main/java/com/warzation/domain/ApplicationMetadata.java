package com.warzation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "application_metadata")
public class ApplicationMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metadata_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "metadata_value", nullable = false, length = 500)
    private String value;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ApplicationMetadata() {
    }

    public ApplicationMetadata(String key, String value) {
        this.key = key;
        this.value = value;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateValue(String value) {
        this.value = value;
        this.updatedAt = OffsetDateTime.now();
    }
}
