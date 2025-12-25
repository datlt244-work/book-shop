package com.ecommerce.common.config;

import io.minio.MinioClient;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO configuration for object storage.
 * Enabled when minio.endpoint property is set.
 * All services can use this shared configuration.
 */
@Configuration
@ConditionalOnProperty(name = "minio.endpoint")
public class MinioConfig {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${minio.bucket:book-shop}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public MinioProperties minioProperties() {
        return new MinioProperties(endpoint, bucket);
    }

    @Getter
    public static class MinioProperties {
        private final String endpoint;
        private final String bucket;

        public MinioProperties(String endpoint, String bucket) {
            this.endpoint = endpoint;
            this.bucket = bucket;
        }
    }
}
