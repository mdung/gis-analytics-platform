package com.example.gis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceEventMessage {
    private UUID deviceId;
    private String deviceCode;
    private String deviceName;
    private UUID geofenceId;
    private String geofenceName;
    private String eventType; // ENTER or EXIT
    private Double longitude;
    private Double latitude;
    private OffsetDateTime timestamp;
}

