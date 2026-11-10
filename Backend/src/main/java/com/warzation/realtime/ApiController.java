package com.warzation.realtime;

import com.warzation.application.ApplicationMetadataService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final ApplicationMetadataService metadataService;
    private final String serviceVersion;

    public ApiController(
            ApplicationMetadataService metadataService,
            @Value("${application.version:0.0.1-SNAPSHOT}") String serviceVersion) {
        this.metadataService = metadataService;
        this.serviceVersion = serviceVersion;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "warzation-backend", serviceVersion);
    }

    @PostMapping("/metadata")
    @ResponseStatus(HttpStatus.CREATED)
    public MetadataResponse saveMetadata(@Valid @RequestBody MetadataRequest request) {
        return MetadataResponse.from(metadataService.save(request.key(), request.value()));
    }

    @GetMapping("/metadata/{key}")
    public MetadataResponse getMetadata(@PathVariable String key) {
        return MetadataResponse.from(metadataService.findByKey(key));
    }
}
