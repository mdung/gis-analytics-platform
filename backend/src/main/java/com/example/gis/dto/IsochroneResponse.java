package com.example.gis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IsochroneResponse {
    private Double longitude; // Start point longitude
    private Double latitude; // Start point latitude
    private List<IsochroneContour> contours; // List of isochrone contours
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IsochroneContour {
        private Integer value; // Contour value in seconds
        private String geometry; // GeoJSON Polygon geometry
        private Map<String, Object> properties; // Additional properties
    }
}

