package com.warzation.application.map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/** Lê e valida um cenário a partir do scenario.json e do arquivo de geometria ao lado dele. */
@Component
public class ScenarioReader {

    private final ObjectMapper strictMapper;

    public ScenarioReader(ObjectMapper objectMapper) {
        this.strictMapper = objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    public LoadedScenario read(Resource scenarioResource) {
        String source = describe(scenarioResource);
        byte[] scenarioBytes = bytes(scenarioResource, source);

        ScenarioFile file;
        try {
            file = strictMapper.readValue(scenarioBytes, ScenarioFile.class);
        } catch (JsonMappingException exception) {
            throw new ScenarioValidationException(source, List.of(
                    exception.getOriginalMessage().split("\n")[0] + " em " + path(exception)));
        } catch (IOException exception) {
            throw new ScenarioValidationException(source, List.of("JSON inválido: " + exception.getMessage()));
        }

        if (file.geometryFile() == null || file.geometryFile().contains("/") || file.geometryFile().contains("\\")) {
            throw new ScenarioValidationException(source, List.of(
                    "geometryFile precisa ser o nome de um arquivo na mesma pasta"));
        }
        Resource geometryResource;
        try {
            geometryResource = scenarioResource.createRelative(file.geometryFile());
        } catch (IOException exception) {
            throw new ScenarioValidationException(source, List.of("geometria inacessível: " + file.geometryFile()));
        }
        if (!geometryResource.exists()) {
            throw new ScenarioValidationException(source, List.of("arquivo de geometria não encontrado: "
                    + file.geometryFile()));
        }
        byte[] geometryBytes = bytes(geometryResource, describe(geometryResource));
        JsonNode geometry;
        try {
            geometry = strictMapper.readTree(geometryBytes);
        } catch (JsonProcessingException exception) {
            throw new ScenarioValidationException(describe(geometryResource), List.of("GeoJSON inválido"));
        } catch (IOException exception) {
            throw new ScenarioValidationException(describe(geometryResource), List.of(exception.getMessage()));
        }

        ScenarioValidator.validate(file, geometry, source);
        return LoadedScenario.of(file, geometry, sha256(scenarioBytes, geometryBytes));
    }

    private static byte[] bytes(Resource resource, String source) {
        try (InputStream input = resource.getInputStream()) {
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new ScenarioValidationException(source, List.of("não foi possível ler: " + exception.getMessage()));
        }
    }

    private static String path(JsonMappingException exception) {
        return "$" + exception.getPath().stream()
                .map(reference -> reference.getFieldName() != null
                        ? "." + reference.getFieldName()
                        : "[" + reference.getIndex() + "]")
                .collect(Collectors.joining());
    }

    static String describe(Resource resource) {
        try {
            String uri = resource.getURI().toString();
            int index = uri.lastIndexOf("data/scenarios/");
            return index >= 0 ? uri.substring(index) : uri;
        } catch (IOException exception) {
            return resource.getDescription();
        }
    }

    private static String sha256(byte[]... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (byte[] part : parts) {
                digest.update(part);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
