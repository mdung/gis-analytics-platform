package com.example.gis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterPoint {
    private Double longitude;
    private Double latitude;
    private Integer pointCount; // Number of features in cluster
    private Boolean isCluster; // true if cluster, false if single point
    private List<UUID> featureIds; // IDs of features in cluster (if not too many)
    private ClusterBounds bounds; // Bounding box of cluster
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClusterBounds {
        private Double minLng;
        private Double minLat;
        private Double maxLng;
        private Double maxLat;
    }
}

