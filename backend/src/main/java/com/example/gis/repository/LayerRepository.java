package com.example.gis.repository;

import com.example.gis.entity.Layer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LayerRepository extends JpaRepository<Layer, UUID> {
    Optional<Layer> findByCodeAndDeletedAtIsNull(String code);
    boolean existsByCodeAndDeletedAtIsNull(String code);
}

