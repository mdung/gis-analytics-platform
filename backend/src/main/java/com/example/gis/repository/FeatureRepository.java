package com.example.gis.repository;

import com.example.gis.entity.Feature;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, UUID> {
    Page<Feature> findByLayerIdAndDeletedAtIsNull(UUID layerId, Pageable pageable);
    
    @Query(value = "SELECT f.*, ST_Distance(geography(f.geom), geography(:point)) as distance " +
            "FROM features f " +
            "WHERE f.layer_id = :layerId AND f.deleted_at IS NULL " +
            "ORDER BY f.geom <-> :point " +
            "LIMIT :k", nativeQuery = true)
    List<Object[]> findNearestFeatures(@Param("layerId") UUID layerId, 
                                       @Param("point") Point point, 
                                       @Param("k") int k);
    
    @Query(value = "SELECT f.* FROM features f " +
            "WHERE f.layer_id = :layerId " +
            "AND ST_Intersects(f.geom, :geometry) " +
            "AND f.deleted_at IS NULL", nativeQuery = true)
    List<Feature> findIntersectingFeatures(@Param("layerId") UUID layerId, 
                                          @Param("geometry") Geometry geometry);
    
    @Query(value = "SELECT f.* FROM features f " +
            "WHERE f.layer_id = :layerId " +
            "AND ST_Within(f.geom, :polygon) " +
            "AND f.deleted_at IS NULL", nativeQuery = true)
    List<Feature> findFeaturesWithin(@Param("layerId") UUID layerId, 
                                     @Param("polygon") Geometry polygon);
    
    @Query(value = "SELECT f.* FROM features f " +
            "WHERE f.layer_id = :layerId " +
            "AND ST_Contains(:polygon, f.geom) " +
            "AND f.deleted_at IS NULL", nativeQuery = true)
    List<Feature> findFeaturesContained(@Param("layerId") UUID layerId, 
                                        @Param("polygon") Geometry polygon);
    
    @Query(value = "SELECT f.* FROM features f " +
            "WHERE f.layer_id = :layerId " +
            "AND ST_DWithin(geography(f.geom), geography(:center), :radiusMeters) " +
            "AND f.deleted_at IS NULL", nativeQuery = true)
    List<Feature> findFeaturesInBuffer(@Param("layerId") UUID layerId, 
                                       @Param("center") Point center, 
                                       @Param("radiusMeters") double radiusMeters);
    
    @Query(value = "SELECT f.* FROM features f " +
            "WHERE f.layer_id = :layerId " +
            "AND f.geom && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326) " +
            "AND f.deleted_at IS NULL", nativeQuery = true)
    List<Feature> findFeaturesInBbox(@Param("layerId") UUID layerId,
                                     @Param("minLng") double minLng,
                                     @Param("minLat") double minLat,
                                     @Param("maxLng") double maxLng,
                                     @Param("maxLat") double maxLat);
}

