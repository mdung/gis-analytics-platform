package com.example.gis.util;

import com.example.gis.entity.Layer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class FileParser {
    private final ObjectMapper objectMapper;
    private final GeometryFactory geometryFactory;
    private final GeometryJSON geometryJSON;

    public FileParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        this.geometryJSON = new GeometryJSON();
    }

    public List<ParsedFeature> parseGeoJSON(InputStream inputStream) throws Exception {
        JsonNode root = objectMapper.readTree(inputStream);
        List<ParsedFeature> features = new ArrayList<>();

        if (root.has("features")) {
            JsonNode featuresNode = root.get("features");
            for (JsonNode featureNode : featuresNode) {
                ParsedFeature feature = parseGeoJSONFeature(featureNode);
                if (feature != null) {
                    features.add(feature);
                }
            }
        } else if (root.has("type") && "Feature".equals(root.get("type").asText())) {
            ParsedFeature feature = parseGeoJSONFeature(root);
            if (feature != null) {
                features.add(feature);
            }
        }

        return features;
    }

    private ParsedFeature parseGeoJSONFeature(JsonNode featureNode) throws Exception {
        if (!featureNode.has("geometry") || !featureNode.has("properties")) {
            return null;
        }

        JsonNode geometryNode = featureNode.get("geometry");
        String geometryJson = objectMapper.writeValueAsString(geometryNode);
        Geometry geometry = geometryJSON.read(geometryJson);
        
        Map<String, Object> properties = new HashMap<>();
        JsonNode propertiesNode = featureNode.get("properties");
        propertiesNode.fields().forEachRemaining(entry -> {
            properties.put(entry.getKey(), getValue(entry.getValue()));
        });

        return ParsedFeature.builder()
                .geometry(geometry)
                .properties(properties)
                .build();
    }

    private Object getValue(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.isInt() ? node.asInt() : node.asDouble();
        if (node.isBoolean()) return node.asBoolean();
        if (node.isNull()) return null;
        return node.toString();
    }

    public List<ParsedFeature> parseShapefile(InputStream shpStream, InputStream dbfStream) throws Exception {
        List<ParsedFeature> features = new ArrayList<>();
        
        // Note: Shapefile parsing requires file-based access, so we need to save to temp files
        File tempShp = File.createTempFile("shapefile", ".shp");
        File tempDbf = File.createTempFile("shapefile", ".dbf");
        
        try {
            copyStreamToFile(shpStream, tempShp);
            if (dbfStream != null) {
                copyStreamToFile(dbfStream, tempDbf);
            }
            
            ShapefileDataStore dataStore = new ShapefileDataStore(tempShp.toURI().toURL());
            SimpleFeatureCollection featureCollection = dataStore.getFeatureSource().getFeatures();
            
            try (SimpleFeatureIterator iterator = featureCollection.features()) {
                while (iterator.hasNext()) {
                    SimpleFeature simpleFeature = iterator.next();
                    ParsedFeature feature = parseSimpleFeature(simpleFeature);
                    if (feature != null) {
                        features.add(feature);
                    }
                }
            }
            
            dataStore.dispose();
        } finally {
            tempShp.delete();
            if (tempDbf.exists()) {
                tempDbf.delete();
            }
        }
        
        return features;
    }

    private ParsedFeature parseSimpleFeature(SimpleFeature simpleFeature) throws Exception {
        org.locationtech.jts.geom.Geometry geom = (org.locationtech.jts.geom.Geometry) simpleFeature.getDefaultGeometry();
        if (geom == null) {
            return null;
        }

        Map<String, Object> properties = new HashMap<>();
        for (org.opengis.feature.Property property : simpleFeature.getProperties()) {
            if (!property.getName().toString().equals("the_geom")) {
                properties.put(property.getName().toString(), property.getValue());
            }
        }

        return ParsedFeature.builder()
                .geometry(geom)
                .properties(properties)
                .build();
    }

    public List<ParsedFeature> parseCSV(InputStream inputStream, String latColumn, String lngColumn) throws Exception {
        List<ParsedFeature> features = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            
            if (!headerMap.containsKey(latColumn) || !headerMap.containsKey(lngColumn)) {
                throw new IllegalArgumentException("Lat/Lng columns not found in CSV");
            }
            
            int latIndex = headerMap.get(latColumn);
            int lngIndex = headerMap.get(lngColumn);
            
            for (CSVRecord record : csvParser) {
                try {
                    double lat = Double.parseDouble(record.get(latIndex));
                    double lng = Double.parseDouble(record.get(lngIndex));
                    
                    Point point = geometryFactory.createPoint(
                            new org.locationtech.jts.geom.Coordinate(lng, lat)
                    );
                    
                    Map<String, Object> properties = new HashMap<>();
                    for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                        if (!entry.getKey().equals(latColumn) && !entry.getKey().equals(lngColumn)) {
                            properties.put(entry.getKey(), record.get(entry.getValue()));
                        }
                    }
                    
                    features.add(ParsedFeature.builder()
                            .geometry(point)
                            .properties(properties)
                            .build());
                } catch (NumberFormatException e) {
                    // Skip invalid rows
                    continue;
                }
            }
        }
        
        return features;
    }

    private void copyStreamToFile(InputStream inputStream, File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    public static class ParsedFeature {
        private Geometry geometry;
        private Map<String, Object> properties;
        private Integer sourceSrid;

        public static ParsedFeatureBuilder builder() {
            return new ParsedFeatureBuilder();
        }

        public Geometry getGeometry() {
            return geometry;
        }

        public void setGeometry(Geometry geometry) {
            this.geometry = geometry;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }

        public Integer getSourceSrid() {
            return sourceSrid;
        }

        public void setSourceSrid(Integer sourceSrid) {
            this.sourceSrid = sourceSrid;
        }

        public static class ParsedFeatureBuilder {
            private ParsedFeature feature = new ParsedFeature();

            public ParsedFeatureBuilder geometry(Geometry geometry) {
                feature.setGeometry(geometry);
                return this;
            }

            public ParsedFeatureBuilder properties(Map<String, Object> properties) {
                feature.setProperties(properties);
                return this;
            }

            public ParsedFeatureBuilder sourceSrid(Integer srid) {
                feature.setSourceSrid(srid);
                return this;
            }

            public ParsedFeature build() {
                return feature;
            }
        }
    }
}

