package com.example.gis.service;

import com.example.gis.dto.FeatureDto;
import com.example.gis.entity.Feature;
import com.example.gis.entity.Layer;
import com.example.gis.entity.User;
import com.example.gis.repository.FeatureRepository;
import com.example.gis.repository.LayerRepository;
import com.example.gis.repository.UserRepository;
import com.example.gis.service.CacheService;
import com.example.gis.util.GeoJsonConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeatureService {
    private final FeatureRepository featureRepository;
    private final LayerRepository layerRepository;
    private final UserRepository userRepository;
    private final GeoJsonConverter geoJsonConverter;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    public Page<FeatureDto> findByLayerId(UUID layerId, Pageable pageable) {
        return featureRepository.findByLayerIdAndDeletedAtIsNull(layerId, pageable)
                .map(this::toDto);
    }

    public List<FeatureDto> findByLayerIdAndBbox(UUID layerId, double minLng, double minLat, double maxLng, double maxLat) {
        // Try cache first
        List<FeatureDto> cached = cacheService.getCachedFeatureBbox(
                layerId, minLng, minLat, maxLng, maxLat, FeatureDto.class);
        if (cached != null) {
            return cached;
        }

        // Query database
        List<FeatureDto> result = featureRepository.findFeaturesInBbox(layerId, minLng, minLat, maxLng, maxLat)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        // Cache result
        cacheService.cacheFeatureBbox(layerId, minLng, minLat, maxLng, maxLat, result);

        return result;
    }

    public FeatureDto findById(UUID id) {
        Feature feature = featureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feature not found"));
        return toDto(feature);
    }

    @Transactional
    public FeatureDto create(FeatureDto dto) {
        Layer layer = layerRepository.findById(dto.getLayerId())
                .orElseThrow(() -> new RuntimeException("Layer not found"));
        
        Map<String, Object> geoJson = (Map<String, Object>) dto.getGeometry();
        Geometry geom = geoJsonConverter.geoJsonToGeometry(geoJson);
        
        Feature feature = Feature.builder()
                .layer(layer)
                .properties(objectMapper.valueToTree(dto.getProperties()).toString())
                .geom(geom)
                .createdBy(getCurrentUser())
                .build();
        
        return toDto(featureRepository.save(feature));
    }

    @Transactional
    public FeatureDto update(UUID id, FeatureDto dto) {
        Feature feature = featureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feature not found"));
        
        if (dto.getProperties() != null) {
            feature.setProperties(objectMapper.valueToTree(dto.getProperties()).toString());
        }
        if (dto.getGeometry() != null) {
            Map<String, Object> geoJson = (Map<String, Object>) dto.getGeometry();
            feature.setGeom(geoJsonConverter.geoJsonToGeometry(geoJson));
        }
        feature.setUpdatedBy(getCurrentUser());
        
        return toDto(featureRepository.save(feature));
    }

    @Transactional
    public void delete(UUID id) {
        Feature feature = featureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feature not found"));
        feature.setDeletedAt(java.time.OffsetDateTime.now());
        featureRepository.save(feature);
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

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return userRepository.findByUsernameAndDeletedAtIsNull(auth.getName())
                    .orElse(null);
        }
        return null;
    }
}

