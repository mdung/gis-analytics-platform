package com.example.gis.dto;

import lombok.Data;

import java.util.List;

@Data
public class IsochroneRequest {
    private Double longitude; // Start point longitude
    private Double latitude; // Start point latitude
    private List<Integer> contours; // Contour values in seconds (e.g., [300, 600, 900] for 5, 10, 15 minutes)
    private String profile = "driving"; // Profile: driving, walking, cycling
    private Boolean denoise = true; // Denoise isochrone polygons
    private Double generalize = 0.0; // Generalization tolerance
}

