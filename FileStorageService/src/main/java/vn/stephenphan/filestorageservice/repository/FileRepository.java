package vn.stephenphan.filestorageservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.stephenphan.filestorageservice.model.FileEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, String> {

    Optional<FileEntity> findByStoredFilenameAndIsDeletedFalse(String storedFilename);

    Optional<FileEntity> findByIdAndIsDeletedFalse(String id);

    Page<FileEntity> findByOwnerIdAndIsDeletedFalseOrderByCreatedAtDesc(String ownerId, Pageable pageable);

    Page<FileEntity> findByServiceNameAndIsDeletedFalseOrderByCreatedAtDesc(String serviceName, Pageable pageable);

    Page<FileEntity> findByBucketNameAndIsDeletedFalseOrderByCreatedAtDesc(String bucketName, Pageable pageable);

    @Query("SELECT f FROM FileEntity f WHERE f.ownerId = :ownerId AND f.serviceName = :serviceName AND f.isDeleted = false")
    Page<FileEntity> findByOwnerIdAndServiceName(@Param("ownerId") String ownerId, @Param("serviceName") String serviceName, Pageable pageable);

    @Query("SELECT SUM(f.fileSize) FROM FileEntity f WHERE f.ownerId = :ownerId AND f.isDeleted = false")
    Long getTotalStorageUsedByOwner(@Param("ownerId") String ownerId);

    @Query("SELECT f FROM FileEntity f WHERE f.isDeleted = false AND " +
           "(:search IS NULL OR LOWER(f.originalFilename) LIKE LOWER(CONCAT('%', :search, '%')))"
    )
    Page<FileEntity> searchFiles(@Param("search") String search, Pageable pageable);

    List<FileEntity> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime date);
}
