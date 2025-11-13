package com.example.gis.controller;

import com.example.gis.dto.ExportRequest;
import com.example.gis.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/export")
@Tag(name = "Export", description = "Data export APIs")
@RequiredArgsConstructor
public class ExportController {
    private final ExportService exportService;
    private final com.example.gis.service.MapSnapshotService mapSnapshotService;

    @PostMapping("/geojson")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Export as GeoJSON", description = "Export features as GeoJSON FeatureCollection with optional filters")
    public ResponseEntity<byte[]> exportGeoJSON(@Valid @RequestBody ExportRequest request) {
        try {
            byte[] geojson = exportService.exportGeoJSON(request);
            
            String filename = String.format("export_%s.geojson",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(geojson);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/csv")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Export as CSV", description = "Export features as CSV with coordinates and properties")
    public ResponseEntity<byte[]> exportCSV(@Valid @RequestBody ExportRequest request) {
        try {
            byte[] csv = exportService.exportCSV(request);
            
            String filename = String.format("export_%s.csv",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/geojson")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Export as GeoJSON (GET)", description = "Export features as GeoJSON using query parameters")
    public ResponseEntity<byte[]> exportGeoJSONGet(
            @RequestParam java.util.UUID layerId,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLng,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Integer limit) {
        try {
            ExportRequest request = new ExportRequest();
            request.setLayerId(layerId);
            request.setMinLng(minLng);
            request.setMinLat(minLat);
            request.setMaxLng(maxLng);
            request.setMaxLat(maxLat);
            request.setLimit(limit);
            
            byte[] geojson = exportService.exportGeoJSON(request);
            
            String filename = String.format("export_%s.geojson",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(geojson);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/csv")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Export as CSV (GET)", description = "Export features as CSV using query parameters")
    public ResponseEntity<byte[]> exportCSVGet(
            @RequestParam java.util.UUID layerId,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLng,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Integer limit) {
        try {
            ExportRequest request = new ExportRequest();
            request.setLayerId(layerId);
            request.setMinLng(minLng);
            request.setMinLat(minLat);
            request.setMaxLng(maxLng);
            request.setMaxLat(maxLat);
            request.setLimit(limit);
            
            byte[] csv = exportService.exportCSV(request);
            
            String filename = String.format("export_%s.csv",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/png")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Export map snapshot as PNG", description = "Generate a map snapshot image (basic implementation)")
    public ResponseEntity<byte[]> exportMapSnapshot(
            @Valid @RequestBody ExportRequest request,
            @RequestParam(defaultValue = "800") int width,
            @RequestParam(defaultValue = "600") int height) {
        try {
            byte[] png = mapSnapshotService.generateMapSnapshot(request, width, height);
            
            String filename = String.format("map_snapshot_%s.png",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(png);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

