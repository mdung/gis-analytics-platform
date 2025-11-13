package com.example.gis.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class CSVParser {
    private final GeometryFactory geometryFactory;

    // Common column name patterns for latitude
    private static final List<String> LAT_PATTERNS = Arrays.asList(
            "lat", "latitude", "y", "ycoord", "y_coord", "ycoord", "緯度"
    );

    // Common column name patterns for longitude
    private static final List<String> LNG_PATTERNS = Arrays.asList(
            "lng", "lon", "long", "longitude", "x", "xcoord", "x_coord", "経度"
    );

    public CSVParser() {
        this.geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
    }

    /**
     * Auto-detect lat/lng columns from CSV header
     */
    public ColumnDetectionResult detectLatLngColumns(InputStream inputStream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            List<String> headers = new ArrayList<>(headerMap.keySet());
            
            String latColumn = findMatchingColumn(headers, LAT_PATTERNS);
            String lngColumn = findMatchingColumn(headers, LNG_PATTERNS);
            
            return new ColumnDetectionResult(latColumn, lngColumn, headers);
        }
    }

    /**
     * Parse CSV with auto-detected or specified lat/lng columns
     */
    public List<FileParser.ParsedFeature> parseCSV(InputStream inputStream, String latColumn, String lngColumn) throws Exception {
        List<FileParser.ParsedFeature> features = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            
            // Auto-detect if not specified
            if (latColumn == null || lngColumn == null) {
                List<String> headers = new ArrayList<>(headerMap.keySet());
                latColumn = findMatchingColumn(headers, LAT_PATTERNS);
                lngColumn = findMatchingColumn(headers, LNG_PATTERNS);
                
                if (latColumn == null || lngColumn == null) {
                    throw new IllegalArgumentException(
                            "Could not auto-detect lat/lng columns. Available columns: " + headers);
                }
            }
            
            if (!headerMap.containsKey(latColumn) || !headerMap.containsKey(lngColumn)) {
                throw new IllegalArgumentException(
                        String.format("Lat/Lng columns not found. Lat: %s, Lng: %s. Available: %s",
                                latColumn, lngColumn, headerMap.keySet()));
            }
            
            int latIndex = headerMap.get(latColumn);
            int lngIndex = headerMap.get(lngColumn);
            
            int rowNumber = 1; // Start from 1 (header is row 0)
            for (CSVRecord record : csvParser) {
                rowNumber++;
                try {
                    String latStr = record.get(latIndex).trim();
                    String lngStr = record.get(lngIndex).trim();
                    
                    if (latStr.isEmpty() || lngStr.isEmpty()) {
                        continue; // Skip rows with empty coordinates
                    }
                    
                    double lat = Double.parseDouble(latStr);
                    double lng = Double.parseDouble(lngStr);
                    
                    // Validate coordinate ranges
                    if (lat < -90 || lat > 90) {
                        throw new IllegalArgumentException(
                                String.format("Invalid latitude %f at row %d (must be between -90 and 90)", lat, rowNumber));
                    }
                    if (lng < -180 || lng > 180) {
                        throw new IllegalArgumentException(
                                String.format("Invalid longitude %f at row %d (must be between -180 and 180)", lng, rowNumber));
                    }
                    
                    Point point = geometryFactory.createPoint(
                            new org.locationtech.jts.geom.Coordinate(lng, lat)
                    );
                    
                    Map<String, Object> properties = new HashMap<>();
                    for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                        String key = entry.getKey();
                        if (!key.equals(latColumn) && !key.equals(lngColumn)) {
                            String value = record.get(entry.getValue());
                            properties.put(key, parseValue(value));
                        }
                    }
                    
                    features.add(FileParser.ParsedFeature.builder()
                            .geometry(point)
                            .properties(properties)
                            .build());
                } catch (NumberFormatException e) {
                    // Skip invalid rows but log warning
                    System.err.println(String.format("Skipping row %d: invalid number format - %s", rowNumber, e.getMessage()));
                    continue;
                } catch (IllegalArgumentException e) {
                    // Re-throw validation errors
                    throw e;
                }
            }
        }
        
        return features;
    }

    private String findMatchingColumn(List<String> headers, List<String> patterns) {
        // First try exact match (case-insensitive)
        for (String pattern : patterns) {
            for (String header : headers) {
                if (header.equalsIgnoreCase(pattern)) {
                    return header;
                }
            }
        }
        
        // Then try contains match (case-insensitive)
        for (String pattern : patterns) {
            for (String header : headers) {
                if (header.toLowerCase().contains(pattern.toLowerCase()) ||
                    pattern.toLowerCase().contains(header.toLowerCase())) {
                    return header;
                }
            }
        }
        
        return null;
    }

    private Object parseValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        value = value.trim();
        
        // Try to parse as number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            // Return as string
            return value;
        }
    }

    public static class ColumnDetectionResult {
        private final String latColumn;
        private final String lngColumn;
        private final List<String> allColumns;

        public ColumnDetectionResult(String latColumn, String lngColumn, List<String> allColumns) {
            this.latColumn = latColumn;
            this.lngColumn = lngColumn;
            this.allColumns = allColumns;
        }

        public String getLatColumn() {
            return latColumn;
        }

        public String getLngColumn() {
            return lngColumn;
        }

        public List<String> getAllColumns() {
            return allColumns;
        }

        public boolean isDetected() {
            return latColumn != null && lngColumn != null;
        }
    }
}

