package com.example.gis.controller;

import com.example.gis.entity.Device;
import com.example.gis.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
@Tag(name = "Devices", description = "Device tracking APIs")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;

    @GetMapping
    @Operation(summary = "List devices", description = "Get all devices")
    public ResponseEntity<List<Device>> findAll() {
        return ResponseEntity.ok(deviceService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get device by ID")
    public ResponseEntity<Device> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(deviceService.findById(id));
    }

    @PostMapping("/position")
    @Operation(summary = "Update device position", description = "Update device position and broadcast via WebSocket (for tracking)")
    public ResponseEntity<Device> updatePosition(@RequestBody Map<String, Object> request) {
        String code = (String) request.get("code");
        List<Number> coordinates = (List<Number>) request.get("coordinates");
        double lng = coordinates.get(0).doubleValue();
        double lat = coordinates.get(1).doubleValue();
        
        Device device = deviceService.updatePosition(code, lng, lat);
        return ResponseEntity.ok(device);
    }
}

