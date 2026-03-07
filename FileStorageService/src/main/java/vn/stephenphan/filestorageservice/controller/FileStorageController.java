package vn.stephenphan.filestorageservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.stephenphan.filestorageservice.model.FileEntity;
import vn.stephenphan.filestorageservice.model.FileUploadRequest;
import vn.stephenphan.filestorageservice.model.FileUploadResponse;
import vn.stephenphan.filestorageservice.service.FileStorageService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Log4j2
public class FileStorageController {

    private final FileStorageService fileStorageService;

    // ==================== Public Endpoints ====================

    @GetMapping("/files/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        FileEntity file = fileStorageService.getFile(fileId);
        byte[] fileBytes = fileStorageService.getFileBytes(fileId);

        ByteArrayResource resource = new ByteArrayResource(fileBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getFileSize())
                .body(resource);
    }

    @GetMapping("/files/public/{fileId}")
    public ResponseEntity<Map<String, String>> getPublicFileUrl(@PathVariable String fileId) {
        FileEntity file = fileStorageService.getFile(fileId);
        
        Map<String, String> response = new HashMap<>();
        response.put("fileId", file.getId());
        response.put("url", file.getPublicUrl());
        response.put("contentType", file.getContentType());
        return ResponseEntity.ok(response);
    }

    // ==================== Authenticated Endpoints ====================

    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("serviceName") String serviceName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "isPublic", defaultValue = "true") Boolean isPublic,
            Authentication authentication) {

        String userId = getUserIdFromAuth(authentication);

        FileUploadRequest request = FileUploadRequest.builder()
                .serviceName(serviceName)
                .ownerId(userId)
                .description(description)
                .isPublic(isPublic)
                .build();

        FileUploadResponse response = fileStorageService.uploadFile(file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/files/{fileId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<FileEntity> getFileInfo(@PathVariable String fileId) {
        return ResponseEntity.ok(fileStorageService.getFile(fileId));
    }

    @GetMapping("/files/{fileId}/presigned-url")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, String>> getPresignedUrl(@PathVariable String fileId) {
        String presignedUrl = fileStorageService.getPresignedUrl(fileId);
        
        Map<String, String> response = new HashMap<>();
        response.put("presignedUrl", presignedUrl);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/files/{fileId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, String>> deleteFile(
            @PathVariable String fileId,
            Authentication authentication) {

        String userId = getUserIdFromAuth(authentication);
        boolean isAdmin = isAdmin(authentication);

        fileStorageService.deleteFile(fileId, userId, isAdmin);

        Map<String, String> response = new HashMap<>();
        response.put("message", "File deleted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/files/my-files")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Page<FileEntity>> getMyFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        String userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(fileStorageService.getFilesByOwner(userId, pageable));
    }

    @GetMapping("/files/my-files/service/{serviceName}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Page<FileEntity>> getMyFilesByService(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        String userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(fileStorageService.getFilesByOwnerAndService(userId, serviceName, pageable));
    }

    @GetMapping("/files/storage-used")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, Long>> getStorageUsed(Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        Long storageUsed = fileStorageService.getStorageUsedByOwner(userId);

        Map<String, Long> response = new HashMap<>();
        response.put("storageUsedBytes", storageUsed);
        response.put("storageUsedMB", storageUsed / (1024 * 1024));
        return ResponseEntity.ok(response);
    }

    // ==================== Admin Endpoints ====================

    @GetMapping("/files/admin/service/{serviceName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<FileEntity>> getFilesByService(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(fileStorageService.getFilesByService(serviceName, pageable));
    }

    @GetMapping("/files/admin/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<FileEntity>> searchFiles(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(fileStorageService.searchFiles(q, pageable));
    }

    @DeleteMapping("/files/admin/{fileId}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> permanentlyDeleteFile(@PathVariable String fileId) {
        fileStorageService.permanentlyDeleteFile(fileId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "File permanently deleted");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/files/admin/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> cleanupDeletedFiles(
            @RequestParam(defaultValue = "7") int daysOld) {

        fileStorageService.cleanupDeletedFiles(daysOld);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Cleanup completed");
        return ResponseEntity.ok(response);
    }

    // ==================== Internal API (Service-to-Service) ====================

    @PostMapping(value = "/internal/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SERVICE', 'ADMIN')")
    public ResponseEntity<FileUploadResponse> internalUploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("serviceName") String serviceName,
            @RequestParam("ownerId") String ownerId,
            @RequestParam(value = "description", required = false) String description) {

        FileUploadRequest request = FileUploadRequest.builder()
                .serviceName(serviceName)
                .ownerId(ownerId)
                .description(description)
                .isPublic(true)
                .build();

        FileUploadResponse response = fileStorageService.uploadFile(file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/internal/files/{fileId}")
    @PreAuthorize("hasAnyRole('SERVICE', 'ADMIN')")
    public ResponseEntity<FileEntity> internalGetFile(@PathVariable String fileId) {
        return ResponseEntity.ok(fileStorageService.getFile(fileId));
    }

    @DeleteMapping("/internal/files/{fileId}")
    @PreAuthorize("hasAnyRole('SERVICE', 'ADMIN')")
    public ResponseEntity<Map<String, String>> internalDeleteFile(@PathVariable String fileId) {
        fileStorageService.permanentlyDeleteFile(fileId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "File deleted successfully");
        return ResponseEntity.ok(response);
    }

    // ==================== Helper Methods ====================

    private String getUserIdFromAuth(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject();
        }
        return authentication.getName();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}
