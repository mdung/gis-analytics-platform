package com.example.gis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class ExportRequest {
    @NotNull
    private UUID layerId;
    
    // Bounding box filter
    private Double minLng;
    private Double minLat;
    private Double maxLng;
    private Double maxLat;
    
    // Attribute filters (key-value pairs)
    private Map<String, Object> attributeFilters;
    
    // Spatial filter
    private Map<String, Object> geometryFilter; // GeoJSON geometry for within/intersect
    
    // Limit results
    private Integer limit;
    private Integer offset;
    
    // CSV specific
    private String latColumnName = "latitude";
    private String lngColumnName = "longitude";
    private List<String> includeColumns; // If null, include all
}

