package com.example.gis.repository;

import com.example.gis.entity.Upload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UploadRepository extends JpaRepository<Upload, UUID> {
    List<Upload> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Upload> findByLayerIdOrderByCreatedAtDesc(UUID layerId);
}

