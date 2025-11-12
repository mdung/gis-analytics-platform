package com.example.gis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class SpatialQueryRequest {
    @NotNull
    private UUID layerId;
    
    // For buffer query
    private List<Double> center; // [lng, lat]
    private Double radiusMeters;
    
    // For within/intersect query
    private Map<String, Object> polygonGeoJson;
    
    // For spatial join
    private UUID targetLayerId;
    private String predicate; // INTERSECTS, WITHIN, CONTAINS
    
    // For attribute filter
    private Map<String, Object> attributeFilters;
    
    // Pagination
    private Integer limit = 100;
    private Integer offset = 0;
}

