package com.warzation.config;

import com.warzation.realtime.CurrentPlayerArgumentResolver;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties({WebProperties.class, PlayersProperties.class})
public class WebConfiguration implements WebMvcConfigurer {

    private final WebProperties webProperties;
    private final CurrentPlayerArgumentResolver currentPlayerResolver;

    public WebConfiguration(WebProperties webProperties, CurrentPlayerArgumentResolver currentPlayerResolver) {
        this.webProperties = webProperties;
        this.currentPlayerResolver = currentPlayerResolver;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(webProperties.allowedOriginsArray())
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type", "Authorization")
                .maxAge(3600);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentPlayerResolver);
    }
}
