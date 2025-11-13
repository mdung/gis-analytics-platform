package com.example.gis.service;

import com.example.gis.entity.Feature;
import com.example.gis.entity.Layer;
import com.example.gis.entity.Upload;
import com.example.gis.entity.User;
import com.example.gis.repository.FeatureRepository;
import com.example.gis.repository.LayerRepository;
import com.example.gis.repository.UploadRepository;
import com.example.gis.repository.UserRepository;
import com.example.gis.util.CRSTransformer;
import com.example.gis.util.FileParser;
import com.example.gis.util.ShapefileParser;
import com.example.gis.util.CSVParser;
import com.example.gis.util.GeometryValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {
    private final UploadRepository uploadRepository;
    private final LayerRepository layerRepository;
    private final FeatureRepository featureRepository;
    private final UserRepository userRepository;
    private final MinIOService minIOService;
    private final FileParser fileParser;
    private final ShapefileParser shapefileParser;
    private final CSVParser csvParser;
    private final CRSTransformer crsTransformer;
    private final GeometryValidator geometryValidator;
    private final ObjectMapper objectMapper;

    @Transactional
    public Upload createUpload(MultipartFile file, UUID layerId, String latColumn, String lngColumn) throws Exception {
        User user = getCurrentUser();
        if (user == null) {
            throw new RuntimeException("User not authenticated");
        }

        Layer layer = null;
        if (layerId != null) {
            layer = layerRepository.findById(layerId)
                    .orElseThrow(() -> new RuntimeException("Layer not found"));
        }

        String fileKey = minIOService.uploadFile(file);
        
        Upload upload = Upload.builder()
                .user(user)
                .layer(layer)
                .fileKey(fileKey)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .status(Upload.UploadStatus.UPLOADED)
                .build();
        
        upload = uploadRepository.save(upload);
        
        // Process asynchronously
        processUploadAsync(upload, file, latColumn, lngColumn);
        
        return upload;
    }

    @Async
    public CompletableFuture<Void> processUploadAsync(Upload upload, MultipartFile file, String latColumn, String lngColumn) {
        try {
            upload.setStatus(Upload.UploadStatus.PROCESSING);
            uploadRepository.save(upload);
            
            List<FileParser.ParsedFeature> parsedFeatures;
            Integer sourceSrid = 4326; // Default
            
            String fileName = file.getOriginalFilename().toLowerCase();
            
            if (fileName.endsWith(".geojson") || fileName.endsWith(".json")) {
                parsedFeatures = fileParser.parseGeoJSON(file.getInputStream());
            } else if (fileName.endsWith(".zip")) {
                // Handle Shapefile ZIP with full support
                ShapefileParser.ShapefileParseResult result = shapefileParser.parseShapefileZip(file.getInputStream());
                parsedFeatures = result.getFeatures();
                sourceSrid = result.getSourceSrid();
            } else if (fileName.endsWith(".csv")) {
                // Auto-detect or use provided columns
                parsedFeatures = csvParser.parseCSV(file.getInputStream(), latColumn, lngColumn);
            } else {
                throw new IllegalArgumentException("Unsupported file format: " + fileName);
            }
            
            if (parsedFeatures.isEmpty()) {
                upload.setStatus(Upload.UploadStatus.FAILED);
                upload.setMessage("No features found in file");
                uploadRepository.save(upload);
                return CompletableFuture.completedFuture(null);
            }
            
            // Get or create layer
            Layer layer = upload.getLayer();
            if (layer == null) {
                // Auto-create layer based on file
                layer = createLayerFromFile(file.getOriginalFilename(), parsedFeatures.get(0));
                upload.setLayer(layer);
            }
            
            // Transform CRS if needed
            List<Feature> features = new ArrayList<>();
            int successCount = 0;
            int failedCount = 0;
            
            for (FileParser.ParsedFeature parsedFeature : parsedFeatures) {
                try {
                    Geometry geometry = parsedFeature.getGeometry();
                    Integer featureSrid = parsedFeature.getSourceSrid() != null ? parsedFeature.getSourceSrid() : sourceSrid;
                    
                    // Validate geometry
                    GeometryValidator.ValidationResult validation = geometryValidator.validate(geometry);
                    if (!validation.isValid()) {
                        log.warn("Invalid geometry, attempting normalization: {}", validation.getErrorMessage());
                        geometry = geometryValidator.normalize(geometry);
                        validation = geometryValidator.validate(geometry);
                        if (!validation.isValid()) {
                            log.warn("Failed to fix invalid geometry: {}", validation.getErrorMessage());
                            failedCount++;
                            continue;
                        }
                    }
                    
                    // Transform to target SRID (4326)
                    if (featureSrid != null && !featureSrid.equals(4326)) {
                        geometry = crsTransformer.transform(geometry, featureSrid, 4326);
                    }
                    
                    // Final validation after transformation
                    if (!geometryValidator.isWithinBounds(geometry)) {
                        log.warn("Geometry out of bounds after transformation");
                        failedCount++;
                        continue;
                    }
                    
                    // Normalize final geometry
                    geometry = geometryValidator.normalize(geometry);
                    
                    Feature feature = Feature.builder()
                            .layer(layer)
                            .properties(objectMapper.valueToTree(parsedFeature.getProperties()).toString())
                            .geom(geometry)
                            .createdBy(upload.getUser())
                            .build();
                    
                    features.add(feature);
                    successCount++;
                } catch (Exception e) {
                    log.warn("Failed to process feature: {}", e.getMessage(), e);
                    failedCount++;
                }
            }
            
            // Bulk insert in batches
            if (!features.isEmpty()) {
                int batchSize = 100;
                for (int i = 0; i < features.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, features.size());
                    List<Feature> batch = features.subList(i, end);
                    featureRepository.saveAll(batch);
                    featureRepository.flush();
                }
            }
            
            // Update upload status
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalFeatures", parsedFeatures.size());
            stats.put("successCount", successCount);
            stats.put("failedCount", failedCount);
            stats.put("bbox", calculateBbox(features));
            
            upload.setStatus(Upload.UploadStatus.PROCESSED);
            upload.setStats(objectMapper.valueToTree(stats).toString());
            upload.setMessage(String.format("Processed %d features successfully, %d failed", successCount, failedCount));
            uploadRepository.save(upload);
            
        } catch (Exception e) {
            log.error("Error processing upload {}: {}", upload.getId(), e.getMessage(), e);
            upload.setStatus(Upload.UploadStatus.FAILED);
            upload.setMessage("Processing failed: " + e.getMessage());
            uploadRepository.save(upload);
        }
        
        return CompletableFuture.completedFuture(null);
    }


    private Layer createLayerFromFile(String fileName, FileParser.ParsedFeature sampleFeature) {
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String code = baseName.toLowerCase().replaceAll("[^a-z0-9]", "_");
        
        Layer.GeometryType geomType;
        String geomTypeName = sampleFeature.getGeometry().getGeometryType();
        if (geomTypeName.contains("Point")) {
            geomType = Layer.GeometryType.POINT;
        } else if (geomTypeName.contains("Line")) {
            geomType = Layer.GeometryType.LINE;
        } else {
            geomType = Layer.GeometryType.POLYGON;
        }
        
        Layer layer = Layer.builder()
                .code(code)
                .name(baseName)
                .geomType(geomType)
                .srid(4326)
                .style("{}")
                .metadata("{}")
                .createdBy(getCurrentUser())
                .build();
        
        return layerRepository.save(layer);
    }

    private Map<String, Double> calculateBbox(List<Feature> features) {
        if (features.isEmpty()) {
            return null;
        }
        
        double minLng = Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE;
        double maxLng = Double.MIN_VALUE;
        double maxLat = Double.MIN_VALUE;
        
        for (Feature feature : features) {
            org.locationtech.jts.geom.Envelope envelope = feature.getGeom().getEnvelopeInternal();
            minLng = Math.min(minLng, envelope.getMinX());
            minLat = Math.min(minLat, envelope.getMinY());
            maxLng = Math.max(maxLng, envelope.getMaxX());
            maxLat = Math.max(maxLat, envelope.getMaxY());
        }
        
        Map<String, Double> bbox = new HashMap<>();
        bbox.put("minLng", minLng);
        bbox.put("minLat", minLat);
        bbox.put("maxLng", maxLng);
        bbox.put("maxLat", maxLat);
        return bbox;
    }

    public Upload getUpload(UUID id) {
        return uploadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Upload not found"));
    }

    public List<Upload> getUserUploads(UUID userId) {
        return uploadRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Upload> getLayerUploads(UUID layerId) {
        return uploadRepository.findByLayerIdOrderByCreatedAtDesc(layerId);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return userRepository.findByUsernameAndDeletedAtIsNull(auth.getName())
                    .orElse(null);
        }
        return null;
    }
}

