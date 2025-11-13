package com.example.gis.dto;

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
public class UploadDto {
    private UUID id;
    private UUID userId;
    private UUID layerId;
    private String fileName;
    private Long fileSize;
    private String status;
    private String message;
    private Object stats;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

