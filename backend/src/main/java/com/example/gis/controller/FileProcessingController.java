package com.example.gis.controller;

import com.example.gis.util.CSVParser;
import com.example.gis.util.GeometryValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/file-processing")
@Tag(name = "File Processing", description = "File processing utilities APIs")
@RequiredArgsConstructor
public class FileProcessingController {
    private final CSVParser csvParser;
    private final GeometryValidator geometryValidator;

    @PostMapping("/csv/detect-columns")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Detect CSV columns", description = "Auto-detect lat/lng columns from CSV file (ADMIN/EDITOR only)")
    public ResponseEntity<Map<String, Object>> detectCSVColumns(@RequestParam("file") MultipartFile file) {
        try {
            CSVParser.ColumnDetectionResult result = csvParser.detectLatLngColumns(file.getInputStream());
            
            Map<String, Object> response = new HashMap<>();
            response.put("detected", result.isDetected());
            response.put("latColumn", result.getLatColumn());
            response.put("lngColumn", result.getLngColumn());
            response.put("allColumns", result.getAllColumns());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/geometry/validate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Validate geometry", description = "Validate a geometry (ADMIN/EDITOR only)")
    public ResponseEntity<Map<String, Object>> validateGeometry(@RequestBody Map<String, Object> geometryJson) {
        try {
            // This would need to parse the geometry from JSON
            // For now, return a placeholder
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Geometry validation endpoint - implementation needed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

