package com.example.gis.service;

import com.example.gis.dto.FeatureDto;
import com.example.gis.dto.SpatialQueryRequest;
import com.example.gis.entity.Feature;
import com.example.gis.repository.FeatureRepository;
import com.example.gis.util.GeoJsonConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpatialQueryService {
    private final FeatureRepository featureRepository;
    private final GeoJsonConverter geoJsonConverter;
    private final ObjectMapper objectMapper;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public List<FeatureDto> bufferQuery(SpatialQueryRequest request) {
        if (request.getCenter() == null || request.getRadiusMeters() == null) {
            throw new IllegalArgumentException("Center and radiusMeters are required for buffer query");
        }
        
        Point center = geometryFactory.createPoint(
                new org.locationtech.jts.geom.Coordinate(
                        request.getCenter().get(0),
                        request.getCenter().get(1)
                )
        );
        
        List<Feature> features = featureRepository.findFeaturesInBuffer(
                request.getLayerId(),
                center,
                request.getRadiusMeters()
        );
        
        return features.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<FeatureDto> withinQuery(SpatialQueryRequest request) {
        if (request.getPolygonGeoJson() == null) {
            throw new IllegalArgumentException("Polygon GeoJSON is required for within query");
        }
        
        Geometry polygon = geoJsonConverter.geoJsonToGeometry(request.getPolygonGeoJson());
        List<Feature> features = featureRepository.findFeaturesWithin(
                request.getLayerId(),
                polygon
        );
        
        return features.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<FeatureDto> intersectQuery(SpatialQueryRequest request) {
        if (request.getPolygonGeoJson() == null) {
            throw new IllegalArgumentException("Geometry GeoJSON is required for intersect query");
        }
        
        Geometry geometry = geoJsonConverter.geoJsonToGeometry(request.getPolygonGeoJson());
        List<Feature> features = featureRepository.findIntersectingFeatures(
                request.getLayerId(),
                geometry
        );
        
        return features.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<FeatureDto> nearestQuery(UUID layerId, double lng, double lat, int k) {
        Point point = geometryFactory.createPoint(
                new org.locationtech.jts.geom.Coordinate(lng, lat)
        );
        
        List<Object[]> results = featureRepository.findNearestFeatures(layerId, point, k);
        
        return results.stream()
                .map(result -> {
                    Feature feature = (Feature) result[0];
                    Double distance = result.length > 1 ? ((Number) result[1]).doubleValue() : null;
                    FeatureDto dto = toDto(feature);
                    if (distance != null) {
                        dto.setDistanceMeters(distance);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<FeatureDto> spatialJoin(SpatialQueryRequest request) {
        if (request.getTargetLayerId() == null || request.getPredicate() == null) {
            throw new IllegalArgumentException("Target layer ID and predicate are required for spatial join");
        }
        
        // Get all features from source layer
        List<Feature> sourceFeatures = featureRepository.findByLayerIdAndDeletedAtIsNull(
                request.getLayerId(),
                PageRequest.of(0, 10000)
        ).getContent();
        
        // Get all features from target layer
        List<Feature> targetFeatures = featureRepository.findByLayerIdAndDeletedAtIsNull(
                request.getTargetLayerId(),
                PageRequest.of(0, 10000)
        ).getContent();
        
        // Perform spatial join based on predicate
        return sourceFeatures.stream()
                .filter(source -> targetFeatures.stream().anyMatch(target -> {
                    switch (request.getPredicate().toUpperCase()) {
                        case "INTERSECTS":
                            return source.getGeom().intersects(target.getGeom());
                        case "WITHIN":
                            return source.getGeom().within(target.getGeom());
                        case "CONTAINS":
                            return source.getGeom().contains(target.getGeom());
                        default:
                            return false;
                    }
                }))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private FeatureDto toDto(Feature feature) {
        try {
            return FeatureDto.builder()
                    .id(feature.getId())
                    .layerId(feature.getLayer().getId())
                    .properties(objectMapper.readValue(feature.getProperties(), Map.class))
                    .geometry(geoJsonConverter.geometryToGeoJson(feature.getGeom()))
                    .createdAt(feature.getCreatedAt())
                    .updatedAt(feature.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error converting feature to DTO", e);
        }
    }
}

