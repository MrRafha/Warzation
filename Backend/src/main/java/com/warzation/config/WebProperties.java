package com.warzation.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Origens do navegador autorizadas a chamar a API HTTP e o WebSocket.
 * Coringas são recusados para não liberar origens indiscriminadamente.
 */
@ConfigurationProperties(prefix = "application.web")
public record WebProperties(List<String> allowedOrigins) {

    public WebProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("application.web.allowed-origins precisa ter ao menos uma origem");
        }
        allowedOrigins = allowedOrigins.stream().map(String::trim).filter(origin -> !origin.isEmpty()).toList();
        if (allowedOrigins.isEmpty() || allowedOrigins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalArgumentException(
                    "application.web.allowed-origins deve listar origens explícitas, sem '*'");
        }
    }

    public String[] allowedOriginsArray() {
        return allowedOrigins.toArray(String[]::new);
    }
}
