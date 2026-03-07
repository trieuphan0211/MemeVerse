package vn.stephenphan.memeservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.stephenphan.memeservice.model.*;
import vn.stephenphan.memeservice.service.MemeService;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Log4j2
public class MemeController {

    private final MemeService memeService;

    // ==================== Public Endpoints ====================

    @GetMapping("/memes/public/feed")
    public ResponseEntity<Page<Meme>> getPublicFeed(
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Meme> result;
        switch (sort.toLowerCase()) {
            case "trending":
                result = memeService.getTrendingMemes(pageable);
                break;
            case "hot":
                result = memeService.getHotMemes(pageable);
                break;
            case "top":
                result = memeService.getTopMemes("day", pageable);
                break;
            default: // latest
                result = memeService.getLatestMemes(pageable);
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/memes/public/latest")
    public ResponseEntity<Page<Meme>> getLatestMemes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(memeService.getLatestMemes(pageable));
    }

    @GetMapping("/memes/public/trending")
    public ResponseEntity<Page<Meme>> getTrendingMemes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(memeService.getTrendingMemes(pageable));
    }

    @GetMapping("/memes/public/hot")
    public ResponseEntity<Page<Meme>> getHotMemes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(memeService.getHotMemes(pageable));
    }

    @GetMapping("/memes/public/top")
    public ResponseEntity<Page<Meme>> getTopMemes(
            @RequestParam(defaultValue = "day") String period,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(memeService.getTopMemes(period, pageable));
    }

    @GetMapping("/memes/public/search")
    public ResponseEntity<Page<Meme>> searchMemes(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(memeService.searchMemes(q, pageable));
    }

    @GetMapping("/memes/public/tag/{tag}")
    public ResponseEntity<Page<Meme>> getMemesByTag(
            @PathVariable String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(memeService.getMemesByTag(tag, pageable));
    }

    @GetMapping("/memes/public/{memeId}")
    public ResponseEntity<Meme> getMemeById(@PathVariable String memeId) {
        return ResponseEntity.ok(memeService.getMemeById(memeId));
    }

    @GetMapping("/memes/public/user/{userId}")
    public ResponseEntity<Page<Meme>> getUserMemes(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(memeService.getUserMemes(userId, pageable));
    }

    @GetMapping("/memes/public/tags/trending")
    public ResponseEntity<List<Tag>> getTrendingTags(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(memeService.getTrendingTags(limit));
    }

    // ==================== Authenticated Endpoints ====================

    @GetMapping("/memes/following")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Page<Meme>> getFollowingFeed(
            @RequestParam List<String> followingIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(memeService.getFollowingMemes(followingIds, pageable));
    }

    @PostMapping(value = "/memes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Meme> createMeme(
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        String userId = getUserIdFromAuth(authentication);
        String username = getUsernameFromAuth(authentication);
        
        Meme meme = memeService.createMeme(userId, username, caption, tags, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(meme);
    }

    @PutMapping(value = "/memes/{memeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Meme> updateMeme(
            @PathVariable String memeId,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication) {
        
        String userId = getUserIdFromAuth(authentication);
        
        Meme meme = memeService.updateMeme(memeId, userId, caption, tags, file);
        return ResponseEntity.ok(meme);
    }

    @DeleteMapping("/memes/{memeId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, String>> deleteMeme(
            @PathVariable String memeId,
            Authentication authentication) {
        
        String userId = getUserIdFromAuth(authentication);
        boolean isAdmin = isAdmin(authentication);
        
        memeService.deleteMeme(memeId, userId, isAdmin);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Meme deleted successfully");
        return ResponseEntity.ok(response);
    }

    // ==================== Vote System ====================

    @PostMapping("/memes/{memeId}/upvote")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Meme> upvoteMeme(
            @PathVariable String memeId,
            Authentication authentication) {
        
        String userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(memeService.voteMeme(memeId, userId, VoteType.UP));
    }

    @PostMapping("/memes/{memeId}/downvote")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Meme> downvoteMeme(
            @PathVariable String memeId,
            Authentication authentication) {
        
        String userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(memeService.voteMeme(memeId, userId, VoteType.DOWN));
    }

    @GetMapping("/memes/{memeId}/vote")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, Object>> getUserVote(
            @PathVariable String memeId,
            Authentication authentication) {
        
        String userId = getUserIdFromAuth(authentication);
        Optional<VoteType> vote = memeService.getUserVote(memeId, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasVoted", vote.isPresent());
        response.put("voteType", vote.map(Enum::name).orElse(null));
        return ResponseEntity.ok(response);
    }

    // ==================== Comment System ====================

    @GetMapping("/memes/{memeId}/comments")
    public ResponseEntity<Page<MemeComment>> getComments(
            @PathVariable String memeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(memeService.getComments(memeId, pageable));
    }

    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<List<MemeComment>> getReplies(@PathVariable String commentId) {
        return ResponseEntity.ok(memeService.getReplies(commentId));
    }

    @PostMapping("/memes/{memeId}/comments")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<MemeComment> addComment(
            @PathVariable String memeId,
            @RequestParam("content") String content,
            @RequestParam(value = "parentId", required = false) String parentId,
            Authentication authentication) {
        
        String userId = getUserIdFromAuth(authentication);
        String username = getUsernameFromAuth(authentication);
        
        MemeComment comment = memeService.addComment(memeId, userId, username, content, parentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, String>> deleteComment(
            @PathVariable String commentId,
            Authentication authentication) {
        
        String userId = getUserIdFromAuth(authentication);
        boolean isAdmin = isAdmin(authentication);
        
        memeService.deleteComment(commentId, userId, isAdmin);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Comment deleted successfully");
        return ResponseEntity.ok(response);
    }

    // ==================== Report System ====================

    @PostMapping("/memes/{memeId}/report")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Report> reportMeme(
            @PathVariable String memeId,
            @RequestParam("reason") ReportReason reason,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) {
        
        String userId = getUserIdFromAuth(authentication);
        Report report = memeService.reportMeme(memeId, userId, reason, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    // ==================== Admin Endpoints ====================

    @GetMapping("/memes/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Meme>> getPendingMemes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(memeService.getPendingMemes(pageable));
    }

    @PostMapping("/memes/{memeId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Meme> approveMeme(@PathVariable String memeId) {
        return ResponseEntity.ok(memeService.approveMeme(memeId));
    }

    @PostMapping("/memes/{memeId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Meme> rejectMeme(@PathVariable String memeId) {
        return ResponseEntity.ok(memeService.rejectMeme(memeId));
    }

    @GetMapping("/memes/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Report>> getPendingReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(memeService.getPendingReports(pageable));
    }

    @PostMapping("/reports/{reportId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> resolveReport(
            @PathVariable String reportId,
            @RequestParam(defaultValue = "true") boolean approved) {
        
        memeService.resolveReport(reportId, approved);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Report resolved successfully");
        return ResponseEntity.ok(response);
    }

    // ==================== Helper Methods ====================

    private String getUserIdFromAuth(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject();
        }
        return authentication.getName();
    }

    private String getUsernameFromAuth(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getClaimAsString("preferred_username");
        }
        return authentication.getName();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}
