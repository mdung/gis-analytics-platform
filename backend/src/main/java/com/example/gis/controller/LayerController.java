package com.example.gis.controller;

import com.example.gis.dto.LayerDto;
import com.example.gis.service.LayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/layers")
@Tag(name = "Layers", description = "Layer management APIs")
@RequiredArgsConstructor
public class LayerController {
    private final LayerService layerService;

    @GetMapping
    @Operation(summary = "List layers", description = "Get paginated list of all layers")
    public ResponseEntity<Page<LayerDto>> findAll(Pageable pageable) {
        return ResponseEntity.ok(layerService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get layer by ID")
    public ResponseEntity<LayerDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(layerService.findById(id));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get layer by code")
    public ResponseEntity<LayerDto> findByCode(@PathVariable String code) {
        return ResponseEntity.ok(layerService.findByCode(code));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Create layer", description = "Create a new layer (ADMIN/EDITOR only)")
    public ResponseEntity<LayerDto> create(@Valid @RequestBody LayerDto dto) {
        return ResponseEntity.ok(layerService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Update layer", description = "Update an existing layer (ADMIN/EDITOR only)")
    public ResponseEntity<LayerDto> update(@PathVariable UUID id, @Valid @RequestBody LayerDto dto) {
        return ResponseEntity.ok(layerService.update(id, dto));
    }

    @PutMapping("/{id}/style")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Update layer style", description = "Update layer styling (ADMIN/EDITOR only)")
    public ResponseEntity<LayerDto> updateStyle(@PathVariable UUID id, @RequestBody Object style) {
        LayerDto dto = layerService.findById(id);
        dto.setStyle(style);
        return ResponseEntity.ok(layerService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete layer", description = "Soft delete a layer (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        layerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

