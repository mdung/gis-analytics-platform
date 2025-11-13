package com.example.gis.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ClusterRequest {
    private UUID layerId;
    private Integer zoom; // Map zoom level (typically 0-18)
    private Double minLng; // Bounding box
    private Double minLat;
    private Double maxLng;
    private Double maxLat;
    private Integer clusterRadius; // Optional: pixels, default based on zoom
}

