package com.example.gis.service;

import com.example.gis.entity.Geofence;
import com.example.gis.entity.User;
import com.example.gis.repository.GeofenceRepository;
import com.example.gis.repository.UserRepository;
import com.example.gis.util.GeoJsonConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
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
public class GeofenceService {
    private final GeofenceRepository geofenceRepository;
    private final UserRepository userRepository;
    private final GeoJsonConverter geoJsonConverter;
    private final ObjectMapper objectMapper;

    public List<Geofence> findAll() {
        return geofenceRepository.findByActiveTrueAndDeletedAtIsNull();
    }

    public Geofence findById(UUID id) {
        return geofenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Geofence not found"));
    }

    @Transactional
    public Geofence create(String name, String description, Map<String, Object> polygonGeoJson, Boolean active) {
        Polygon polygon = (Polygon) geoJsonConverter.geoJsonToGeometry(polygonGeoJson);
        
        Geofence geofence = Geofence.builder()
                .name(name)
                .description(description)
                .geom(polygon)
                .active(active != null ? active : true)
                .createdBy(getCurrentUser())
                .build();
        
        return geofenceRepository.save(geofence);
    }

    @Transactional
    public Geofence update(UUID id, String name, String description, Map<String, Object> polygonGeoJson, Boolean active) {
        Geofence geofence = findById(id);
        
        if (name != null) geofence.setName(name);
        if (description != null) geofence.setDescription(description);
        if (polygonGeoJson != null) {
            geofence.setGeom((Polygon) geoJsonConverter.geoJsonToGeometry(polygonGeoJson));
        }
        if (active != null) geofence.setActive(active);
        geofence.setUpdatedBy(getCurrentUser());
        
        return geofenceRepository.save(geofence);
    }

    @Transactional
    public void delete(UUID id) {
        Geofence geofence = findById(id);
        geofence.setDeletedAt(java.time.OffsetDateTime.now());
        geofenceRepository.save(geofence);
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

