package com.example.gis.service;

import com.example.gis.dto.IsochroneRequest;
import com.example.gis.dto.IsochroneResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class IsochroneService {
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${isochrone.provider:internal}")
    private String isochroneProvider; // internal, osrm, graphhopper
    
    @Value("${osrm.url:http://router.project-osrm.org}")
    private String osrmUrl;
    
    @Value("${graphhopper.url:http://localhost:8989}")
    private String graphhopperUrl;
    
    /**
     * Calculate isochrone from a point
     * This is a stub implementation that can be extended with OSRM/GraphHopper integration
     */
    public IsochroneResponse calculateIsochrone(IsochroneRequest request) {
        if (request.getLongitude() == null || request.getLatitude() == null) {
            throw new IllegalArgumentException("Longitude and latitude are required");
        }
        
        if (request.getContours() == null || request.getContours().isEmpty()) {
            // Default contours: 5, 10, 15 minutes
            request.setContours(List.of(300, 600, 900));
        }
        
        log.info("Calculating isochrone for point ({}, {}) with contours: {}", 
                request.getLongitude(), request.getLatitude(), request.getContours());
        
        // Route to appropriate provider
        switch (isochroneProvider.toLowerCase()) {
            case "osrm":
                return calculateIsochroneOSRM(request);
            case "graphhopper":
                return calculateIsochroneGraphHopper(request);
            case "internal":
            default:
                return calculateIsochroneInternal(request);
        }
    }
    
    /**
     * Internal isochrone calculation (stub - returns simple buffer)
     */
    private IsochroneResponse calculateIsochroneInternal(IsochroneRequest request) {
        // Stub implementation: return simple circular buffers
        // In production, this would use a routing engine or spatial analysis
        
        List<IsochroneResponse.IsochroneContour> contours = new ArrayList<>();
        
        for (Integer contourValue : request.getContours()) {
            // Simple approximation: assume average speed based on profile
            double averageSpeedMps = getAverageSpeedMps(request.getProfile());
            double radiusMeters = averageSpeedMps * contourValue;
            
            // Create a simple circular buffer (simplified - in production use proper routing)
            String geometry = createCircularBufferGeoJson(
                    request.getLongitude(), 
                    request.getLatitude(), 
                    radiusMeters
            );
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("value", contourValue);
            properties.put("radius_meters", radiusMeters);
            properties.put("profile", request.getProfile());
            
            contours.add(IsochroneResponse.IsochroneContour.builder()
                    .value(contourValue)
                    .geometry(geometry)
                    .properties(properties)
                    .build());
        }
        
        return IsochroneResponse.builder()
                .longitude(request.getLongitude())
                .latitude(request.getLatitude())
                .contours(contours)
                .build();
    }
    
    /**
     * Calculate isochrone using OSRM
     */
    private IsochroneResponse calculateIsochroneOSRM(IsochroneRequest request) {
        try {
            // OSRM Isochrone API endpoint
            String url = String.format("%s/route/v1/%s/%f,%f?contours=%s&denoise=%s&generalize=%f",
                    osrmUrl,
                    request.getProfile(),
                    request.getLongitude(),
                    request.getLatitude(),
                    String.join(",", request.getContours().stream().map(String::valueOf).toList()),
                    request.getDenoise(),
                    request.getGeneralize());
            
            log.debug("Calling OSRM isochrone API: {}", url);
            
            // Call OSRM API (stub - would need proper response parsing)
            // Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            // For now, fall back to internal calculation
            log.warn("OSRM integration not fully implemented, using internal calculation");
            return calculateIsochroneInternal(request);
            
        } catch (Exception e) {
            log.error("Error calling OSRM isochrone API: {}", e.getMessage(), e);
            return calculateIsochroneInternal(request);
        }
    }
    
    /**
     * Calculate isochrone using GraphHopper
     */
    private IsochroneResponse calculateIsochroneGraphHopper(IsochroneRequest request) {
        try {
            // GraphHopper Isochrone API endpoint
            String url = String.format("%s/isochrone?point=%f,%f&profile=%s&time_limit=%s",
                    graphhopperUrl,
                    request.getLatitude(), // GraphHopper uses lat,lng
                    request.getLongitude(),
                    request.getProfile(),
                    String.join(",", request.getContours().stream().map(String::valueOf).toList()));
            
            log.debug("Calling GraphHopper isochrone API: {}", url);
            
            // Call GraphHopper API (stub - would need proper response parsing)
            // Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            // For now, fall back to internal calculation
            log.warn("GraphHopper integration not fully implemented, using internal calculation");
            return calculateIsochroneInternal(request);
            
        } catch (Exception e) {
            log.error("Error calling GraphHopper isochrone API: {}", e.getMessage(), e);
            return calculateIsochroneInternal(request);
        }
    }
    
    private double getAverageSpeedMps(String profile) {
        // Average speeds in meters per second
        return switch (profile.toLowerCase()) {
            case "walking" -> 1.4; // ~5 km/h
            case "cycling" -> 4.2; // ~15 km/h
            case "driving" -> 13.9; // ~50 km/h
            default -> 13.9; // Default to driving
        };
    }
    
    private String createCircularBufferGeoJson(double lng, double lat, double radiusMeters) {
        // Create a simple circular buffer (simplified - in production use proper geometry library)
        // This is a stub that creates a basic polygon approximation
        int segments = 32; // Number of segments for circle approximation
        double radiusDegrees = radiusMeters / 111000.0; // Rough conversion: 1 degree ≈ 111km
        
        StringBuilder coords = new StringBuilder("[");
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double offsetLng = radiusDegrees * Math.cos(angle);
            double offsetLat = radiusDegrees * Math.sin(angle);
            
            if (i > 0) coords.append(",");
            coords.append(String.format("[%.6f,%.6f]", lng + offsetLng, lat + offsetLat));
        }
        coords.append("]");
        
        return String.format("{\"type\":\"Polygon\",\"coordinates\":[[%s]]}", coords);
    }
}

