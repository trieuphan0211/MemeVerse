package vn.stephenphan.filestorageservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.stephenphan.filestorageservice.config.StorageProperties;
import vn.stephenphan.filestorageservice.model.FileEntity;
import vn.stephenphan.filestorageservice.model.FileUploadRequest;
import vn.stephenphan.filestorageservice.model.FileUploadResponse;
import vn.stephenphan.filestorageservice.repository.FileRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Log4j2
public class FileStorageService {

    private final FileRepository fileRepository;
    private final MinioStorageService minioStorageService;
    private final StorageProperties storageProperties;

    /**
     * Upload file
     */
    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file, FileUploadRequest request) {
        try {
            // Validate service name
            String serviceName = request.getServiceName();
            if (!storageProperties.getBuckets().containsKey(serviceName)) {
                serviceName = "default";
            }
            // Validate file type
            validateFileType(file, serviceName);

            // Validate file size
            validateFileSize(file, serviceName);

            // Get bucket name
            String bucketName = storageProperties.getBuckets().get(serviceName);

            // Generate unique filename
            String storedFilename = minioStorageService.generateUniqueFilename(file.getOriginalFilename());

            // Upload to MinIO
            minioStorageService.uploadFile(bucketName, storedFilename, file);

            // Generate public URL
            String publicUrl = minioStorageService.generatePublicUrl(bucketName, storedFilename);

            // Generate presigned URL for direct access
            String presignedUrl = minioStorageService.generatePresignedUrl(
                    bucketName, storedFilename, io.minio.http.Method.GET
            );

            // Save metadata to database
            FileEntity fileEntity = FileEntity.builder()
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(storedFilename)
                    .fileType(getFileExtension(file.getOriginalFilename()))
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .bucketName(bucketName)
                    .ownerId(request.getOwnerId())
                    .serviceName(request.getServiceName())
                    .publicUrl(publicUrl)
                    .description(request.getDescription())
                    .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                    .isDeleted(false)
                    .build();

            FileEntity saved = fileRepository.save(fileEntity);

            return FileUploadResponse.builder()
                    .fileId(saved.getId())
                    .originalFilename(saved.getOriginalFilename())
                    .storedFilename(saved.getStoredFilename())
                    .contentType(saved.getContentType())
                    .fileSize(saved.getFileSize())
                    .bucketName(saved.getBucketName())
                    .publicUrl(saved.getPublicUrl())
                    .presignedUrl(presignedUrl)
                    .createdAt(saved.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Get file by ID
     */
    public FileEntity getFile(String fileId) {
        return fileRepository.findByIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    /**
     * Get file by stored filename
     */
    public FileEntity getFileByStoredName(String storedFilename) {
        return fileRepository.findByStoredFilenameAndIsDeletedFalse(storedFilename)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    /**
     * Get presigned URL for file
     */
    public String getPresignedUrl(String fileId) {
        try {
            FileEntity file = getFile(fileId);
            return minioStorageService.generatePresignedUrl(
                    file.getBucketName(), file.getStoredFilename(), io.minio.http.Method.GET
            );
        } catch (Exception e) {
            log.error("Error generating presigned URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate presigned URL");
        }
    }

    /**
     * Get file bytes
     */
    public byte[] getFileBytes(String fileId) {
        try {
            FileEntity file = getFile(fileId);
            return minioStorageService.getFileBytes(file.getBucketName(), file.getStoredFilename());
        } catch (Exception e) {
            log.error("Error getting file bytes: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get file");
        }
    }

    /**
     * Soft delete file
     */
    @Transactional
    public void deleteFile(String fileId, String userId, boolean isAdmin) {
        FileEntity file = fileRepository.findByIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getOwnerId().equals(userId) && !isAdmin) {
            throw new RuntimeException("You don't have permission to delete this file");
        }

        file.setIsDeleted(true);
        file.setDeletedAt(LocalDateTime.now());
        fileRepository.save(file);

        log.info("Soft deleted file: {}", fileId);
    }

    /**
     * Hard delete file (permanent)
     */
    @Transactional
    public void permanentlyDeleteFile(String fileId) {
        try {
            FileEntity file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            // Delete from MinIO
            minioStorageService.deleteFile(file.getBucketName(), file.getStoredFilename());

            // Delete from database
            fileRepository.delete(file);

            log.info("Permanently deleted file: {}", fileId);
        } catch (Exception e) {
            log.error("Error permanently deleting file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file");
        }
    }

    /**
     * Get files by owner
     */
    public Page<FileEntity> getFilesByOwner(String ownerId, Pageable pageable) {
        return fileRepository.findByOwnerIdAndIsDeletedFalseOrderByCreatedAtDesc(ownerId, pageable);
    }

    /**
     * Get files by service
     */
    public Page<FileEntity> getFilesByService(String serviceName, Pageable pageable) {
        return fileRepository.findByServiceNameAndIsDeletedFalseOrderByCreatedAtDesc(serviceName, pageable);
    }

    /**
     * Get files by owner and service
     */
    public Page<FileEntity> getFilesByOwnerAndService(String ownerId, String serviceName, Pageable pageable) {
        return fileRepository.findByOwnerIdAndServiceName(ownerId, serviceName, pageable);
    }

    /**
     * Search files
     */
    public Page<FileEntity> searchFiles(String search, Pageable pageable) {
        return fileRepository.searchFiles(search, pageable);
    }

    /**
     * Get total storage used by owner
     */
    public Long getStorageUsedByOwner(String ownerId) {
        Long total = fileRepository.getTotalStorageUsedByOwner(ownerId);
        return total != null ? total : 0L;
    }

    /**
     * Clean up deleted files (scheduled job)
     */
    @Transactional
    public void cleanupDeletedFiles(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<FileEntity> filesToDelete = fileRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoffDate);

        for (FileEntity file : filesToDelete) {
            try {
                permanentlyDeleteFile(file.getId());
            } catch (Exception e) {
                log.error("Failed to cleanup file {}: {}", file.getId(), e.getMessage());
            }
        }
    }

    // ==================== Helper Methods ====================

    private void validateFileType(MultipartFile file, String serviceName) {
        List<String> allowed = storageProperties.getAllowedTypes().get(serviceName);
        if (allowed == null) {
            allowed = storageProperties.getAllowedTypes().get("default");
        }

        if (allowed != null && !allowed.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null || !allowed.contains(contentType)) {
                throw new RuntimeException("File type not allowed. Allowed types: " + allowed);
            }
        }
    }

    private void validateFileSize(MultipartFile file, String serviceName) {
        Integer maxSize = storageProperties.getMaxSizes().get(serviceName);
        if (maxSize == null) {
            maxSize = 10; // Default 10MB
        }

        long maxBytes = maxSize * 1024 * 1024L;
        if (file.getSize() > maxBytes) {
            throw new RuntimeException("File size exceeds limit of " + maxSize + "MB");
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
}
