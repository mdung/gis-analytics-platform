package com.example.gis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RasterAnalysisResponse {
    private String analysisType;
    private String rasterData; // Base64 encoded raster data or URL
    private Map<String, Object> metadata; // Min, max, mean, stddev, etc.
    private String format; // PNG, GeoTIFF, etc.
    private Integer width;
    private Integer height;
    private Double minLng;
    private Double minLat;
    private Double maxLng;
    private Double maxLat;
}

