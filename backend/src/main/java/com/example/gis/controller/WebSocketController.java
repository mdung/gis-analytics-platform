package com.example.gis.controller;

import com.example.gis.dto.DevicePositionMessage;
import com.example.gis.entity.Device;
import com.example.gis.service.DeviceService;
import com.example.gis.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {
    private final DeviceService deviceService;
    private final WebSocketService webSocketService;

    /**
     * Handle device position updates via WebSocket
     * Client sends to: /app/device/position
     * Server broadcasts to: /topic/devices, /topic/devices.{code}
     */
    @MessageMapping("/device/position")
    @SendTo("/topic/devices")
    public DevicePositionMessage handleDevicePosition(
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        
        try {
            String deviceCode = (String) payload.get("deviceCode");
            Map<String, Number> coordinates = (Map<String, Number>) payload.get("coordinates");
            
            if (deviceCode == null || coordinates == null) {
                log.warn("Invalid position update payload: {}", payload);
                return null;
            }
            
            double lng = coordinates.get("lng") != null ? 
                    coordinates.get("lng").doubleValue() : 
                    coordinates.get("longitude").doubleValue();
            double lat = coordinates.get("lat") != null ? 
                    coordinates.get("lat").doubleValue() : 
                    coordinates.get("latitude").doubleValue();
            
            // Update device position (this will trigger broadcast)
            Device device = deviceService.updatePosition(deviceCode, lng, lat);
            
            // Return message for /topic/devices
            if (device.getLastPosition() != null) {
                return DevicePositionMessage.builder()
                        .deviceId(device.getId())
                        .deviceCode(device.getCode())
                        .deviceName(device.getName())
                        .longitude(device.getLastPosition().getX())
                        .latitude(device.getLastPosition().getY())
                        .timestamp(device.getUpdatedAt())
                        .build();
            }
        } catch (Exception e) {
            log.error("Error handling device position update: {}", e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Handle subscription confirmation
     */
    @MessageMapping("/device/subscribe")
    public void handleDeviceSubscribe(
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String deviceCode = (String) payload.get("deviceCode");
        log.info("Device subscription request: {}", deviceCode);
        
        // Could send initial position or confirmation here
    }
}

