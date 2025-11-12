package com.example.gis.repository;

import com.example.gis.entity.Geofence;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GeofenceRepository extends JpaRepository<Geofence, UUID> {
    List<Geofence> findByActiveTrueAndDeletedAtIsNull();
    
    @Query(value = "SELECT g.* FROM geofences g " +
            "WHERE g.active = true " +
            "AND ST_Contains(g.geom, :point) " +
            "AND g.deleted_at IS NULL", nativeQuery = true)
    List<Geofence> findContainingGeofences(@Param("point") Point point);
}

