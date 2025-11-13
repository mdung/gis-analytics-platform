package com.example.gis.util;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ShapefileParser {
    private final CRSTransformer crsTransformer;

    public ShapefileParser(CRSTransformer crsTransformer) {
        this.crsTransformer = crsTransformer;
    }

    /**
     * Parse Shapefile from ZIP archive
     */
    public ShapefileParseResult parseShapefileZip(InputStream zipStream) throws Exception {
        Map<String, File> extractedFiles = new HashMap<>();
        Path tempDir = Files.createTempDirectory("shapefile_");
        
        try {
            // Extract ZIP contents
            try (ZipInputStream zis = new ZipInputStream(zipStream)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    String extension = getExtension(entryName).toLowerCase();
                    
                    if (isShapefileComponent(extension)) {
                        File tempFile = new File(tempDir.toFile(), entryName);
                        tempFile.getParentFile().mkdirs();
                        
                        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                        
                        extractedFiles.put(extension, tempFile);
                    }
                    zis.closeEntry();
                }
            }
            
            // Check required files
            if (!extractedFiles.containsKey(".shp")) {
                throw new IllegalArgumentException("Shapefile ZIP must contain .shp file");
            }
            
            File shpFile = extractedFiles.get(".shp");
            File dbfFile = extractedFiles.get(".dbf");
            File prjFile = extractedFiles.get(".prj");
            
            // Detect CRS from PRJ file
            Integer sourceSrid = null;
            if (prjFile != null && prjFile.exists()) {
                try (FileInputStream prjStream = new FileInputStream(prjFile)) {
                    sourceSrid = crsTransformer.detectSridFromPrj(prjStream);
                }
            }
            
            // Parse shapefile
            List<FileParser.ParsedFeature> features = parseShapefile(shpFile, dbfFile);
            
            // Set source SRID for all features
            if (sourceSrid != null) {
                for (FileParser.ParsedFeature feature : features) {
                    feature.setSourceSrid(sourceSrid);
                }
            }
            
            return new ShapefileParseResult(features, sourceSrid);
            
        } finally {
            // Cleanup temp files
            cleanupTempFiles(tempDir);
        }
    }

    /**
     * Parse Shapefile from individual files
     */
    public List<FileParser.ParsedFeature> parseShapefile(File shpFile, File dbfFile) throws Exception {
        List<FileParser.ParsedFeature> features = new ArrayList<>();
        
        ShapefileDataStore dataStore = new ShapefileDataStore(shpFile.toURI().toURL());
        
        try {
            SimpleFeatureCollection featureCollection = dataStore.getFeatureSource().getFeatures();
            
            try (SimpleFeatureIterator iterator = featureCollection.features()) {
                while (iterator.hasNext()) {
                    SimpleFeature simpleFeature = iterator.next();
                    FileParser.ParsedFeature feature = parseSimpleFeature(simpleFeature);
                    if (feature != null) {
                        features.add(feature);
                    }
                }
            }
        } finally {
            dataStore.dispose();
        }
        
        return features;
    }

    private FileParser.ParsedFeature parseSimpleFeature(SimpleFeature simpleFeature) {
        Geometry geom = (Geometry) simpleFeature.getDefaultGeometry();
        if (geom == null) {
            return null;
        }

        Map<String, Object> properties = new HashMap<>();
        for (org.opengis.feature.Property property : simpleFeature.getProperties()) {
            String name = property.getName().toString();
            if (!name.equals("the_geom") && !name.startsWith("fid")) {
                Object value = property.getValue();
                // Convert to appropriate type
                if (value != null) {
                    properties.put(name, convertValue(value));
                }
            }
        }

        return FileParser.ParsedFeature.builder()
                .geometry(geom)
                .properties(properties)
                .build();
    }

    private Object convertValue(Object value) {
        // Handle common types
        if (value instanceof Number) {
            return value;
        }
        if (value instanceof String) {
            return value;
        }
        if (value instanceof Date) {
            return value.toString();
        }
        return value != null ? value.toString() : null;
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot >= 0 ? filename.substring(lastDot) : "";
    }

    private boolean isShapefileComponent(String extension) {
        return extension.equals(".shp") || 
               extension.equals(".dbf") || 
               extension.equals(".shx") || 
               extension.equals(".prj") ||
               extension.equals(".cpg");
    }

    private void cleanupTempFiles(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            Files.deleteIfExists(tempDir);
        } catch (IOException e) {
            // Log warning but don't fail
            System.err.println("Failed to cleanup temp files: " + e.getMessage());
        }
    }

    public static class ShapefileParseResult {
        private final List<FileParser.ParsedFeature> features;
        private final Integer sourceSrid;

        public ShapefileParseResult(List<FileParser.ParsedFeature> features, Integer sourceSrid) {
            this.features = features;
            this.sourceSrid = sourceSrid;
        }

        public List<FileParser.ParsedFeature> getFeatures() {
            return features;
        }

        public Integer getSourceSrid() {
            return sourceSrid;
        }
    }
}

