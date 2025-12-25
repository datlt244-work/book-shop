package com.ecommerce.common.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Shared MinIO service for all microservices.
 * Provides common operations: upload, get pre-signed URL, delete.
 * 
 * Usage in any service:
 * - Inject MinioService
 * - Call uploadFile(), getPresignedUrl(), deleteObject()
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(MinioClient.class)
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket:book-shop}")
    private String bucketName;

    @Value("${minio.presigned-url.expiry:7}")
    private int presignedUrlExpiryDays;

    /**
     * Upload a file to MinIO
     * 
     * @param folder   Folder path (e.g., "Avatar", "Products", "Orders")
     * @param fileName Base file name
     * @param file     MultipartFile to upload
     * @return Object path (folder/filename with UUID)
     */
    public String uploadFile(String folder, String fileName, MultipartFile file) {
        try {
            ensureBucketExists();

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";

            String objectName = folder + "/" + fileName + "-" + UUID.randomUUID() + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            log.info("Uploaded file: {}", objectName);
            return objectName;

        } catch (Exception e) {
            log.error("Failed to upload file: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    /**
     * Upload avatar for a user
     * Convenience method that uses "Avatar" folder
     */
    public String uploadAvatar(Integer userId, MultipartFile file) {
        return uploadFile("Avatar", "user-" + userId, file);
    }

    /**
     * Generate pre-signed URL for viewing an object
     * URL will be valid for configured days
     */
    public String getPresignedUrl(String objectPath) {
        if (objectPath == null || objectPath.isEmpty()) {
            return null;
        }

        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectPath)
                            .expiry(presignedUrlExpiryDays, TimeUnit.DAYS)
                            .build());
            log.debug("Generated pre-signed URL for: {}", objectPath);
            return url;

        } catch (Exception e) {
            log.error("Failed to generate pre-signed URL for {}: {}", objectPath, e.getMessage());
            return null;
        }
    }

    /**
     * Delete an object from MinIO
     */
    public void deleteObject(String objectPath) {
        if (objectPath == null || objectPath.isEmpty()) {
            return;
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .build());
            log.info("Deleted object: {}", objectPath);

        } catch (Exception e) {
            log.error("Failed to delete object {}: {}", objectPath, e.getMessage());
        }
    }

    /**
     * Check if bucket exists, create if not
     */
    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build());

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build());
                log.info("Created bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to check/create bucket: {}", e.getMessage());
            throw new RuntimeException("Failed to ensure bucket exists", e);
        }
    }
}
