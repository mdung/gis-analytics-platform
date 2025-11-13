package com.example.gis.controller;

import com.example.gis.dto.RasterAnalysisRequest;
import com.example.gis.dto.RasterAnalysisResponse;
import com.example.gis.service.RasterAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/raster")
@Tag(name = "Raster Analysis", description = "Raster analysis APIs (stub implementation)")
@RequiredArgsConstructor
public class RasterAnalysisController {
    private final RasterAnalysisService rasterAnalysisService;

    @PostMapping("/analyze")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Analyze raster", description = "Perform raster analysis (elevation, slope, aspect, hillshade) - stub implementation")
    public ResponseEntity<RasterAnalysisResponse> analyzeRaster(@Valid @RequestBody RasterAnalysisRequest request) {
        return ResponseEntity.ok(rasterAnalysisService.analyzeRaster(request));
    }

    @GetMapping("/analyze")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Analyze raster (GET)", description = "Perform raster analysis using query parameters")
    public ResponseEntity<RasterAnalysisResponse> analyzeRasterGet(
            @RequestParam String rasterSource,
            @RequestParam String analysisType,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLng,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false, defaultValue = "256") Integer resolution) {
        
        RasterAnalysisRequest request = new RasterAnalysisRequest();
        request.setRasterSource(rasterSource);
        request.setAnalysisType(analysisType);
        request.setMinLng(minLng);
        request.setMinLat(minLat);
        request.setMaxLng(maxLng);
        request.setMaxLat(maxLat);
        request.setResolution(resolution);
        
        return ResponseEntity.ok(rasterAnalysisService.analyzeRaster(request));
    }
}

