package com.example.gis.controller;

import com.example.gis.service.MinIOService;
import io.minio.messages.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/minio")
@Tag(name = "MinIO/S3", description = "MinIO/S3 storage management APIs")
@RequiredArgsConstructor
public class MinIOController {
    private final MinIOService minIOService;

    // ========== Bucket Management ==========

    @GetMapping("/buckets")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all buckets", description = "Get list of all buckets (ADMIN only)")
    public ResponseEntity<List<Bucket>> listBuckets() {
        try {
            return ResponseEntity.ok(minIOService.listBuckets());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/buckets/{bucketName}/exists")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Check bucket exists", description = "Check if bucket exists (ADMIN only)")
    public ResponseEntity<Map<String, Boolean>> bucketExists(@PathVariable String bucketName) {
        try {
            Map<String, Boolean> response = new HashMap<>();
            response.put("exists", minIOService.bucketExists(bucketName));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/buckets/{bucketName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create bucket", description = "Create a new bucket (ADMIN only)")
    public ResponseEntity<Void> createBucket(@PathVariable String bucketName) {
        try {
            minIOService.createBucket(bucketName);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/buckets/{bucketName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete bucket", description = "Delete an empty bucket (ADMIN only)")
    public ResponseEntity<Void> deleteBucket(@PathVariable String bucketName) {
        try {
            minIOService.deleteBucket(bucketName);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/buckets/{bucketName}/force")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force delete bucket", description = "Delete bucket and all its contents (ADMIN only)")
    public ResponseEntity<Void> deleteBucketForce(@PathVariable String bucketName) {
        try {
            minIOService.deleteBucketForce(bucketName);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========== File Operations ==========

    @GetMapping("/files")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "List files", description = "List files in default bucket (ADMIN/EDITOR only)")
    public ResponseEntity<List<MinIOService.FileInfo>> listFiles(
            @RequestParam(value = "prefix", required = false) String prefix) {
        try {
            return ResponseEntity.ok(minIOService.listFiles(prefix != null ? prefix : ""));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/files/{bucketName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "List files in bucket", description = "List files in specific bucket (ADMIN/EDITOR only)")
    public ResponseEntity<List<MinIOService.FileInfo>> listFilesInBucket(
            @PathVariable String bucketName,
            @RequestParam(value = "prefix", required = false) String prefix) {
        try {
            return ResponseEntity.ok(minIOService.listFiles(bucketName, prefix != null ? prefix : ""));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/files/metadata/{fileKey}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Get file metadata", description = "Get file metadata (ADMIN/EDITOR only)")
    public ResponseEntity<Map<String, Object>> getFileMetadata(@PathVariable String fileKey) {
        try {
            io.minio.StatObjectResponse metadata = minIOService.getFileMetadata(fileKey);
            Map<String, Object> response = new HashMap<>();
            response.put("size", metadata.size());
            response.put("contentType", metadata.contentType());
            response.put("lastModified", metadata.lastModified());
            response.put("etag", metadata.etag());
            response.put("userMetadata", metadata.userMetadata());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/files/{fileKey}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Delete file", description = "Delete a file (ADMIN/EDITOR only)")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileKey) {
        try {
            minIOService.deleteFile(fileKey);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========== Presigned URLs ==========

    @PostMapping("/presigned/upload")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Generate presigned upload URL", description = "Generate presigned URL for direct upload (ADMIN/EDITOR only)")
    public ResponseEntity<Map<String, String>> generatePresignedUploadUrl(
            @RequestParam String fileKey,
            @RequestParam(value = "expirationMinutes", defaultValue = "60") int expirationMinutes) {
        try {
            String url = minIOService.generatePresignedUploadUrl(
                    fileKey,
                    Duration.ofMinutes(expirationMinutes)
            );
            Map<String, String> response = new HashMap<>();
            response.put("url", url);
            response.put("fileKey", fileKey);
            response.put("expirationMinutes", String.valueOf(expirationMinutes));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/presigned/download")
    @Operation(summary = "Generate presigned download URL", description = "Generate presigned URL for direct download")
    public ResponseEntity<Map<String, String>> generatePresignedDownloadUrl(
            @RequestParam String fileKey,
            @RequestParam(value = "expirationMinutes", defaultValue = "60") int expirationMinutes) {
        try {
            String url = minIOService.generatePresignedDownloadUrl(
                    fileKey,
                    Duration.ofMinutes(expirationMinutes)
            );
            Map<String, String> response = new HashMap<>();
            response.put("url", url);
            response.put("fileKey", fileKey);
            response.put("expirationMinutes", String.valueOf(expirationMinutes));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/presigned/delete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    @Operation(summary = "Generate presigned delete URL", description = "Generate presigned URL for direct delete (ADMIN/EDITOR only)")
    public ResponseEntity<Map<String, String>> generatePresignedDeleteUrl(
            @RequestParam String fileKey,
            @RequestParam(value = "expirationMinutes", defaultValue = "5") int expirationMinutes) {
        try {
            String url = minIOService.generatePresignedDeleteUrl(
                    fileKey,
                    Duration.ofMinutes(expirationMinutes)
            );
            Map<String, String> response = new HashMap<>();
            response.put("url", url);
            response.put("fileKey", fileKey);
            response.put("expirationMinutes", String.valueOf(expirationMinutes));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/presigned/upload/{bucketName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate presigned upload URL for bucket", description = "Generate presigned URL for specific bucket (ADMIN only)")
    public ResponseEntity<Map<String, String>> generatePresignedUploadUrlForBucket(
            @PathVariable String bucketName,
            @RequestParam String fileKey,
            @RequestParam(value = "expirationMinutes", defaultValue = "60") int expirationMinutes) {
        try {
            String url = minIOService.generatePresignedUploadUrl(
                    bucketName,
                    fileKey,
                    Duration.ofMinutes(expirationMinutes)
            );
            Map<String, String> response = new HashMap<>();
            response.put("url", url);
            response.put("bucketName", bucketName);
            response.put("fileKey", fileKey);
            response.put("expirationMinutes", String.valueOf(expirationMinutes));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

