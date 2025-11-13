package com.example.gis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String SPATIAL_QUERY_PREFIX = "spatial:query:";
    private static final String TILE_PREFIX = "tile:";
    private static final String FEATURE_BBOX_PREFIX = "feature:bbox:";
    private static final String CLUSTER_PREFIX = "cluster:";
    private static final String HEATMAP_PREFIX = "heatmap:";

    // Default TTLs
    private static final int SPATIAL_QUERY_TTL = 3600; // 1 hour
    private static final int TILE_TTL = 7200; // 2 hours
    private static final int FEATURE_BBOX_TTL = 1800; // 30 minutes
    private static final int CLUSTER_TTL = 600; // 10 minutes
    private static final int HEATMAP_TTL = 1800; // 30 minutes

    /**
     * Cache spatial query result
     */
    public <T> void cacheSpatialQuery(String queryKey, T result, Integer ttlSeconds) {
        String key = SPATIAL_QUERY_PREFIX + queryKey;
        try {
            redisTemplate.opsForValue().set(key, result, ttlSeconds != null ? ttlSeconds : SPATIAL_QUERY_TTL, TimeUnit.SECONDS);
            log.debug("Cached spatial query: {}", key);
        } catch (Exception e) {
            log.warn("Failed to cache spatial query: {}", e.getMessage());
        }
    }

    /**
     * Get cached spatial query result
     */
    @SuppressWarnings("unchecked")
    public <T> T getCachedSpatialQuery(String queryKey, Class<T> type) {
        String key = SPATIAL_QUERY_PREFIX + queryKey;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache hit for spatial query: {}", key);
                return (T) cached;
            }
        } catch (Exception e) {
            log.warn("Failed to get cached spatial query: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Cache tile data
     */
    public void cacheTile(String tileKey, byte[] tileData, Integer ttlSeconds) {
        String key = TILE_PREFIX + tileKey;
        try {
            redisTemplate.opsForValue().set(key, tileData, ttlSeconds != null ? ttlSeconds : TILE_TTL, TimeUnit.SECONDS);
            log.debug("Cached tile: {}", key);
        } catch (Exception e) {
            log.warn("Failed to cache tile: {}", e.getMessage());
        }
    }

    /**
     * Get cached tile
     */
    public byte[] getCachedTile(String tileKey) {
        String key = TILE_PREFIX + tileKey;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache hit for tile: {}", key);
                return (byte[]) cached;
            }
        } catch (Exception e) {
            log.warn("Failed to get cached tile: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Cache feature bbox query
     */
    public <T> void cacheFeatureBbox(UUID layerId, double minLng, double minLat, double maxLng, double maxLat, T result) {
        String key = FEATURE_BBOX_PREFIX + layerId + ":" + 
                     String.format("%.6f,%.6f,%.6f,%.6f", minLng, minLat, maxLng, maxLat);
        try {
            redisTemplate.opsForValue().set(key, result, FEATURE_BBOX_TTL, TimeUnit.SECONDS);
            log.debug("Cached feature bbox: {}", key);
        } catch (Exception e) {
            log.warn("Failed to cache feature bbox: {}", e.getMessage());
        }
    }

    /**
     * Get cached feature bbox query
     */
    @SuppressWarnings("unchecked")
    public <T> T getCachedFeatureBbox(UUID layerId, double minLng, double minLat, double maxLng, double maxLat, Class<T> type) {
        String key = FEATURE_BBOX_PREFIX + layerId + ":" + 
                     String.format("%.6f,%.6f,%.6f,%.6f", minLng, minLat, maxLng, maxLat);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache hit for feature bbox: {}", key);
                return (T) cached;
            }
        } catch (Exception e) {
            log.warn("Failed to get cached feature bbox: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Cache cluster result
     */
    public void cacheCluster(String clusterKey, List<?> result, Integer ttlSeconds) {
        String key = CLUSTER_PREFIX + clusterKey;
        try {
            redisTemplate.opsForValue().set(key, result, ttlSeconds != null ? ttlSeconds : CLUSTER_TTL, TimeUnit.SECONDS);
            log.debug("Cached cluster: {}", key);
        } catch (Exception e) {
            log.warn("Failed to cache cluster: {}", e.getMessage());
        }
    }

    /**
     * Get cached cluster result
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getCachedCluster(String clusterKey, Class<T> type) {
        String key = CLUSTER_PREFIX + clusterKey;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache hit for cluster: {}", key);
                return (List<T>) cached;
            }
        } catch (Exception e) {
            log.warn("Failed to get cached cluster: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Cache heatmap result
     */
    public void cacheHeatmap(String heatmapKey, List<?> result, Integer ttlSeconds) {
        String key = HEATMAP_PREFIX + heatmapKey;
        try {
            redisTemplate.opsForValue().set(key, result, ttlSeconds != null ? ttlSeconds : HEATMAP_TTL, TimeUnit.SECONDS);
            log.debug("Cached heatmap: {}", key);
        } catch (Exception e) {
            log.warn("Failed to cache heatmap: {}", e.getMessage());
        }
    }

    /**
     * Get cached heatmap result
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getCachedHeatmap(String heatmapKey, Class<T> type) {
        String key = HEATMAP_PREFIX + heatmapKey;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache hit for heatmap: {}", key);
                return (List<T>) cached;
            }
        } catch (Exception e) {
            log.warn("Failed to get cached heatmap: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Generate cache key for spatial query
     */
    public String generateSpatialQueryKey(UUID layerId, String queryType, Object... params) {
        StringBuilder key = new StringBuilder();
        key.append(layerId).append(":").append(queryType);
        for (Object param : params) {
            key.append(":").append(param != null ? param.toString() : "null");
        }
        return key.toString();
    }

    /**
     * Generate cache key for tile
     */
    public String generateTileKey(int z, int x, int y, String layerId) {
        return String.format("%s:%d:%d:%d", layerId, z, x, y);
    }

    /**
     * Clear cache by pattern
     */
    public void clearCacheByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} cache keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Failed to clear cache by pattern: {}", e.getMessage());
        }
    }

    /**
     * Clear all cache
     */
    public void clearAllCache() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            log.info("Cleared all cache");
        } catch (Exception e) {
            log.warn("Failed to clear all cache: {}", e.getMessage());
        }
    }
}

