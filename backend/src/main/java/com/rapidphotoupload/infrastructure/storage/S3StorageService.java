package com.rapidphotoupload.infrastructure.storage;

import com.rapidphotoupload.application.dto.InitiateUploadResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for interacting with AWS S3.
 * Handles multipart upload operations with presigned URLs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private Cache<String, CachedDownloadUrl> downloadUrlCache;
    
    @Value("${aws.s3.bucket.name}")
    private String bucketName;
    
    @Value("${aws.s3.presigned-url.expiration-hours:2}")
    private int presignedUrlExpirationHours;
    
    @Value("${aws.s3.presigned-url.cache.max-size:2000}")
    private long downloadUrlCacheMaxSize;
    
    @Value("${aws.s3.presigned-url.cache.buffer-seconds:60}")
    private long cacheBufferSeconds;

    @PostConstruct
    void initializeCaches() {
        rebuildDownloadCache();
        log.info("Initialized S3 download URL cache (maxSize={}, ttl={}h)",
            downloadUrlCacheMaxSize,
            presignedUrlExpirationHours);
    }

    private synchronized void rebuildDownloadCache() {
        this.downloadUrlCache = Caffeine.newBuilder()
            .maximumSize(Math.max(100, downloadUrlCacheMaxSize))
            .expireAfterWrite(Duration.ofHours(presignedUrlExpirationHours))
            .build();
    }

    private Cache<String, CachedDownloadUrl> downloadCache() {
        if (downloadUrlCache == null) {
            rebuildDownloadCache();
        }
        return downloadUrlCache;
    }
    
    /**
     * Initiate a multipart upload.
     * @param s3Key the S3 object key
     * @param contentType the MIME type
     * @return the multipart upload ID
     */
    @CircuitBreaker(name = "s3", fallbackMethod = "initiateMultipartUploadFallback")
    @Retry(name = "s3")
    public String initiateMultipartUpload(String s3Key, String contentType) {
        log.info("Initiating multipart upload for key: {}", s3Key);
        
        CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .contentType(contentType)
            .build();
        
        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(request);
        
        log.info("Multipart upload initiated with ID: {}", response.uploadId());
        return response.uploadId();
    }
    
    /**
     * Generate a presigned PUT URL for small uploads.
     */
    @CircuitBreaker(name = "s3", fallbackMethod = "generatePresignedPutObjectUrlFallback")
    @Retry(name = "s3")
    public String generatePresignedPutObjectUrl(String s3Key, String contentType) {
        Duration expiration = Duration.ofHours(presignedUrlExpirationHours);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .contentType(contentType)
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .putObjectRequest(putObjectRequest)
            .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    /**
     * Generate presigned URLs for uploading parts.
     * Uses parallel streams for improved performance when generating multiple URLs.
     * @param s3Key the S3 object key
     * @param uploadId the multipart upload ID
     * @param numberOfParts total number of parts
     * @return list of presigned URLs for each part
     */
    @CircuitBreaker(name = "s3", fallbackMethod = "generatePresignedUploadUrlsFallback")
    public List<InitiateUploadResponse.PresignedPartUrl> generatePresignedUploadUrls(
            String s3Key, 
            String uploadId, 
            int numberOfParts) {
        
        long startTime = System.currentTimeMillis();
        log.info("Generating {} presigned URLs for key: {}", numberOfParts, s3Key);
        
        Duration expiration = Duration.ofHours(presignedUrlExpirationHours);
        
        // Parallelize URL generation for better performance
        List<InitiateUploadResponse.PresignedPartUrl> presignedUrls = java.util.stream.IntStream
            .rangeClosed(1, numberOfParts)
            .parallel()
            .mapToObj(partNumber -> {
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();
                
                UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(expiration)
                    .uploadPartRequest(uploadPartRequest)
                    .build();
                
                PresignedUploadPartRequest presignedRequest = s3Presigner.presignUploadPart(presignRequest);
                
                return new InitiateUploadResponse.PresignedPartUrl(
                    partNumber,
                    presignedRequest.url().toString()
                );
            })
            .collect(Collectors.toList());
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Generated {} presigned URLs in {}ms (parallel)", presignedUrls.size(), duration);
        return presignedUrls;
    }
    
    /**
     * Complete a multipart upload.
     * @param s3Key the S3 object key
     * @param uploadId the multipart upload ID
     * @param parts the uploaded parts with ETags
     * @return the final ETag of the completed object
     */
    @CircuitBreaker(name = "s3", fallbackMethod = "completeMultipartUploadFallback")
    @Retry(name = "s3")
    public String completeMultipartUpload(String s3Key, String uploadId, List<CompletedPart> parts) {
        long startTime = System.currentTimeMillis();
        log.info("Completing multipart upload for key: {} with {} parts", s3Key, parts.size());
        
        List<software.amazon.awssdk.services.s3.model.CompletedPart> s3Parts = parts.stream()
            .map(part -> software.amazon.awssdk.services.s3.model.CompletedPart.builder()
                .partNumber(part.getPartNumber())
                .eTag(part.getEtag())
                .build())
            .collect(Collectors.toList());
        
        CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
            .parts(s3Parts)
            .build();
        
        CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .uploadId(uploadId)
            .multipartUpload(completedUpload)
            .build();
        
        CompleteMultipartUploadResponse response = s3Client.completeMultipartUpload(request);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Multipart upload completed with ETag: {} in {}ms", response.eTag(), duration);
        return response.eTag();
    }
    
    /**
     * Abort a multipart upload.
     * @param s3Key the S3 object key
     * @param uploadId the multipart upload ID
     */
    @CircuitBreaker(name = "s3", fallbackMethod = "abortMultipartUploadFallback")
    @Retry(name = "s3")
    public void abortMultipartUpload(String s3Key, String uploadId) {
        log.info("Aborting multipart upload for key: {}", s3Key);
        
        AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .uploadId(uploadId)
            .build();
        
        s3Client.abortMultipartUpload(request);
        
        log.info("Multipart upload aborted");
    }
    
    /**
     * Delete an object from S3.
     * @param s3Key the S3 object key
     */
    @CircuitBreaker(name = "s3", fallbackMethod = "deleteObjectFallback")
    @Retry(name = "s3")
    public void deleteObject(String s3Key) {
        log.info("Deleting object with key: {}", s3Key);
        
        DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .build();
        
        s3Client.deleteObject(request);
        
        log.info("Object deleted");
    }
    
    /**
     * Generate a presigned download URL for an S3 object.
     * @param s3Key the S3 object key
     * @return the presigned download URL
     */
    @CircuitBreaker(name = "s3", fallbackMethod = "generatePresignedDownloadUrlFallback")
    @Retry(name = "s3")
    public String generatePresignedDownloadUrl(String s3Key) {
        log.debug("Generating presigned download URL for key: {}", s3Key);

        CachedDownloadUrl cached = downloadCache().getIfPresent(s3Key);
        if (isCachedUrlValid(cached)) {
            log.trace("Using cached presigned download URL for key: {}", s3Key);
            return cached.url();
        }

        Duration expiration = Duration.ofHours(presignedUrlExpirationHours);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .getObjectRequest(getObjectRequest)
            .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

        String presignedUrl = presignedRequest.url().toString();
        Instant expiresAt = Instant.now().plus(expiration);
        downloadCache().put(s3Key, new CachedDownloadUrl(presignedUrl, expiresAt));
        log.debug("Generated presigned download URL (expires in {} hours)", presignedUrlExpirationHours);

        return presignedUrl;
    }

    public String getBucketName() {
        return bucketName;
    }

    private boolean isCachedUrlValid(CachedDownloadUrl cached) {
        if (cached == null) {
            return false;
        }
        Instant safetyCutoff = Instant.now().plusSeconds(Math.max(0, cacheBufferSeconds));
        return cached.expiresAt().isAfter(safetyCutoff);
    }
    
    // Fallback methods
    
    private String initiateMultipartUploadFallback(String s3Key, String contentType, Throwable throwable) {
        log.error("Failed to initiate multipart upload for key: {}", s3Key, throwable);
        throw new RuntimeException("S3 service unavailable", throwable);
    }
    
    private List<InitiateUploadResponse.PresignedPartUrl> generatePresignedUploadUrlsFallback(
            String s3Key, String uploadId, int numberOfParts, Throwable throwable) {
        log.error("Failed to generate presigned URLs for key: {}", s3Key, throwable);
        throw new RuntimeException("S3 service unavailable", throwable);
    }

    private String generatePresignedPutObjectUrlFallback(String s3Key, String contentType, Throwable throwable) {
        log.error("Failed to generate simple upload URL for key: {}", s3Key, throwable);
        throw new RuntimeException("S3 service unavailable", throwable);
    }
    
    private String completeMultipartUploadFallback(String s3Key, String uploadId, List<CompletedPart> parts, Throwable throwable) {
        log.error("Failed to complete multipart upload for key: {}", s3Key, throwable);
        throw new RuntimeException("S3 service unavailable", throwable);
    }
    
    private void abortMultipartUploadFallback(String s3Key, String uploadId, Throwable throwable) {
        log.error("Failed to abort multipart upload for key: {}", s3Key, throwable);
        throw new RuntimeException("S3 service unavailable", throwable);
    }
    
    private void deleteObjectFallback(String s3Key, Throwable throwable) {
        log.error("Failed to delete object with key: {}", s3Key, throwable);
        throw new RuntimeException("S3 service unavailable", throwable);
    }

    private String generatePresignedDownloadUrlFallback(String s3Key, Throwable throwable) {
        log.error("Failed to generate presigned download URL for key: {}", s3Key, throwable);
        return null; // Return null instead of throwing to allow graceful degradation
    }
    
    private record CachedDownloadUrl(String url, Instant expiresAt) {}
    
    /**
     * Represents a completed part in a multipart upload.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompletedPart {
        private int partNumber;
        private String etag;
    }
}
