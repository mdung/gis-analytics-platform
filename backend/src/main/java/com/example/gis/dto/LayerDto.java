package com.example.gis.dto;

import com.example.gis.entity.Layer;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LayerDto {
    private UUID id;
    private String code;
    private String name;
    private Layer.GeometryType geomType;
    private Integer srid;
    private Object style;
    private Object metadata;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

