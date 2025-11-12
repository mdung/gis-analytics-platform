package com.example.gis.service;

import com.example.gis.dto.LayerDto;
import com.example.gis.entity.Layer;
import com.example.gis.entity.User;
import com.example.gis.repository.LayerRepository;
import com.example.gis.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LayerService {
    private final LayerRepository layerRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public Page<LayerDto> findAll(Pageable pageable) {
        return layerRepository.findAll(pageable)
                .map(this::toDto);
    }

    public LayerDto findById(UUID id) {
        Layer layer = layerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Layer not found"));
        return toDto(layer);
    }

    public LayerDto findByCode(String code) {
        Layer layer = layerRepository.findByCodeAndDeletedAtIsNull(code)
                .orElseThrow(() -> new RuntimeException("Layer not found"));
        return toDto(layer);
    }

    @Transactional
    public LayerDto create(LayerDto dto) {
        if (layerRepository.existsByCodeAndDeletedAtIsNull(dto.getCode())) {
            throw new RuntimeException("Layer code already exists");
        }
        
        Layer layer = Layer.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .geomType(dto.getGeomType())
                .srid(dto.getSrid() != null ? dto.getSrid() : 4326)
                .style(objectMapper.valueToTree(dto.getStyle()).toString())
                .metadata(objectMapper.valueToTree(dto.getMetadata()).toString())
                .createdBy(getCurrentUser())
                .build();
        
        return toDto(layerRepository.save(layer));
    }

    @Transactional
    public LayerDto update(UUID id, LayerDto dto) {
        Layer layer = layerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Layer not found"));
        
        layer.setName(dto.getName());
        if (dto.getStyle() != null) {
            layer.setStyle(objectMapper.valueToTree(dto.getStyle()).toString());
        }
        if (dto.getMetadata() != null) {
            layer.setMetadata(objectMapper.valueToTree(dto.getMetadata()).toString());
        }
        layer.setUpdatedBy(getCurrentUser());
        
        return toDto(layerRepository.save(layer));
    }

    @Transactional
    public void delete(UUID id) {
        Layer layer = layerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Layer not found"));
        layer.setDeletedAt(java.time.OffsetDateTime.now());
        layerRepository.save(layer);
    }

    private LayerDto toDto(Layer layer) {
        try {
            return LayerDto.builder()
                    .id(layer.getId())
                    .code(layer.getCode())
                    .name(layer.getName())
                    .geomType(layer.getGeomType())
                    .srid(layer.getSrid())
                    .style(objectMapper.readValue(layer.getStyle(), Object.class))
                    .metadata(objectMapper.readValue(layer.getMetadata(), Object.class))
                    .createdAt(layer.getCreatedAt())
                    .updatedAt(layer.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error converting layer to DTO", e);
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return userRepository.findByUsernameAndDeletedAtIsNull(auth.getName())
                    .orElse(null);
        }
        return null;
    }
}

