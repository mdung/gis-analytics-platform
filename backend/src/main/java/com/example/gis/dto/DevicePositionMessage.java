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
public class DevicePositionMessage {
    private UUID deviceId;
    private String deviceCode;
    private String deviceName;
    private Double longitude;
    private Double latitude;
    private OffsetDateTime timestamp;
    private Double speed; // Optional: km/h
    private Double heading; // Optional: degrees
}

