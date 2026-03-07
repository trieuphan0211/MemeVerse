package vn.stephenphan.filestorageservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponse {
    private String fileId;
    private String originalFilename;
    private String storedFilename;
    private String contentType;
    private Long fileSize;
    private String bucketName;
    private String publicUrl;
    private String presignedUrl;
    private LocalDateTime createdAt;
}
