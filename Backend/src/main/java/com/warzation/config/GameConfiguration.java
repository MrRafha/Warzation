package com.warzation.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MatchRulesProperties.class)
public class GameConfiguration {

    /** Relógio único do servidor; os testes trocam por um relógio controlado. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
