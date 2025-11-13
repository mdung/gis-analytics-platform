package com.example.gis.controller;

import com.example.gis.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/api/tiles")
@Tag(name = "Tiles", description = "Vector tile proxy APIs")
@RequiredArgsConstructor
@Slf4j
public class TileController {
    private final CacheService cacheService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${tileserver.url:http://tileserver:8080}")
    private String tileServerUrl;
    
    @GetMapping("/vector/{z}/{x}/{y}.pbf")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Get vector tile", description = "Proxy vector tile request to TileServer")
    public ResponseEntity<byte[]> getVectorTile(
            @PathVariable int z,
            @PathVariable int x,
            @PathVariable int y,
            @RequestParam(required = false) String layer) {
        
        try {
            // Generate cache key
            String cacheKey = cacheService.generateTileKey(z, x, y, layer != null ? layer : "default");
            
            // Try cache first
            byte[] cachedTile = cacheService.getCachedTile(cacheKey);
            if (cachedTile != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
                headers.set("Content-Encoding", "gzip");
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(cachedTile);
            }
            
            // Build TileServer URL
            String tileUrl = String.format("%s/data/%s/%d/%d/%d.pbf", 
                    tileServerUrl, 
                    layer != null ? layer : "default",
                    z, x, y);
            
            log.debug("Fetching tile from TileServer: {}", tileUrl);
            
            // Fetch tile from TileServer
            byte[] tileData = restTemplate.getForObject(tileUrl, byte[].class);
            
            if (tileData != null) {
                // Cache tile
                cacheService.cacheTile(cacheKey, tileData, null);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
                headers.set("Content-Encoding", "gzip");
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(tileData);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error fetching tile from TileServer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/raster/{z}/{x}/{y}.png")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR') or hasRole('VIEWER')")
    @Operation(summary = "Get raster tile", description = "Proxy raster tile request to TileServer")
    public ResponseEntity<byte[]> getRasterTile(
            @PathVariable int z,
            @PathVariable int x,
            @PathVariable int y,
            @RequestParam(required = false) String layer) {
        
        try {
            // Generate cache key
            String cacheKey = "raster:" + cacheService.generateTileKey(z, x, y, layer != null ? layer : "default");
            
            // Try cache first
            byte[] cachedTile = cacheService.getCachedTile(cacheKey);
            if (cachedTile != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.IMAGE_PNG);
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(cachedTile);
            }
            
            // Build TileServer URL
            String tileUrl = String.format("%s/data/%s/%d/%d/%d.png", 
                    tileServerUrl, 
                    layer != null ? layer : "default",
                    z, x, y);
            
            log.debug("Fetching raster tile from TileServer: {}", tileUrl);
            
            // Fetch tile from TileServer
            byte[] tileData = restTemplate.getForObject(tileUrl, byte[].class);
            
            if (tileData != null) {
                // Cache tile
                cacheService.cacheTile(cacheKey, tileData, null);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.IMAGE_PNG);
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(tileData);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error fetching raster tile from TileServer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

