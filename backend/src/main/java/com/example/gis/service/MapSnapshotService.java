package com.example.gis.service;

import com.example.gis.dto.ExportRequest;
import com.example.gis.entity.Feature;
import com.example.gis.entity.Layer;
import com.example.gis.repository.FeatureRepository;
import com.example.gis.repository.LayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Service for generating map snapshots as PNG images
 * Note: This is a basic implementation. For production, consider using:
 * - Headless browser (Selenium, Puppeteer) to render MapLibre/Leaflet
 * - Map rendering libraries (Mapnik, MapServer)
 * - Static map services (Mapbox Static API, Google Static Maps)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MapSnapshotService {
    private final FeatureRepository featureRepository;
    private final LayerRepository layerRepository;

    /**
     * Generate a simple map snapshot as PNG
     * This is a basic implementation - for production, use proper map rendering
     */
    public byte[] generateMapSnapshot(ExportRequest request, int width, int height) throws Exception {
        Layer layer = layerRepository.findById(request.getLayerId())
                .orElseThrow(() -> new RuntimeException("Layer not found"));

        List<Feature> features = getFilteredFeatures(request);

        if (features.isEmpty()) {
            throw new RuntimeException("No features to render");
        }

        // Calculate bounds
        double minLng = features.stream()
                .mapToDouble(f -> f.getGeom().getEnvelopeInternal().getMinX())
                .min().orElse(-180);
        double maxLng = features.stream()
                .mapToDouble(f -> f.getGeom().getEnvelopeInternal().getMaxX())
                .max().orElse(180);
        double minLat = features.stream()
                .mapToDouble(f -> f.getGeom().getEnvelopeInternal().getMinY())
                .min().orElse(-90);
        double maxLat = features.stream()
                .mapToDouble(f -> f.getGeom().getEnvelopeInternal().getMaxY())
                .max().orElse(90);

        // Create image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Fill background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Draw title
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString(layer.getName(), 10, 20);

        // Draw features (simplified - just points for now)
        g.setColor(Color.BLUE);
        for (Feature feature : features) {
            if (feature.getGeom() instanceof org.locationtech.jts.geom.Point) {
                org.locationtech.jts.geom.Point point = (org.locationtech.jts.geom.Point) feature.getGeom();
                int x = (int) ((point.getX() - minLng) / (maxLng - minLng) * width);
                int y = (int) (height - (point.getY() - minLat) / (maxLat - minLat) * height);
                
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    g.fillOval(x - 3, y - 3, 6, 6);
                }
            }
        }

        g.dispose();

        // Convert to PNG bytes
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    private List<Feature> getFilteredFeatures(ExportRequest request) {
        // Reuse logic from ExportService or extract to shared service
        // For now, simplified version
        if (request.getMinLng() != null && request.getMinLat() != null &&
            request.getMaxLng() != null && request.getMaxLat() != null) {
            return featureRepository.findFeaturesInBbox(
                    request.getLayerId(),
                    request.getMinLng(),
                    request.getMinLat(),
                    request.getMaxLng(),
                    request.getMaxLat()
            );
        } else {
            return featureRepository.findByLayerIdAndDeletedAtIsNull(
                    request.getLayerId(),
                    org.springframework.data.domain.PageRequest.of(0, 1000)
            ).getContent();
        }
    }
}

