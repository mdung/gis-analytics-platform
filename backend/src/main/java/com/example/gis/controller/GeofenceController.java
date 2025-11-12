package com.example.gis.controller;

import com.example.gis.entity.Geofence;
import com.example.gis.service.GeofenceService;
import com.example.gis.util.GeoJsonConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/geofences")
@Tag(name = "Geofences", description = "Geofence management APIs")
@RequiredArgsConstructor
public class GeofenceController {
    private final GeofenceService geofenceService;
    private final GeoJsonConverter geoJsonConverter;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "List geofences", description = "Get all active geofences")
    public ResponseEntity<List<ObjectNode>> findAll() {
        List<ObjectNode> geofences = geofenceService.findAll().stream()
                .map(this::toGeoJson)
                .collect(Collectors.toList());
        return ResponseEntity.ok(geofences);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get geofence by ID")
    public ResponseEntity<ObjectNode> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(toGeoJson(geofenceService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Create geofence", description = "Create a new geofence (ADMIN/EDITOR only)")
    public ResponseEntity<ObjectNode> create(@RequestBody Map<String, Object> request) {
        Geofence geofence = geofenceService.create(
                (String) request.get("name"),
                (String) request.get("description"),
                (Map<String, Object>) request.get("geometry"),
                request.get("active") != null ? (Boolean) request.get("active") : true
        );
        return ResponseEntity.ok(toGeoJson(geofence));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Update geofence", description = "Update an existing geofence (ADMIN/EDITOR only)")
    public ResponseEntity<ObjectNode> update(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        Geofence geofence = geofenceService.update(
                id,
                (String) request.get("name"),
                (String) request.get("description"),
                (Map<String, Object>) request.get("geometry"),
                request.get("active") != null ? (Boolean) request.get("active") : null
        );
        return ResponseEntity.ok(toGeoJson(geofence));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete geofence", description = "Soft delete a geofence (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        geofenceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ObjectNode toGeoJson(Geofence geofence) {
        ObjectNode feature = objectMapper.createObjectNode();
        feature.put("type", "Feature");
        feature.put("id", geofence.getId().toString());
        feature.set("geometry", geoJsonConverter.geometryToGeoJson(geofence.getGeom()));
        
        ObjectNode properties = objectMapper.createObjectNode();
        properties.put("name", geofence.getName());
        if (geofence.getDescription() != null) {
            properties.put("description", geofence.getDescription());
        }
        properties.put("active", geofence.getActive());
        feature.set("properties", properties);
        
        return feature;
    }
}

