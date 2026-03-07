package vn.stephenphan.memeservice.service;

import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.stephenphan.memeservice.model.*;
import vn.stephenphan.memeservice.repository.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class MemeService {

    private final MemeRepository memeRepository;
    private final MemeVoteRepository memeVoteRepository;
    private final MemeCommentRepository memeCommentRepository;
    private final TagRepository tagRepository;
    private final ReportRepository reportRepository;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    // ==================== Meme CRUD ====================

    @Transactional
    public Meme createMeme(String userId, String username, String caption, 
                          List<String> tagNames, MultipartFile file) {
        try {
            // Ensure bucket exists
            ensureBucketExists();

            // Generate unique file name
            String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

            // Upload to MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Build file URL
            String fileUrl = String.format("%s/%s/%s", 
                    minioEndpoint, bucketName, fileName);

            // Create meme entity
            Meme meme = Meme.builder()
                    .userId(userId)
                    .username(username)
                    .imageUrl(fileUrl)
                    .fileName(fileName)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .caption(caption)
                    .status(MemeStatus.APPROVED) // Auto-approve for now
                    .build();

            // Process tags
            if (tagNames != null && !tagNames.isEmpty()) {
                Set<Tag> tags = processTags(tagNames);
                meme.setTags(tags);
            }

            return memeRepository.save(meme);

        } catch (Exception e) {
            log.error("Error creating meme: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create meme: " + e.getMessage());
        }
    }

    @Transactional
    public Meme updateMeme(String memeId, String userId, String caption, 
                          List<String> tagNames, MultipartFile file) {
        Meme meme = memeRepository.findById(memeId)
                .orElseThrow(() -> new RuntimeException("Meme not found"));

        // Check ownership
        if (!meme.getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to update this meme");
        }

        if (caption != null) {
            meme.setCaption(caption);
        }

        // Update tags
        if (tagNames != null) {
            // Decrement old tags usage
            for (Tag tag : meme.getTags()) {
                tag.decrementUsageCount();
                tagRepository.save(tag);
            }
            
            Set<Tag> newTags = processTags(tagNames);
            meme.setTags(newTags);
        }

        // Update file if provided
        if (file != null && !file.isEmpty()) {
            try {
                // Delete old file
                deleteFileFromMinio(meme.getFileName());

                // Upload new file
                String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );

                String fileUrl = String.format("%s/%s/%s",
                        minioEndpoint, bucketName, fileName);

                meme.setImageUrl(fileUrl);
                meme.setFileName(fileName);
                meme.setFileType(file.getContentType());
                meme.setFileSize(file.getSize());

            } catch (Exception e) {
                log.error("Error updating meme file: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to update meme file: " + e.getMessage());
            }
        }

        return memeRepository.save(meme);
    }

    @Transactional
    public void deleteMeme(String memeId, String userId, boolean isAdmin) {
        Meme meme = memeRepository.findById(memeId)
                .orElseThrow(() -> new RuntimeException("Meme not found"));

        // Check ownership or admin
        if (!meme.getUserId().equals(userId) && !isAdmin) {
            throw new RuntimeException("You don't have permission to delete this meme");
        }

        try {
            // Delete file from MinIO
            deleteFileFromMinio(meme.getFileName());

            // Decrement tag usage
            for (Tag tag : meme.getTags()) {
                tag.decrementUsageCount();
                tagRepository.save(tag);
            }

            // Soft delete - mark as DELETED
            meme.setStatus(MemeStatus.DELETED);
            memeRepository.save(meme);

        } catch (Exception e) {
            log.error("Error deleting meme: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete meme: " + e.getMessage());
        }
    }

    public Meme getMemeById(String memeId) {
        Meme meme = memeRepository.findById(memeId)
                .orElseThrow(() -> new RuntimeException("Meme not found"));
        
        // Increment view count
        meme.incrementViewCount();
        return memeRepository.save(meme);
    }

    // ==================== Feed Algorithms ====================

    public Page<Meme> getLatestMemes(Pageable pageable) {
        return memeRepository.findByStatusOrderByCreatedAtDesc(MemeStatus.APPROVED, pageable);
    }

    public Page<Meme> getTrendingMemes(Pageable pageable) {
        return memeRepository.findTrendingMemes(MemeStatus.APPROVED, pageable);
    }

    public Page<Meme> getHotMemes(Pageable pageable) {
        // Using Reddit hot algorithm
        return memeRepository.findHotMemes(MemeStatus.APPROVED, pageable);
    }

    public Page<Meme> getTopMemes(String timePeriod, Pageable pageable) {
        LocalDateTime since;
        switch (timePeriod.toLowerCase()) {
            case "day":
                since = LocalDateTime.now().minusDays(1);
                break;
            case "week":
                since = LocalDateTime.now().minusWeeks(1);
                break;
            case "month":
                since = LocalDateTime.now().minusMonths(1);
                break;
            case "year":
                since = LocalDateTime.now().minusYears(1);
                break;
            default:
                since = LocalDateTime.now().minusDays(1);
        }
        return memeRepository.findTopMemes(MemeStatus.APPROVED, since, pageable);
    }

    public Page<Meme> getFollowingMemes(List<String> followingIds, Pageable pageable) {
        if (followingIds == null || followingIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return memeRepository.findFollowingMemes(followingIds, MemeStatus.APPROVED, pageable);
    }

    // ==================== Search ====================

    public Page<Meme> searchMemes(String search, Pageable pageable) {
        return memeRepository.searchMemes(search, MemeStatus.APPROVED, pageable);
    }

    public Page<Meme> getMemesByTag(String tagName, Pageable pageable) {
        return memeRepository.findByTagName(tagName, MemeStatus.APPROVED, pageable);
    }

    public Page<Meme> getUserMemes(String userId, Pageable pageable) {
        return memeRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, MemeStatus.APPROVED, pageable);
    }

    // ==================== Vote System ====================

    @Transactional
    public Meme voteMeme(String memeId, String userId, VoteType voteType) {
        Meme meme = memeRepository.findById(memeId)
                .orElseThrow(() -> new RuntimeException("Meme not found"));

        Optional<MemeVote> existingVote = memeVoteRepository.findByMemeIdAndUserId(memeId, userId);

        if (existingVote.isPresent()) {
            MemeVote vote = existingVote.get();
            
            if (vote.getVoteType() == voteType) {
                // Remove vote if same type clicked again
                if (voteType == VoteType.UP) {
                    meme.removeUpvote();
                } else {
                    meme.removeDownvote();
                }
                memeVoteRepository.delete(vote);
            } else {
                // Change vote type
                if (voteType == VoteType.UP) {
                    meme.changeDownvoteToUpvote();
                } else {
                    meme.changeUpvoteToDownvote();
                }
                vote.setVoteType(voteType);
                memeVoteRepository.save(vote);
            }
        } else {
            // New vote
            MemeVote newVote = MemeVote.builder()
                    .memeId(memeId)
                    .userId(userId)
                    .voteType(voteType)
                    .build();
            memeVoteRepository.save(newVote);

            if (voteType == VoteType.UP) {
                meme.upvote();
            } else {
                meme.downvote();
            }
        }

        return memeRepository.save(meme);
    }

    public Optional<VoteType> getUserVote(String memeId, String userId) {
        return memeVoteRepository.findByMemeIdAndUserId(memeId, userId)
                .map(MemeVote::getVoteType);
    }

    // ==================== Comment System ====================

    @Transactional
    public MemeComment addComment(String memeId, String userId, String username, 
                                   String content, String parentId) {
        Meme meme = memeRepository.findById(memeId)
                .orElseThrow(() -> new RuntimeException("Meme not found"));

        // Validate parent comment if provided
        if (parentId != null) {
            MemeComment parentComment = memeCommentRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            if (!parentComment.getMemeId().equals(memeId)) {
                throw new RuntimeException("Parent comment does not belong to this meme");
            }
        }

        MemeComment comment = MemeComment.builder()
                .memeId(memeId)
                .userId(userId)
                .username(username)
                .content(content)
                .parentId(parentId)
                .build();

        MemeComment savedComment = memeCommentRepository.save(comment);
        
        // Increment comment count
        meme.incrementCommentCount();
        memeRepository.save(meme);

        return savedComment;
    }

    @Transactional
    public void deleteComment(String commentId, String userId, boolean isAdmin) {
        MemeComment comment = memeCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUserId().equals(userId) && !isAdmin) {
            throw new RuntimeException("You don't have permission to delete this comment");
        }

        Meme meme = memeRepository.findById(comment.getMemeId())
                .orElseThrow(() -> new RuntimeException("Meme not found"));

        memeCommentRepository.delete(comment);
        
        // Decrement comment count
        meme.decrementCommentCount();
        memeRepository.save(meme);
    }

    public Page<MemeComment> getComments(String memeId, Pageable pageable) {
        return memeCommentRepository.findByMemeIdAndParentIdIsNullOrderByCreatedAtDesc(memeId, pageable);
    }

    public List<MemeComment> getReplies(String parentId) {
        return memeCommentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
    }

    // ==================== Tag Management ====================

    @Transactional
    public Set<Tag> processTags(List<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        
        for (String tagName : tagNames) {
            String normalizedTag = tagName.toLowerCase().trim();
            
            Tag tag = tagRepository.findByName(normalizedTag)
                    .orElseGet(() -> {
                        Tag newTag = Tag.builder()
                                .name(normalizedTag)
                                .build();
                        return tagRepository.save(newTag);
                    });
            
            tag.incrementUsageCount();
            tagRepository.save(tag);
            tags.add(tag);
        }
        
        return tags;
    }

    public List<Tag> getTrendingTags(int limit) {
        return tagRepository.findTopByUsageCount(org.springframework.data.domain.PageRequest.of(0, limit));
    }

    public List<Tag> searchTags(String search) {
        return tagRepository.searchByName(search);
    }

    // ==================== Report System ====================

    @Transactional
    public Report reportMeme(String memeId, String reporterId, ReportReason reason, String description) {
        Meme meme = memeRepository.findById(memeId)
                .orElseThrow(() -> new RuntimeException("Meme not found"));

        // Check if already reported by this user
        if (reportRepository.existsByMemeIdAndReporterId(memeId, reporterId)) {
            throw new RuntimeException("You have already reported this meme");
        }

        Report report = Report.builder()
                .memeId(memeId)
                .reporterId(reporterId)
                .reason(reason)
                .description(description)
                .status(ReportStatus.PENDING)
                .build();

        return reportRepository.save(report);
    }

    public Page<Report> getPendingReports(Pageable pageable) {
        return reportRepository.findByStatus(ReportStatus.PENDING, pageable);
    }

    @Transactional
    public void resolveReport(String reportId, boolean approved) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setStatus(approved ? ReportStatus.RESOLVED : ReportStatus.REJECTED);
        reportRepository.save(report);

        if (approved) {
            // Take action on meme (e.g., reject it)
            Meme meme = memeRepository.findById(report.getMemeId())
                    .orElseThrow(() -> new RuntimeException("Meme not found"));
            meme.setStatus(MemeStatus.REJECTED);
            memeRepository.save(meme);
        }
    }

    // ==================== Admin Operations ====================

    @Transactional
    public Meme approveMeme(String memeId) {
        Meme meme = memeRepository.findById(memeId)
                .orElseThrow(() -> new RuntimeException("Meme not found"));
        meme.setStatus(MemeStatus.APPROVED);
        return memeRepository.save(meme);
    }

    @Transactional
    public Meme rejectMeme(String memeId) {
        Meme meme = memeRepository.findById(memeId)
                .orElseThrow(() -> new RuntimeException("Meme not found"));
        meme.setStatus(MemeStatus.REJECTED);
        return memeRepository.save(meme);
    }

    public Page<Meme> getPendingMemes(Pageable pageable) {
        return memeRepository.findByStatus(MemeStatus.PENDING, pageable);
    }

    // ==================== Helper Methods ====================

    private void ensureBucketExists() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Created bucket: {}", bucketName);
        }
    }

    private void deleteFileFromMinio(String fileName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build()
        );
    }
}
