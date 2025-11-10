package com.rapidphotoupload.infrastructure.persistence.entity;

import com.rapidphotoupload.domain.model.UploadStatus;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity for Photo.
 */
@Entity
@Table(name = "photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhotoEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "upload_session_id")
    private UUID uploadSessionId;
    
    @Column(name = "s3_key", nullable = false)
    private String s3Key;
    
    @Column(name = "s3_bucket", nullable = false)
    private String s3Bucket;
    
    @Column(name = "s3_etag")
    private String s3Etag;
    
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;
    
    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;
    
    @Column(name = "mime_type", nullable = false)
    private String mimeType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false)
    private UploadStatus uploadStatus;
    
    @Column(name = "multipart_upload_id")
    private String multipartUploadId;
    
    @Column(name = "upload_expires_at")
    private Instant uploadExpiresAt;
    
    @Type(JsonType.class)
    @Column(name = "tags", columnDefinition = "jsonb")
    private List<String> tags = new ArrayList<>();
    
    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();
    
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Type(JsonType.class)
    @Column(name = "thumbnail_variants", columnDefinition = "jsonb")
    private Map<String, String> thumbnailVariants = new HashMap<>();

    @Column(name = "thumbnail_fallback_url")
    private String thumbnailFallbackUrl;

    @Column(name = "placeholder_url")
    private String placeholderUrl;

    @Column(name = "placeholder_fallback_url")
    private String placeholderFallbackUrl;

    @Column(name = "placeholder_base64", columnDefinition = "text")
    private String placeholderBase64;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    private Long version;
}
