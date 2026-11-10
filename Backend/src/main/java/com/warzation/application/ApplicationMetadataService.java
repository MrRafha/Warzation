package com.warzation.application;

import com.warzation.domain.ApplicationMetadata;
import com.warzation.domain.ApplicationMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationMetadataService {

    private final ApplicationMetadataRepository repository;

    public ApplicationMetadataService(ApplicationMetadataRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ApplicationMetadata save(String key, String value) {
        ApplicationMetadata metadata = repository.findByKey(key)
                .map(existing -> {
                    existing.updateValue(value);
                    return existing;
                })
                .orElseGet(() -> new ApplicationMetadata(key, value));
        return repository.save(metadata);
    }

    @Transactional(readOnly = true)
    public ApplicationMetadata findByKey(String key) {
        return repository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Metadata não encontrada: " + key));
    }
}
