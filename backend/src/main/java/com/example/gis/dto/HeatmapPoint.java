package com.example.gis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapPoint {
    private Double longitude;
    private Double latitude;
    private Double intensity; // Heat intensity value (0.0 to 1.0)
    private Integer gridX; // Grid cell X coordinate
    private Integer gridY; // Grid cell Y coordinate
}

