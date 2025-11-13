package com.example.gis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
                // Allow subscription to device topics (can be made authenticated if needed)
                .simpDestMatchers("/topic/devices/**", "/topic/geofences/**").permitAll()
                // Require authentication for sending messages
                .simpDestMatchers("/app/**").authenticated()
                // Allow connection (can be made authenticated if needed)
                .anyMessage().permitAll();
    }

    @Override
    protected boolean sameOriginDisabled() {
        // Allow cross-origin for WebSocket connections
        return true;
    }
}

