package com.example.gis.service;

import io.minio.*;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinIOService {
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    // ========== File Operations ==========

    public String uploadFile(MultipartFile file) throws Exception {
        return uploadFile(bucketName, file);
    }

    public String uploadFile(String bucket, MultipartFile file) throws Exception {
        String fileKey = UUID.randomUUID().toString() + "/" + file.getOriginalFilename();
        
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .object(fileKey)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
        );
        
        return fileKey;
    }

    public InputStream downloadFile(String fileKey) throws Exception {
        return downloadFile(bucketName, fileKey);
    }

    public InputStream downloadFile(String bucket, String fileKey) throws Exception {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucket)
                .object(fileKey)
                .build()
        );
    }

    public void deleteFile(String fileKey) throws Exception {
        deleteFile(bucketName, fileKey);
    }

    public void deleteFile(String bucket, String fileKey) throws Exception {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(fileKey)
                .build()
        );
    }

    public StatObjectResponse getFileMetadata(String fileKey) throws Exception {
        return getFileMetadata(bucketName, fileKey);
    }

    public StatObjectResponse getFileMetadata(String bucket, String fileKey) throws Exception {
        return minioClient.statObject(
            StatObjectArgs.builder()
                .bucket(bucket)
                .object(fileKey)
                .build()
        );
    }

    public List<FileInfo> listFiles(String prefix) throws Exception {
        return listFiles(bucketName, prefix);
    }

    public List<FileInfo> listFiles(String bucket, String prefix) throws Exception {
        Iterable<Result<Item>> results = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucket)
                .prefix(prefix)
                .recursive(true)
                .build()
        );

        List<FileInfo> files = new ArrayList<>();
        for (Result<Item> result : results) {
            Item item = result.get();
            if (!item.isDir()) {
                files.add(FileInfo.builder()
                        .name(item.objectName())
                        .size(item.size())
                        .lastModified(item.lastModified())
                        .contentType(item.contentType())
                        .etag(item.etag())
                        .build());
            }
        }
        return files;
    }

    // ========== Presigned URL Operations ==========

    public String generatePresignedUploadUrl(String fileKey, Duration expiration) throws Exception {
        return generatePresignedUploadUrl(bucketName, fileKey, expiration);
    }

    public String generatePresignedUploadUrl(String bucket, String fileKey, Duration expiration) throws Exception {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(bucket)
                .object(fileKey)
                .expiry((int) expiration.getSeconds())
                .build()
        );
    }

    public String generatePresignedDownloadUrl(String fileKey, Duration expiration) throws Exception {
        return generatePresignedDownloadUrl(bucketName, fileKey, expiration);
    }

    public String generatePresignedDownloadUrl(String bucket, String fileKey, Duration expiration) throws Exception {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(fileKey)
                .expiry((int) expiration.getSeconds())
                .build()
        );
    }

    public String generatePresignedDeleteUrl(String fileKey, Duration expiration) throws Exception {
        return generatePresignedDeleteUrl(bucketName, fileKey, expiration);
    }

    public String generatePresignedDeleteUrl(String bucket, String fileKey, Duration expiration) throws Exception {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.DELETE)
                .bucket(bucket)
                .object(fileKey)
                .expiry((int) expiration.getSeconds())
                .build()
        );
    }

    // ========== Bucket Management ==========

    public List<Bucket> listBuckets() throws Exception {
        return minioClient.listBuckets();
    }

    public boolean bucketExists(String bucketName) throws Exception {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    public void createBucket(String bucketName) throws Exception {
        if (!bucketExists(bucketName)) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Created bucket: {}", bucketName);
        } else {
            log.warn("Bucket already exists: {}", bucketName);
        }
    }

    public void deleteBucket(String bucketName) throws Exception {
        if (bucketExists(bucketName)) {
            // Check if bucket is empty
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
            
            boolean hasItems = StreamSupport.stream(results.spliterator(), false)
                    .anyMatch(result -> {
                        try {
                            return !result.get().isDir();
                        } catch (Exception e) {
                            return false;
                        }
                    });
            
            if (hasItems) {
                throw new IllegalStateException("Cannot delete non-empty bucket: " + bucketName);
            }
            
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
            log.info("Deleted bucket: {}", bucketName);
        } else {
            log.warn("Bucket does not exist: {}", bucketName);
        }
    }

    public void deleteBucketForce(String bucketName) throws Exception {
        if (bucketExists(bucketName)) {
            // Delete all objects first
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .recursive(true)
                    .build()
            );
            
            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    minioClient.removeObject(
                        RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(item.objectName())
                            .build()
                    );
                }
            }
            
            // Then delete bucket
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
            log.info("Force deleted bucket: {}", bucketName);
        }
    }

    public String getFileUrl(String fileKey) {
        return String.format("/api/minio/files/%s", fileKey);
    }

    // ========== DTOs ==========

    @lombok.Data
    @lombok.Builder
    public static class FileInfo {
        private String name;
        private Long size;
        private java.time.ZonedDateTime lastModified;
        private String contentType;
        private String etag;
    }
}

