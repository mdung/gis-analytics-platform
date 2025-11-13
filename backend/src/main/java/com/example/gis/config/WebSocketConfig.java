package com.example.gis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for topics
        config.enableSimpleBroker("/topic");
        // Set prefix for messages bound to methods annotated with @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
        // Enable user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint for SockJS fallback
        registry.addEndpoint("/ws/positions")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        // Native WebSocket endpoint (without SockJS)
        registry.addEndpoint("/ws/positions")
                .setAllowedOriginPatterns("*");
    }
}

