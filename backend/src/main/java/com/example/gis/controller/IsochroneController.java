package com.example.gis.controller;

import com.example.gis.dto.IsochroneRequest;
import com.example.gis.dto.IsochroneResponse;
import com.example.gis.service.IsochroneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/isochrone")
@Tag(name = "Isochrone", description = "Isochrone calculation APIs")
@RequiredArgsConstructor
public class IsochroneController {
    private final IsochroneService isochroneService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Calculate isochrone", description = "Calculate isochrone from a point (stub implementation)")
    public ResponseEntity<IsochroneResponse> calculateIsochrone(@Valid @RequestBody IsochroneRequest request) {
        return ResponseEntity.ok(isochroneService.calculateIsochrone(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Calculate isochrone (GET)", description = "Calculate isochrone using query parameters")
    public ResponseEntity<IsochroneResponse> calculateIsochroneGet(
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(required = false) String contours, // Comma-separated values
            @RequestParam(required = false, defaultValue = "driving") String profile,
            @RequestParam(required = false, defaultValue = "true") Boolean denoise,
            @RequestParam(required = false, defaultValue = "0.0") Double generalize) {
        
        IsochroneRequest request = new IsochroneRequest();
        request.setLongitude(longitude);
        request.setLatitude(latitude);
        request.setProfile(profile);
        request.setDenoise(denoise);
        request.setGeneralize(generalize);
        
        if (contours != null && !contours.isEmpty()) {
            request.setContours(java.util.Arrays.stream(contours.split(","))
                    .map(Integer::parseInt)
                    .toList());
        }
        
        return ResponseEntity.ok(isochroneService.calculateIsochrone(request));
    }
}

