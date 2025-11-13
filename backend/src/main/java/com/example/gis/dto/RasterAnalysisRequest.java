package com.example.gis.dto;

import lombok.Data;

import java.util.List;

@Data
public class RasterAnalysisRequest {
    private String rasterSource; // Raster data source identifier
    private String analysisType; // ELEVATION, SLOPE, ASPECT, HILLSHADE, etc.
    private Double minLng; // Bounding box
    private Double minLat;
    private Double maxLng;
    private Double maxLat;
    private Integer resolution = 256; // Output resolution
    private List<String> operations; // Additional operations (e.g., ["normalize", "smooth"])
}

