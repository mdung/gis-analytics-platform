package com.example.gis.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinIOService {
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public String uploadFile(MultipartFile file) throws Exception {
        String fileKey = UUID.randomUUID().toString() + "/" + file.getOriginalFilename();
        
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(fileKey)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
        );
        
        return fileKey;
    }

    public InputStream downloadFile(String fileKey) throws Exception {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .object(fileKey)
                .build()
        );
    }

    public void deleteFile(String fileKey) throws Exception {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(fileKey)
                .build()
        );
    }

    public String getFileUrl(String fileKey) {
        return String.format("/api/uploads/download/%s", fileKey);
    }
}

