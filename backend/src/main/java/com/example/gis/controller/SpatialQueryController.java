package com.example.gis.controller;

import com.example.gis.dto.FeatureDto;
import com.example.gis.dto.SpatialQueryRequest;
import com.example.gis.service.SpatialQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/query")
@Tag(name = "Spatial Queries", description = "Advanced spatial query APIs")
@RequiredArgsConstructor
public class SpatialQueryController {
    private final SpatialQueryService spatialQueryService;
    private final ObjectMapper objectMapper;

    @PostMapping("/buffer")
    @Operation(summary = "Buffer query", description = "Find features within a buffer radius from a center point")
    public ResponseEntity<List<FeatureDto>> bufferQuery(@Valid @RequestBody SpatialQueryRequest request) {
        return ResponseEntity.ok(spatialQueryService.bufferQuery(request));
    }

    @PostMapping("/within")
    @Operation(summary = "Within query", description = "Find features within a polygon")
    public ResponseEntity<List<FeatureDto>> withinQuery(@Valid @RequestBody SpatialQueryRequest request) {
        return ResponseEntity.ok(spatialQueryService.withinQuery(request));
    }

    @PostMapping("/intersect")
    @Operation(summary = "Intersect query", description = "Find features that intersect with a geometry")
    public ResponseEntity<List<FeatureDto>> intersectQuery(@Valid @RequestBody SpatialQueryRequest request) {
        return ResponseEntity.ok(spatialQueryService.intersectQuery(request));
    }

    @GetMapping("/nearest")
    @Operation(summary = "Nearest neighbor query", description = "Find K nearest features to a point")
    public ResponseEntity<List<FeatureDto>> nearestQuery(
            @RequestParam UUID layerId,
            @RequestParam double lng,
            @RequestParam double lat,
            @RequestParam(defaultValue = "5") int k) {
        return ResponseEntity.ok(spatialQueryService.nearestQuery(layerId, lng, lat, k));
    }

    @PostMapping("/spatial-join")
    @Operation(summary = "Spatial join", description = "Perform spatial join between two layers")
    public ResponseEntity<List<FeatureDto>> spatialJoin(@Valid @RequestBody SpatialQueryRequest request) {
        return ResponseEntity.ok(spatialQueryService.spatialJoin(request));
    }

    @PostMapping("/touches")
    @Operation(summary = "Touches query", description = "Find features that touch the given geometry")
    public ResponseEntity<List<FeatureDto>> touchesQuery(@Valid @RequestBody SpatialQueryRequest request) {
        return ResponseEntity.ok(spatialQueryService.touchesQuery(request));
    }

    @PostMapping("/overlaps")
    @Operation(summary = "Overlaps query", description = "Find features that overlap with the given geometry")
    public ResponseEntity<List<FeatureDto>> overlapsQuery(@Valid @RequestBody SpatialQueryRequest request) {
        return ResponseEntity.ok(spatialQueryService.overlapsQuery(request));
    }

    @PostMapping("/distance")
    @Operation(summary = "Distance query", description = "Find features within a distance using custom metric (HAVERSINE or PLANAR)")
    public ResponseEntity<List<FeatureDto>> distanceQuery(@Valid @RequestBody SpatialQueryRequest request) {
        return ResponseEntity.ok(spatialQueryService.distanceQuery(request));
    }

    @GetMapping("/geojson")
    @Operation(summary = "Export as GeoJSON", description = "Export query results as GeoJSON FeatureCollection")
    public ResponseEntity<ObjectNode> exportGeoJson(@RequestParam UUID layerId) {
        // This would typically use the feature service to get all features and convert to FeatureCollection
        ObjectNode featureCollection = objectMapper.createObjectNode();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.set("features", objectMapper.createArrayNode());
        return ResponseEntity.ok(featureCollection);
    }
}

