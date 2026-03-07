package vn.stephenphan.filestorageservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadRequest {
    private String serviceName; // e.g., "users", "memes"
    private String ownerId;     // User ID
    private String description;
    private Boolean isPublic;
}
