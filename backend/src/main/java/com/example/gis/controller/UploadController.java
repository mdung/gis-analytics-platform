package com.example.gis.controller;

import com.example.gis.dto.UploadDto;
import com.example.gis.entity.Upload;
import com.example.gis.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/uploads")
@Tag(name = "Uploads", description = "File upload and processing APIs")
@RequiredArgsConstructor
public class UploadController {
    private final UploadService uploadService;
    private final com.example.gis.service.MinIOService minIOService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Upload file", description = "Upload GeoJSON, Shapefile ZIP, or CSV file for processing. CSV lat/lng columns can be auto-detected if not provided. (ADMIN/EDITOR only)")
    public ResponseEntity<UploadDto> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "layerId", required = false) UUID layerId,
            @RequestParam(value = "latColumn", required = false) String latColumn,
            @RequestParam(value = "lngColumn", required = false) String lngColumn) {
        
        try {
            Upload upload = uploadService.createUpload(file, layerId, latColumn, lngColumn);
            return ResponseEntity.ok(toDto(upload));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get upload by ID", description = "Get upload details and processing status")
    public ResponseEntity<UploadDto> getUpload(@PathVariable UUID id) {
        Upload upload = uploadService.getUpload(id);
        return ResponseEntity.ok(toDto(upload));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user uploads", description = "Get all uploads by user")
    public ResponseEntity<List<UploadDto>> getUserUploads(@PathVariable UUID userId) {
        List<Upload> uploads = uploadService.getUserUploads(userId);
        return ResponseEntity.ok(uploads.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/layer/{layerId}")
    @Operation(summary = "Get layer uploads", description = "Get all uploads for a layer")
    public ResponseEntity<List<UploadDto>> getLayerUploads(@PathVariable UUID layerId) {
        List<Upload> uploads = uploadService.getLayerUploads(layerId);
        return ResponseEntity.ok(uploads.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/download/{fileKey}")
    @Operation(summary = "Download uploaded file", description = "Download the original uploaded file")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(@PathVariable String fileKey) {
        try {
            InputStream inputStream = minIOService.downloadFile(fileKey);
            org.springframework.core.io.InputStreamResource resource = 
                    new org.springframework.core.io.InputStreamResource(inputStream);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileKey + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private UploadDto toDto(Upload upload) {
        return UploadDto.builder()
                .id(upload.getId())
                .userId(upload.getUser().getId())
                .layerId(upload.getLayer() != null ? upload.getLayer().getId() : null)
                .fileName(upload.getFileName())
                .fileSize(upload.getFileSize())
                .status(upload.getStatus().name())
                .message(upload.getMessage())
                .stats(parseStats(upload.getStats()))
                .createdAt(upload.getCreatedAt())
                .updatedAt(upload.getUpdatedAt())
                .build();
    }

    private Object parseStats(String statsJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(statsJson, Object.class);
        } catch (Exception e) {
            return null;
        }
    }
}

