package com.example.gis.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitingConfig {
    
    // Rate limits per user/IP
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 100;
    private static final int DEFAULT_REQUESTS_PER_HOUR = 1000;
    
    // Separate limits for different user roles
    private static final int ADMIN_REQUESTS_PER_MINUTE = 500;
    private static final int ADMIN_REQUESTS_PER_HOUR = 10000;
    private static final int EDITOR_REQUESTS_PER_MINUTE = 200;
    private static final int EDITOR_REQUESTS_PER_HOUR = 5000;
    private static final int VIEWER_REQUESTS_PER_MINUTE = 100;
    private static final int VIEWER_REQUESTS_PER_HOUR = 2000;
    
    // IP-based rate limiting (for unauthenticated requests)
    private static final int IP_REQUESTS_PER_MINUTE = 60;
    private static final int IP_REQUESTS_PER_HOUR = 500;
    
    // Cache for buckets (per user/IP)
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();
    
    /**
     * Get or create bucket for user
     */
    public Bucket resolveBucket(String key, String role) {
        return bucketCache.computeIfAbsent(key, k -> {
            Bandwidth limit = getBandwidthForRole(role);
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });
    }
    
    /**
     * Get or create bucket for IP
     */
    public Bucket resolveBucketForIp(String ip) {
        return bucketCache.computeIfAbsent("ip:" + ip, k -> {
            Bandwidth limit = Bandwidth.classic(IP_REQUESTS_PER_HOUR, 
                    Refill.intervally(IP_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });
    }
    
    private Bandwidth getBandwidthForRole(String role) {
        int perMinute;
        int perHour;
        
        if (role == null || role.equals("VIEWER")) {
            perMinute = VIEWER_REQUESTS_PER_MINUTE;
            perHour = VIEWER_REQUESTS_PER_HOUR;
        } else if (role.equals("EDITOR")) {
            perMinute = EDITOR_REQUESTS_PER_MINUTE;
            perHour = EDITOR_REQUESTS_PER_HOUR;
        } else if (role.equals("ADMIN")) {
            perMinute = ADMIN_REQUESTS_PER_MINUTE;
            perHour = ADMIN_REQUESTS_PER_HOUR;
        } else {
            perMinute = DEFAULT_REQUESTS_PER_MINUTE;
            perHour = DEFAULT_REQUESTS_PER_HOUR;
        }
        
        return Bandwidth.classic(perHour, Refill.intervally(perMinute, Duration.ofMinutes(1)));
    }
    
    /**
     * Clear bucket cache (for testing or cleanup)
     */
    public void clearCache() {
        bucketCache.clear();
    }
}

