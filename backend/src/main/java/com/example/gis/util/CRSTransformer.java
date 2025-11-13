package com.example.gis.util;

import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

@Component
public class CRSTransformer {
    
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
            throw new RuntimeException("Failed to transform CRS from EPSG:" + sourceSrid + " to EPSG:" + targetSrid, e);
        }
    }

    public Integer detectSridFromPrj(InputStream prjStream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(prjStream))) {
            String prjContent = reader.readLine();
            if (prjContent == null) {
                return null;
            }
            
            // Try to extract EPSG code from PRJ content
            // This is a simplified parser - in production, use proper PRJ parser
            if (prjContent.contains("EPSG")) {
                String[] parts = prjContent.split("EPSG");
                if (parts.length > 1) {
                    String code = parts[1].replaceAll("[^0-9]", "");
                    if (!code.isEmpty()) {
                        return Integer.parseInt(code);
                    }
                }
            }
            
            // Common CRS detection
            if (prjContent.contains("WGS 84") || prjContent.contains("WGS84")) {
                return 4326;
            }
            if (prjContent.contains("UTM") && prjContent.contains("48N")) {
                return 32648; // UTM Zone 48N
            }
        }
        
        return null;
    }
}

