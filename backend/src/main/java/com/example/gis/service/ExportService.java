package com.example.gis.service;

import com.example.gis.dto.ExportRequest;
import com.example.gis.entity.Feature;
import com.example.gis.entity.Layer;
import com.example.gis.repository.FeatureRepository;
import com.example.gis.repository.LayerRepository;
import com.example.gis.util.GeoJsonConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {
    private final FeatureRepository featureRepository;
    private final LayerRepository layerRepository;
    private final GeoJsonConverter geoJsonConverter;
    private final ObjectMapper objectMapper;

    /**
     * Export features as GeoJSON FeatureCollection
     */
    public byte[] exportGeoJSON(ExportRequest request) throws Exception {
        Layer layer = layerRepository.findById(request.getLayerId())
                .orElseThrow(() -> new RuntimeException("Layer not found"));

        List<Feature> features = getFilteredFeatures(request);

        ObjectNode featureCollection = objectMapper.createObjectNode();
        featureCollection.put("type", "FeatureCollection");

        ArrayNode featuresArray = objectMapper.createArrayNode();
        for (Feature feature : features) {
            ObjectNode featureNode = objectMapper.createObjectNode();
            featureNode.put("type", "Feature");
            featureNode.put("id", feature.getId().toString());

            // Geometry
            featureNode.set("geometry", geoJsonConverter.geometryToGeoJson(feature.getGeom()));

            // Properties
            Map<String, Object> properties = objectMapper.readValue(feature.getProperties(), Map.class);
            ObjectNode propertiesNode = objectMapper.valueToTree(properties);
            featureNode.set("properties", propertiesNode);

            featuresArray.add(featureNode);
        }

        featureCollection.set("features", featuresArray);

        // Add metadata
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("layerId", layer.getId().toString());
        metadata.put("layerName", layer.getName());
        metadata.put("featureCount", features.size());
        metadata.put("exportedAt", java.time.OffsetDateTime.now().toString());
        featureCollection.set("metadata", metadata);

        return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(featureCollection);
    }

    /**
     * Export features as CSV
     */
    public byte[] exportCSV(ExportRequest request) throws Exception {
        Layer layer = layerRepository.findById(request.getLayerId())
                .orElseThrow(() -> new RuntimeException("Layer not found"));

        List<Feature> features = getFilteredFeatures(request);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            // Write BOM for Excel compatibility
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);

            // Determine columns
            Set<String> allColumns = new LinkedHashSet<>();
            allColumns.add(request.getLngColumnName());
            allColumns.add(request.getLatColumnName());

            // Collect all property keys from features
            for (Feature feature : features) {
                Map<String, Object> properties = objectMapper.readValue(feature.getProperties(), Map.class);
                allColumns.addAll(properties.keySet());
            }

            // Filter columns if specified
            List<String> columns = request.getIncludeColumns() != null && !request.getIncludeColumns().isEmpty()
                    ? request.getIncludeColumns()
                    : new ArrayList<>(allColumns);

            // Ensure lat/lng columns are present
            if (!columns.contains(request.getLngColumnName())) {
                columns.add(0, request.getLngColumnName());
            }
            if (!columns.contains(request.getLatColumnName())) {
                columns.add(1, request.getLatColumnName());
            }

            // Write header
            writer.write(String.join(",", escapeCSVColumns(columns)));
            writer.write("\n");

            // Write data rows
            for (Feature feature : features) {
                Map<String, Object> properties = objectMapper.readValue(feature.getProperties(), Map.class);
                List<String> row = new ArrayList<>();

                for (String column : columns) {
                    if (column.equals(request.getLngColumnName())) {
                        Point point = (Point) feature.getGeom();
                        row.add(String.valueOf(point.getX()));
                    } else if (column.equals(request.getLatColumnName())) {
                        Point point = (Point) feature.getGeom();
                        row.add(String.valueOf(point.getY()));
                    } else {
                        Object value = properties.get(column);
                        row.add(value != null ? escapeCSVValue(value.toString()) : "");
                    }
                }

                writer.write(String.join(",", row));
                writer.write("\n");
            }
        }

        return baos.toByteArray();
    }

    /**
     * Get filtered features based on request
     */
    private List<Feature> getFilteredFeatures(ExportRequest request) {
        Layer layer = layerRepository.findById(request.getLayerId())
                .orElseThrow(() -> new RuntimeException("Layer not found"));

        List<Feature> features;

        // Apply spatial filter (bbox)
        if (request.getMinLng() != null && request.getMinLat() != null &&
            request.getMaxLng() != null && request.getMaxLat() != null) {
            features = featureRepository.findFeaturesInBbox(
                    request.getLayerId(),
                    request.getMinLng(),
                    request.getMinLat(),
                    request.getMaxLng(),
                    request.getMaxLat()
            );
        } else {
            // Get all features with pagination
            Pageable pageable = PageRequest.of(
                    request.getOffset() != null ? request.getOffset() : 0,
                    request.getLimit() != null ? request.getLimit() : 10000
            );
            Page<Feature> page = featureRepository.findByLayerIdAndDeletedAtIsNull(
                    request.getLayerId(),
                    pageable
            );
            features = page.getContent();
        }

        // Apply attribute filters
        if (request.getAttributeFilters() != null && !request.getAttributeFilters().isEmpty()) {
            features = features.stream()
                    .filter(feature -> {
                        try {
                            Map<String, Object> properties = objectMapper.readValue(
                                    feature.getProperties(), Map.class);
                            return matchesAttributeFilters(properties, request.getAttributeFilters());
                        } catch (Exception e) {
                            log.warn("Error reading feature properties: {}", e.getMessage());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        }

        // Apply geometry filter (within/intersect)
        if (request.getGeometryFilter() != null) {
            try {
                Geometry filterGeometry = geoJsonConverter.geoJsonToGeometry(request.getGeometryFilter());
                features = features.stream()
                        .filter(feature -> feature.getGeom().intersects(filterGeometry))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("Error applying geometry filter: {}", e.getMessage());
            }
        }

        // Apply limit if not already applied
        if (request.getLimit() != null && features.size() > request.getLimit()) {
            features = features.subList(0, request.getLimit());
        }

        return features;
    }

    private boolean matchesAttributeFilters(Map<String, Object> properties, Map<String, Object> filters) {
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            Object propertyValue = properties.get(filter.getKey());
            Object filterValue = filter.getValue();

            if (propertyValue == null && filterValue != null) {
                return false;
            }

            if (filterValue != null && !filterValue.equals(propertyValue)) {
                // Support partial string matching
                if (filterValue instanceof String && propertyValue instanceof String) {
                    if (!propertyValue.toString().toLowerCase().contains(filterValue.toString().toLowerCase())) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private List<String> escapeCSVColumns(List<String> columns) {
        return columns.stream()
                .map(this::escapeCSVValue)
                .collect(Collectors.toList());
    }

    private String escapeCSVValue(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

