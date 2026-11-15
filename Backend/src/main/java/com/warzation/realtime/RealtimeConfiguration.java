package com.warzation.realtime;

import com.warzation.config.WebProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class RealtimeConfiguration implements WebSocketConfigurer {

    public static final String ENDPOINT = "/ws";

    private final RoomWebSocketHandler handler;
    private final WebProperties webProperties;

    public RealtimeConfiguration(RoomWebSocketHandler handler, WebProperties webProperties) {
        this.handler = handler;
        this.webProperties = webProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, ENDPOINT).setAllowedOrigins(webProperties.allowedOriginsArray());
    }
}
