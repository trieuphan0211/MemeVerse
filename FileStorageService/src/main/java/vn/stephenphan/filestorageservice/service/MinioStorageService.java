package vn.stephenphan.filestorageservice.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Log4j2
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${storage.presigned-url-expiry:15}")
    private int presignedUrlExpiry;

    /**
     * Ensure bucket exists, create if not
     */
    public void ensureBucketExists(String bucketName) throws Exception {
        boolean found = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
        if (!found) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
            log.info("Created bucket: {}", bucketName);
        }
    }

    /**
     * Upload file to MinIO
     */
    public String uploadFile(String bucketName, String objectName, MultipartFile file) throws Exception {
        ensureBucketExists(bucketName);

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        log.info("Uploaded file to {}/{}", bucketName, objectName);
        return objectName;
    }

    /**
     * Delete file from MinIO
     */
    public void deleteFile(String bucketName, String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
        log.info("Deleted file from {}/{}", bucketName, objectName);
    }

    /**
     * Generate presigned URL for file access
     */
    public String generatePresignedUrl(String bucketName, String objectName, Method method) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .method(method)
                        .expiry(presignedUrlExpiry, TimeUnit.MINUTES)
                        .build()
        );
    }

    /**
     * Generate public URL for file
     */
    public String generatePublicUrl(String bucketName, String objectName) {
        return String.format("%s/%s/%s", minioEndpoint, bucketName, objectName);
    }

    /**
     * Get file as bytes
     */
    public byte[] getFileBytes(String bucketName, String objectName) throws Exception {
        var object = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
        return object.readAllBytes();
    }

    /**
     * Check if object exists
     */
    public boolean objectExists(String bucketName, String objectName) throws Exception {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Generate unique filename
     */
    public String generateUniqueFilename(String originalFilename) {
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }
}
