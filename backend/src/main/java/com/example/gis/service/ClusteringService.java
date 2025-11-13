package com.example.gis.service;

import com.example.gis.dto.ClusterPoint;
import com.example.gis.dto.ClusterRequest;
import com.example.gis.entity.Feature;
import com.example.gis.repository.FeatureRepository;
import com.example.gis.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClusteringService {
    private final FeatureRepository featureRepository;
    private final CacheService cacheService;

    /**
     * Cluster features based on zoom level and bounding box
     */
    public List<ClusterPoint> clusterFeatures(ClusterRequest request) {
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
            // If no bbox, get all features (with limit)
            features = featureRepository.findByLayerIdAndDeletedAtIsNull(
                    request.getLayerId(),
                    org.springframework.data.domain.PageRequest.of(0, 10000)
            ).getContent();
        }

        if (features.isEmpty()) {
            return Collections.emptyList();
        }

        // Filter to only Point geometries for clustering
        List<Feature> pointFeatures = features.stream()
                .filter(f -> f.getGeom() instanceof Point)
                .collect(Collectors.toList());

        if (pointFeatures.isEmpty()) {
            return Collections.emptyList();
        }

        int zoom = request.getZoom() != null ? request.getZoom() : 12;
        int clusterRadius = request.getClusterRadius() != null ? 
                request.getClusterRadius() : calculateClusterRadius(zoom);

        // Generate cache key
        String cacheKey = cacheService.generateSpatialQueryKey(
                request.getLayerId(),
                "cluster",
                zoom,
                request.getMinLng(), request.getMinLat(),
                request.getMaxLng(), request.getMaxLat(),
                clusterRadius
        );

        // Try cache first
        List<ClusterPoint> cached = cacheService.getCachedCluster(cacheKey, ClusterPoint.class);
        if (cached != null) {
            return cached;
        }

        List<ClusterPoint> result = performClustering(pointFeatures, clusterRadius, request);

        // Cache result
        cacheService.cacheCluster(cacheKey, result, null);

        return result;
    }

    /**
     * Calculate cluster radius in degrees based on zoom level
     * Higher zoom = smaller radius (more clusters)
     * Lower zoom = larger radius (fewer clusters)
     */
    private int calculateClusterRadius(int zoom) {
        // Base radius in degrees (approximate)
        // At zoom 0: ~180 degrees (entire world)
        // At zoom 18: ~0.001 degrees (~100 meters)
        double baseRadius = 180.0 / Math.pow(2, zoom);
        // Convert to a reasonable pixel-based radius
        // Assuming ~256 pixels per tile, and we want ~50-100 pixel clusters
        return (int) (baseRadius * 0.1); // Adjust multiplier as needed
    }

    /**
     * Perform grid-based clustering
     */
    private List<ClusterPoint> performClustering(
            List<Feature> features, 
            int clusterRadius, 
            ClusterRequest request) {
        
        // Convert cluster radius from micro-degrees to degrees
        double cellSize = clusterRadius / 1000000.0; // Convert from micro-degrees
        
        // Create grid-based clusters
        Map<String, List<Feature>> gridClusters = new HashMap<>();
        
        for (Feature feature : features) {
            Point point = (Point) feature.getGeom();
            double lng = point.getX();
            double lat = point.getY();
            
            // Calculate grid cell key
            int gridX = (int) Math.floor(lng / cellSize);
            int gridY = (int) Math.floor(lat / cellSize);
            String cellKey = gridX + "," + gridY;
            
            gridClusters.computeIfAbsent(cellKey, k -> new ArrayList<>()).add(feature);
        }
        
        // Convert grid clusters to ClusterPoints
        List<ClusterPoint> clusters = new ArrayList<>();
        
        for (Map.Entry<String, List<Feature>> entry : gridClusters.entrySet()) {
            List<Feature> clusterFeatures = entry.getValue();
            
            if (clusterFeatures.size() == 1) {
                // Single point (not a cluster)
                Feature feature = clusterFeatures.get(0);
                Point point = (Point) feature.getGeom();
                
                clusters.add(ClusterPoint.builder()
                        .longitude(point.getX())
                        .latitude(point.getY())
                        .pointCount(1)
                        .isCluster(false)
                        .featureIds(Collections.singletonList(feature.getId()))
                        .bounds(ClusterPoint.ClusterBounds.builder()
                                .minLng(point.getX())
                                .minLat(point.getY())
                                .maxLng(point.getX())
                                .maxLat(point.getY())
                                .build())
                        .build());
            } else {
                // Cluster of multiple points
                double sumLng = 0;
                double sumLat = 0;
                double minLng = Double.MAX_VALUE;
                double minLat = Double.MAX_VALUE;
                double maxLng = Double.MIN_VALUE;
                double maxLat = Double.MIN_VALUE;
                List<UUID> featureIds = new ArrayList<>();
                
                for (Feature feature : clusterFeatures) {
                    Point point = (Point) feature.getGeom();
                    double lng = point.getX();
                    double lat = point.getY();
                    
                    sumLng += lng;
                    sumLat += lat;
                    minLng = Math.min(minLng, lng);
                    minLat = Math.min(minLat, lat);
                    maxLng = Math.max(maxLng, lng);
                    maxLat = Math.max(maxLat, lat);
                    
                    featureIds.add(feature.getId());
                }
                
                // Calculate centroid
                double centroidLng = sumLng / clusterFeatures.size();
                double centroidLat = sumLat / clusterFeatures.size();
                
                clusters.add(ClusterPoint.builder()
                        .longitude(centroidLng)
                        .latitude(centroidLat)
                        .pointCount(clusterFeatures.size())
                        .isCluster(true)
                        .featureIds(featureIds.size() <= 100 ? featureIds : null) // Limit IDs for performance
                        .bounds(ClusterPoint.ClusterBounds.builder()
                                .minLng(minLng)
                                .minLat(minLat)
                                .maxLng(maxLng)
                                .maxLat(maxLat)
                                .build())
                        .build());
            }
        }
        
        return clusters;
    }

    /**
     * Alternative: Distance-based clustering using K-means-like approach
     * This is more accurate but slower for large datasets
     */
    public List<ClusterPoint> clusterFeaturesByDistance(
            List<Feature> features, 
            double maxDistance) {
        
        List<ClusterPoint> clusters = new ArrayList<>();
        Set<Feature> processed = new HashSet<>();
        
        for (Feature feature : features) {
            if (processed.contains(feature)) {
                continue;
            }
            
            Point point = (Point) feature.getGeom();
            List<Feature> clusterFeatures = new ArrayList<>();
            clusterFeatures.add(feature);
            processed.add(feature);
            
            // Find nearby features
            for (Feature other : features) {
                if (processed.contains(other)) {
                    continue;
                }
                
                Point otherPoint = (Point) other.getGeom();
                double distance = calculateDistance(
                        point.getX(), point.getY(),
                        otherPoint.getX(), otherPoint.getY()
                );
                
                if (distance <= maxDistance) {
                    clusterFeatures.add(other);
                    processed.add(other);
                }
            }
            
            // Create cluster point
            if (clusterFeatures.size() == 1) {
                Point p = (Point) clusterFeatures.get(0).getGeom();
                clusters.add(ClusterPoint.builder()
                        .longitude(p.getX())
                        .latitude(p.getY())
                        .pointCount(1)
                        .isCluster(false)
                        .featureIds(Collections.singletonList(clusterFeatures.get(0).getId()))
                        .build());
            } else {
                // Calculate centroid
                double sumLng = 0;
                double sumLat = 0;
                double minLng = Double.MAX_VALUE;
                double minLat = Double.MAX_VALUE;
                double maxLng = Double.MIN_VALUE;
                double maxLat = Double.MIN_VALUE;
                List<UUID> featureIds = new ArrayList<>();
                
                for (Feature f : clusterFeatures) {
                    Point p = (Point) f.getGeom();
                    sumLng += p.getX();
                    sumLat += p.getY();
                    minLng = Math.min(minLng, p.getX());
                    minLat = Math.min(minLat, p.getY());
                    maxLng = Math.max(maxLng, p.getX());
                    maxLat = Math.max(maxLat, p.getY());
                    featureIds.add(f.getId());
                }
                
                clusters.add(ClusterPoint.builder()
                        .longitude(sumLng / clusterFeatures.size())
                        .latitude(sumLat / clusterFeatures.size())
                        .pointCount(clusterFeatures.size())
                        .isCluster(true)
                        .featureIds(featureIds.size() <= 100 ? featureIds : null)
                        .bounds(ClusterPoint.ClusterBounds.builder()
                                .minLng(minLng)
                                .minLat(minLat)
                                .maxLng(maxLng)
                                .maxLat(maxLat)
                                .build())
                        .build());
            }
        }
        
        return clusters;
    }

    /**
     * Calculate distance between two points in meters (Haversine formula)
     */
    private double calculateDistance(double lng1, double lat1, double lng2, double lat2) {
        final int R = 6371000; // Earth radius in meters
        
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}

