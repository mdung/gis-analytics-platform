package com.example.gis.controller;

import com.example.gis.dto.HeatmapPoint;
import com.example.gis.dto.HeatmapRequest;
import com.example.gis.service.HeatmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/heatmap")
@Tag(name = "Heatmap", description = "Heatmap generation APIs")
@RequiredArgsConstructor
public class HeatmapController {
    private final HeatmapService heatmapService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Generate heatmap", description = "Generate heatmap data from point features")
    public ResponseEntity<List<HeatmapPoint>> generateHeatmap(@RequestBody HeatmapRequest request) {
        return ResponseEntity.ok(heatmapService.generateHeatmap(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Generate heatmap (GET)", description = "Generate heatmap data using query parameters")
    public ResponseEntity<List<HeatmapPoint>> generateHeatmapGet(
            @RequestParam UUID layerId,
            @RequestParam(required = false) Integer zoom,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLng,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false, defaultValue = "256") Integer gridSize,
            @RequestParam(required = false, defaultValue = "20.0") Double radius,
            @RequestParam(required = false, defaultValue = "1.0") Double intensity) {
        
        HeatmapRequest request = new HeatmapRequest();
        request.setLayerId(layerId);
        request.setZoom(zoom);
        request.setMinLng(minLng);
        request.setMinLat(minLat);
        request.setMaxLng(maxLng);
        request.setMaxLat(maxLat);
        request.setGridSize(gridSize);
        request.setRadius(radius);
        request.setIntensity(intensity);
        
        return ResponseEntity.ok(heatmapService.generateHeatmap(request));
    }
}

