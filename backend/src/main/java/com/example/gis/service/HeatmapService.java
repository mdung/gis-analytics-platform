package com.example.gis.service;

import com.example.gis.dto.HeatmapPoint;
import com.example.gis.dto.HeatmapRequest;
import com.example.gis.entity.Feature;
import com.example.gis.repository.FeatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeatmapService {
    private final FeatureRepository featureRepository;

    /**
     * Generate heatmap data from point features
     */
    public List<HeatmapPoint> generateHeatmap(HeatmapRequest request) {
        if (request.getLayerId() == null) {
            throw new IllegalArgumentException("Layer ID is required");
        }

        // Get features in bounding box
        List<Feature> features;
        if (request.getMinLng() != null && request.getMinLat() != null &&
            request.getMaxLng() != null && request.getMaxLat() != null) {
            features = featureRepository.findFeaturesInBbox(
                    request.getLayerId(),
                    request.getMinLng(),
                    request.getMinLat(),
                    request.getMaxLng(),
                    request.getMaxLat()
            );
        } else {
            features = featureRepository.findByLayerIdAndDeletedAtIsNull(
                    request.getLayerId(),
                    org.springframework.data.domain.PageRequest.of(0, 10000)
            ).getContent();
        }

        // Filter to only Point geometries
        List<Point> points = features.stream()
                .filter(f -> f.getGeom() instanceof Point)
                .map(f -> (Point) f.getGeom())
                .collect(Collectors.toList());

        if (points.isEmpty()) {
            return Collections.emptyList();
        }

        // Generate cache key
        String cacheKey = cacheService.generateSpatialQueryKey(
                request.getLayerId(),
                "heatmap",
                request.getZoom(),
                request.getMinLng(), request.getMinLat(),
                request.getMaxLng(), request.getMaxLat(),
                request.getGridSize(),
                request.getRadius(),
                request.getIntensity()
        );

        // Try cache first
        List<HeatmapPoint> cached = cacheService.getCachedHeatmap(cacheKey, HeatmapPoint.class);
        if (cached != null) {
            return cached;
        }

        // Calculate grid cell size
        double cellSizeLng = (request.getMaxLng() - request.getMinLng()) / request.getGridSize();
        double cellSizeLat = (request.getMaxLat() - request.getMinLat()) / request.getGridSize();

        // Calculate heat radius in degrees
        double radiusDegrees = calculateRadiusInDegrees(
                request.getRadius(),
                request.getZoom() != null ? request.getZoom() : 12,
                request.getMinLng(), request.getMinLat(),
                request.getMaxLng(), request.getMaxLat()
        );

        // Create intensity grid
        double[][] intensityGrid = new double[request.getGridSize()][request.getGridSize()];

        // Calculate intensity for each point
        for (Point point : points) {
            double lng = point.getX();
            double lat = point.getY();

            // Find grid cells affected by this point
            int centerX = (int) ((lng - request.getMinLng()) / cellSizeLng);
            int centerY = (int) ((lat - request.getMinLat()) / cellSizeLat);

            // Apply heat to surrounding cells
            int radiusCells = (int) Math.ceil(radiusDegrees / Math.max(cellSizeLng, cellSizeLat));

            for (int dx = -radiusCells; dx <= radiusCells; dx++) {
                for (int dy = -radiusCells; dy <= radiusCells; dy++) {
                    int gridX = centerX + dx;
                    int gridY = centerY + dy;

                    if (gridX >= 0 && gridX < request.getGridSize() &&
                        gridY >= 0 && gridY < request.getGridSize()) {

                        // Calculate distance from point to grid cell center
                        double cellCenterLng = request.getMinLng() + (gridX + 0.5) * cellSizeLng;
                        double cellCenterLat = request.getMinLat() + (gridY + 0.5) * cellSizeLat;

                        double distance = calculateDistance(
                                lng, lat,
                                cellCenterLng, cellCenterLat
                        );

                        if (distance <= radiusDegrees) {
                            // Apply Gaussian-like falloff
                            double intensity = Math.exp(-(distance * distance) / (2 * radiusDegrees * radiusDegrees / 4));
                            intensityGrid[gridY][gridX] += intensity * request.getIntensity();
                        }
                    }
                }
            }
        }

        // Normalize intensities (0.0 to 1.0)
        double maxIntensity = 0;
        for (double[] row : intensityGrid) {
            for (double intensity : row) {
                maxIntensity = Math.max(maxIntensity, intensity);
            }
        }

        // Convert grid to heatmap points
        List<HeatmapPoint> heatmapPoints = new ArrayList<>();
        for (int y = 0; y < request.getGridSize(); y++) {
            for (int x = 0; x < request.getGridSize(); x++) {
                double intensity = maxIntensity > 0 ? intensityGrid[y][x] / maxIntensity : 0;

                // Only include cells with significant intensity
                if (intensity > 0.01) {
                    double cellLng = request.getMinLng() + (x + 0.5) * cellSizeLng;
                    double cellLat = request.getMinLat() + (y + 0.5) * cellSizeLat;

                    heatmapPoints.add(HeatmapPoint.builder()
                            .longitude(cellLng)
                            .latitude(cellLat)
                            .intensity(intensity)
                            .gridX(x)
                            .gridY(y)
                            .build());
                }
            }
        }

        // Cache result
        cacheService.cacheHeatmap(cacheKey, heatmapPoints, null);

        return heatmapPoints;
    }

    /**
     * Calculate radius in degrees based on pixel radius and zoom level
     */
    private double calculateRadiusInDegrees(double radiusPixels, int zoom,
                                            double minLng, double minLat,
                                            double maxLng, double maxLat) {
        // Calculate degrees per pixel at this zoom level
        double degreesPerPixelLng = (maxLng - minLng) / 256.0; // Assuming 256px tile width
        double degreesPerPixelLat = (maxLat - minLat) / 256.0; // Assuming 256px tile height

        // Use average for radius calculation
        double avgDegreesPerPixel = (degreesPerPixelLng + degreesPerPixelLat) / 2.0;

        return radiusPixels * avgDegreesPerPixel;
    }

    /**
     * Calculate distance between two points in degrees (simplified)
     */
    private double calculateDistance(double lng1, double lat1, double lng2, double lat2) {
        double dx = lng2 - lng1;
        double dy = lat2 - lat1;
        return Math.sqrt(dx * dx + dy * dy);
    }
}

