package com.example.gis.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class HeatmapRequest {
    private UUID layerId;
    private Integer zoom; // Map zoom level
    private Double minLng; // Bounding box
    private Double minLat;
    private Double maxLng;
    private Double maxLat;
    private Integer gridSize = 256; // Grid resolution (default 256x256)
    private Double radius = 20.0; // Heat radius in pixels
    private Double intensity = 1.0; // Intensity multiplier
}

