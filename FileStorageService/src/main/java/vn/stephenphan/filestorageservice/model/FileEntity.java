package vn.stephenphan.filestorageservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "files", indexes = {
        @Index(name = "idx_owner", columnList = "owner_id"),
        @Index(name = "idx_bucket", columnList = "bucket_name"),
        @Index(name = "idx_service", columnList = "service_name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFilename;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "bucket_name", nullable = false)
    private String bucketName;

    @Column(name = "owner_id", nullable = false)
    private String ownerId; // User ID who uploaded the file

    @Column(name = "service_name", nullable = false)
    private String serviceName; // Which service requested the upload (users, memes, etc.)

    @Column(name = "public_url")
    private String publicUrl;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
