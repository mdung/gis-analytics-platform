package com.example.gis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureDto {
    private UUID id;
    private UUID layerId;
    private Map<String, Object> properties;
    private Object geometry; // GeoJSON geometry
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Double distanceMeters; // For nearest neighbor queries
}

