package com.example.gis.controller;

import com.example.gis.dto.ClusterPoint;
import com.example.gis.dto.ClusterRequest;
import com.example.gis.dto.FeatureDto;
import com.example.gis.service.ClusteringService;
import com.example.gis.service.FeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/features")
@Tag(name = "Features", description = "Feature management APIs")
@RequiredArgsConstructor
public class FeatureController {
    private final FeatureService featureService;
    private final ClusteringService clusteringService;

    @GetMapping
    @Operation(summary = "List features", description = "Get paginated list of features by layer")
    public ResponseEntity<Page<FeatureDto>> findAll(
            @RequestParam UUID layerId,
            Pageable pageable) {
        return ResponseEntity.ok(featureService.findByLayerId(layerId, pageable));
    }

    @GetMapping("/bbox")
    @Operation(summary = "Get features in bounding box")
    public ResponseEntity<List<FeatureDto>> findByBbox(
            @RequestParam UUID layerId,
            @RequestParam double minLng,
            @RequestParam double minLat,
            @RequestParam double maxLng,
            @RequestParam double maxLat) {
        return ResponseEntity.ok(featureService.findByLayerIdAndBbox(
                layerId, minLng, minLat, maxLng, maxLat));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get feature by ID")
    public ResponseEntity<FeatureDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(featureService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Create feature", description = "Create a new feature (ADMIN/EDITOR only)")
    public ResponseEntity<FeatureDto> create(@Valid @RequestBody FeatureDto dto) {
        return ResponseEntity.ok(featureService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Update feature", description = "Update an existing feature (ADMIN/EDITOR only)")
    public ResponseEntity<FeatureDto> update(@PathVariable UUID id, @Valid @RequestBody FeatureDto dto) {
        return ResponseEntity.ok(featureService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Delete feature", description = "Soft delete a feature (ADMIN/EDITOR only)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        featureService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cluster")
    @Operation(summary = "Get clustered features", description = "Get features clustered by zoom level and bounding box")
    public ResponseEntity<List<ClusterPoint>> getClusteredFeatures(
            @RequestParam UUID layerId,
            @RequestParam(required = false) Integer zoom,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLng,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Integer clusterRadius) {
        
        ClusterRequest request = new ClusterRequest();
        request.setLayerId(layerId);
        request.setZoom(zoom);
        request.setMinLng(minLng);
        request.setMinLat(minLat);
        request.setMaxLng(maxLng);
        request.setMaxLat(maxLat);
        request.setClusterRadius(clusterRadius);
        
        return ResponseEntity.ok(clusteringService.clusterFeatures(request));
    }
}

