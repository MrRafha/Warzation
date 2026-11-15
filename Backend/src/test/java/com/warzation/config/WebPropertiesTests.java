package com.warzation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class WebPropertiesTests {

    @Test
    void acceptsExplicitOrigins() {
        WebProperties properties = new WebProperties(List.of(" http://localhost:5173 ", "http://127.0.0.1:5173"));

        assertThat(properties.allowedOrigins()).containsExactly("http://localhost:5173", "http://127.0.0.1:5173");
    }

    @Test
    void rejectsWildcardOrigins() {
        assertThatThrownBy(() -> new WebProperties(List.of("*")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WebProperties(List.of("http://*.exemplo.com")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyOrigins() {
        assertThatThrownBy(() -> new WebProperties(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WebProperties(List.of(" ")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
