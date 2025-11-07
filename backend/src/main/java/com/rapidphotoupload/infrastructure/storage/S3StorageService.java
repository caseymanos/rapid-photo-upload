package com.rapidphotoupload.infrastructure.storage;

import com.rapidphotoupload.application.dto.InitiateUploadResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.time.Duration;
import java.util.ArrayList;
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
    
    @Value("${aws.s3.bucket.name}")
    private String bucketName;
    
    @Value("${aws.s3.presigned-url.expiration-hours:2}")
    private int presignedUrlExpirationHours;
    
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
     * Generate presigned URLs for uploading parts.
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
        
        log.info("Generating {} presigned URLs for key: {}", numberOfParts, s3Key);
        
        List<InitiateUploadResponse.PresignedPartUrl> presignedUrls = new ArrayList<>();
        Duration expiration = Duration.ofHours(presignedUrlExpirationHours);
        
        for (int partNumber = 1; partNumber <= numberOfParts; partNumber++) {
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
            
            presignedUrls.add(new InitiateUploadResponse.PresignedPartUrl(
                partNumber,
                presignedRequest.url().toString()
            ));
        }
        
        log.info("Generated {} presigned URLs", presignedUrls.size());
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
        
        log.info("Multipart upload completed with ETag: {}", response.eTag());
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
    
    public String getBucketName() {
        return bucketName;
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
