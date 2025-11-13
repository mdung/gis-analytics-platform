package com.example.gis.service;

import com.example.gis.dto.DevicePositionMessage;
import com.example.gis.dto.GeofenceEventMessage;
import com.example.gis.entity.Device;
import com.example.gis.entity.Geofence;
import com.example.gis.repository.GeofenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;
    private final GeofenceRepository geofenceRepository;

    /**
     * Broadcast device position update
     */
    public void broadcastDevicePosition(Device device) {
        if (device.getLastPosition() == null) {
            return;
        }

        Point position = device.getLastPosition();
        DevicePositionMessage message = DevicePositionMessage.builder()
                .deviceId(device.getId())
                .deviceCode(device.getCode())
                .deviceName(device.getName())
                .longitude(position.getX())
                .latitude(position.getY())
                .timestamp(device.getUpdatedAt())
                .build();

        // Broadcast to all devices topic
        messagingTemplate.convertAndSend("/topic/devices", message);

        // Broadcast to specific device topic
        messagingTemplate.convertAndSend("/topic/devices." + device.getCode(), message);
        messagingTemplate.convertAndSend("/topic/devices." + device.getId(), message);

        log.debug("Broadcasted position for device: {}", device.getCode());
    }

    /**
     * Broadcast geofence enter event
     */
    public void broadcastGeofenceEnter(Device device, Geofence geofence) {
        if (device.getLastPosition() == null) {
            return;
        }

        Point position = device.getLastPosition();
        GeofenceEventMessage message = GeofenceEventMessage.builder()
                .deviceId(device.getId())
                .deviceCode(device.getCode())
                .deviceName(device.getName())
                .geofenceId(geofence.getId())
                .geofenceName(geofence.getName())
                .eventType("ENTER")
                .longitude(position.getX())
                .latitude(position.getY())
                .timestamp(java.time.OffsetDateTime.now())
                .build();

        // Broadcast to geofence events topic
        messagingTemplate.convertAndSend("/topic/geofences", message);
        messagingTemplate.convertAndSend("/topic/geofences." + geofence.getId(), message);
        messagingTemplate.convertAndSend("/topic/devices." + device.getCode() + ".geofences", message);

        log.info("Device {} entered geofence {}", device.getCode(), geofence.getName());
    }

    /**
     * Broadcast geofence exit event
     */
    public void broadcastGeofenceExit(Device device, Geofence geofence) {
        if (device.getLastPosition() == null) {
            return;
        }

        Point position = device.getLastPosition();
        GeofenceEventMessage message = GeofenceEventMessage.builder()
                .deviceId(device.getId())
                .deviceCode(device.getCode())
                .deviceName(device.getName())
                .geofenceId(geofence.getId())
                .geofenceName(geofence.getName())
                .eventType("EXIT")
                .longitude(position.getX())
                .latitude(position.getY())
                .timestamp(java.time.OffsetDateTime.now())
                .build();

        // Broadcast to geofence events topic
        messagingTemplate.convertAndSend("/topic/geofences", message);
        messagingTemplate.convertAndSend("/topic/geofences." + geofence.getId(), message);
        messagingTemplate.convertAndSend("/topic/devices." + device.getCode() + ".geofences", message);

        log.info("Device {} exited geofence {}", device.getCode(), geofence.getName());
    }

    /**
     * Check geofence enter/exit and broadcast events
     */
    public void checkAndBroadcastGeofenceEvents(Device device, Point previousPosition) {
        if (device.getLastPosition() == null) {
            return;
        }

        Point currentPosition = device.getLastPosition();
        List<Geofence> activeGeofences = geofenceRepository.findByActiveTrueAndDeletedAtIsNull();

        for (Geofence geofence : activeGeofences) {
            boolean wasInside = previousPosition != null && 
                    geofence.getGeom().contains(previousPosition);
            boolean isInside = geofence.getGeom().contains(currentPosition);

            if (!wasInside && isInside) {
                // Entered geofence
                broadcastGeofenceEnter(device, geofence);
            } else if (wasInside && !isInside) {
                // Exited geofence
                broadcastGeofenceExit(device, geofence);
            }
        }
    }
}

