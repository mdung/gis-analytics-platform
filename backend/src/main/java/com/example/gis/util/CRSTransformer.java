package com.example.gis.util;

import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.proj4j.*;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CRSTransformer {
    private final CRSFactory crsFactory;
    private static final Pattern EPSG_PATTERN = Pattern.compile("EPSG[:\"]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTHORITY_PATTERN = Pattern.compile("AUTHORITY\\[\"EPSG\",\"(\\d+)\"\\]", Pattern.CASE_INSENSITIVE);

    public CRSTransformer() {
        this.crsFactory = new CRSFactory();
    }
    
    /**
     * Transform geometry using GeoTools (preferred for complex transformations)
     */
    public Geometry transform(Geometry geometry, Integer sourceSrid, Integer targetSrid) throws Exception {
        if (sourceSrid == null || sourceSrid.equals(targetSrid)) {
            return geometry;
        }

        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:" + sourceSrid);
            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:" + targetSrid);
            MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
            
            return org.geotools.geometry.jts.JTS.transform(geometry, transform);
        } catch (Exception e) {
            // Fallback to Proj4J if GeoTools fails
            return transformWithProj4J(geometry, sourceSrid, targetSrid);
        }
    }

    /**
     * Transform geometry using Proj4J (fallback for unsupported CRS)
     */
    public Geometry transformWithProj4J(Geometry geometry, Integer sourceSrid, Integer targetSrid) throws Exception {
        if (sourceSrid == null || sourceSrid.equals(targetSrid)) {
            return geometry;
        }

        try {
            CoordinateReferenceSystem sourceCRS = crsFactory.createFromName("EPSG:" + sourceSrid);
            CoordinateReferenceSystem targetCRS = crsFactory.createFromName("EPSG:" + targetSrid);
            CoordinateTransform transform = new CoordinateTransformFactory().createTransform(sourceCRS, targetCRS);
            
            // Transform coordinates
            org.locationtech.jts.geom.Coordinate[] coords = geometry.getCoordinates();
            ProjCoordinate[] projCoords = new ProjCoordinate[coords.length];
            
            for (int i = 0; i < coords.length; i++) {
                ProjCoordinate src = new ProjCoordinate(coords[i].x, coords[i].y);
                ProjCoordinate dst = new ProjCoordinate();
                transform.transform(src, dst);
                projCoords[i] = dst;
            }
            
            // Recreate geometry with transformed coordinates
            org.locationtech.jts.geom.Coordinate[] transformedCoords = new org.locationtech.jts.geom.Coordinate[projCoords.length];
            for (int i = 0; i < projCoords.length; i++) {
                transformedCoords[i] = new org.locationtech.jts.geom.Coordinate(projCoords[i].x, projCoords[i].y);
            }
            
            org.locationtech.jts.geom.GeometryFactory factory = new org.locationtech.jts.geom.GeometryFactory();
            return factory.createGeometry(geometry).getFactory().createGeometry(
                    factory.createGeometry(geometry).getFactory().createGeometry(geometry)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to transform CRS from EPSG:" + sourceSrid + " to EPSG:" + targetSrid + " using Proj4J", e);
        }
    }

    /**
     * Enhanced PRJ file parser with better EPSG detection
     */
    public Integer detectSridFromPrj(InputStream prjStream) throws Exception {
        StringBuilder prjContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(prjStream, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                prjContent.append(line).append("\n");
            }
        }
        
        String content = prjContent.toString();
        
        // Try AUTHORITY pattern first (most reliable)
        Matcher authMatcher = AUTHORITY_PATTERN.matcher(content);
        if (authMatcher.find()) {
            try {
                return Integer.parseInt(authMatcher.group(1));
            } catch (NumberFormatException e) {
                // Continue to next method
            }
        }
        
        // Try EPSG pattern
        Matcher epsgMatcher = EPSG_PATTERN.matcher(content);
        if (epsgMatcher.find()) {
            try {
                return Integer.parseInt(epsgMatcher.group(1));
            } catch (NumberFormatException e) {
                // Continue to next method
            }
        }
        
        // Common CRS detection by name
        String upperContent = content.toUpperCase();
        
        // WGS84 variants
        if (upperContent.contains("WGS 84") || upperContent.contains("WGS84") || 
            upperContent.contains("WORLD GEODETIC SYSTEM 1984")) {
            return 4326;
        }
        
        // UTM zones
        if (upperContent.contains("UTM")) {
            // Try to extract zone number
            Pattern utmPattern = Pattern.compile("ZONE\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher utmMatcher = utmPattern.matcher(content);
            if (utmMatcher.find()) {
                int zone = Integer.parseInt(utmMatcher.group(1));
                // Determine hemisphere
                boolean isNorth = !upperContent.contains("SOUTH");
                if (isNorth) {
                    return 32600 + zone; // UTM North
                } else {
                    return 32700 + zone; // UTM South
                }
            }
        }
        
        // Vietnam VN-2000
        if (upperContent.contains("VN-2000") || upperContent.contains("VIETNAM 2000")) {
            // Common VN-2000 zones
            if (upperContent.contains("ZONE 1") || upperContent.contains("4814")) {
                return 4814;
            }
            if (upperContent.contains("ZONE 2") || upperContent.contains("4815")) {
                return 4815;
            }
            if (upperContent.contains("ZONE 3") || upperContent.contains("4816")) {
                return 4816;
            }
        }
        
        // Web Mercator
        if (upperContent.contains("WEB MERCATOR") || upperContent.contains("GOOGLE") || 
            upperContent.contains("EPSG:3857")) {
            return 3857;
        }
        
        return null;
    }

    /**
     * Detect SRID from WKT string
     */
    public Integer detectSridFromWkt(String wkt) {
        if (wkt == null || wkt.isEmpty()) {
            return null;
        }
        
        // Try AUTHORITY pattern
        Matcher authMatcher = AUTHORITY_PATTERN.matcher(wkt);
        if (authMatcher.find()) {
            try {
                return Integer.parseInt(authMatcher.group(1));
            } catch (NumberFormatException e) {
                // Continue
            }
        }
        
        // Try EPSG pattern
        Matcher epsgMatcher = EPSG_PATTERN.matcher(wkt);
        if (epsgMatcher.find()) {
            try {
                return Integer.parseInt(epsgMatcher.group(1));
            } catch (NumberFormatException e) {
                // Continue
            }
        }
        
        return null;
    }
}

