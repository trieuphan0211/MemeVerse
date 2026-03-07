package vn.stephenphan.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.stephenphan.userservice.dto.UserPrincipal;
import vn.stephenphan.userservice.model.Follower;
import vn.stephenphan.userservice.model.UserProfile;
import vn.stephenphan.userservice.model.UserStats;
import vn.stephenphan.userservice.service.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Log4j2
public class UserController extends BaseController{

    private final UserService userService;

    // ==================== Current User ====================

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<UserProfile> getCurrentUser() {
        String userId = getUserId();
        return userService.getUserProfile(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> createProfileFromAuth(currentUser()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<UserProfile> updateMyProfile(@RequestBody Map<String, String> updates) {
        
        String userId = getUserId();
        UserProfile updatedProfile = userService.updateProfile(
                userId,
                updates.get("displayName"),
                updates.get("bio"),
                updates.get("avatarUrl"),
                updates.get("location"),
                updates.get("website")
        );
        return ResponseEntity.ok(updatedProfile);
    }

    @GetMapping("/me/stats")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<UserStats> getMyStats() {
        String userId = getUserId();
        return ResponseEntity.ok(userService.getUserStats(userId));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<UserProfile> syncUser() {
        return createProfileFromAuth(currentUser());
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, String>> deactivateAccount() {
        String userId = getUserId();
        userService.deactivateUser(userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Account deactivated successfully");
        return ResponseEntity.ok(response);
    }

    // ==================== User Profile ====================

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserProfile> getUserById(@PathVariable String userId) {
        return userService.getUserProfile(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/username/{username}")
    public ResponseEntity<UserProfile> getUserByUsername(@PathVariable String username) {
        return userService.getUserProfileByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{userId}/stats")
    public ResponseEntity<UserStats> getUserStats(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserStats(userId));
    }

    @GetMapping("/users/search")
    public ResponseEntity<Page<UserProfile>> searchUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        return ResponseEntity.ok(userService.searchUsers(search, pageable));
    }

    // ==================== Follow System ====================

    @PostMapping("/users/{userId}/follow")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, String>> followUser(
            @PathVariable String userId) {
        
        String currentUserId =  getUserId();;
        userService.followUser(currentUserId, userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Followed successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{userId}/follow")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, String>> unfollowUser(
            @PathVariable String userId) {
        
        String currentUserId = getUserId();
        userService.unfollowUser(currentUserId, userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Unfollowed successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/following")
    public ResponseEntity<Page<Follower>> getUserFollowing(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(userService.getFollowing(userId, pageable));
    }

    @GetMapping("/users/{userId}/followers")
    public ResponseEntity<Page<Follower>> getUserFollowers(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(userService.getFollowers(userId, pageable));
    }

    @GetMapping("/me/following/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, Boolean>> checkFollowing(
            @PathVariable String userId) {
        
        String currentUserId = getUserId();
        boolean isFollowing = userService.isFollowing(currentUserId, userId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("following", isFollowing);
        return ResponseEntity.ok(response);
    }

    // ==================== Block System ====================

    @PostMapping("/users/{userId}/block")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, String>> blockUser(
            @PathVariable String userId) {
        
        String currentUserId = getUserId();
        userService.blockUser(currentUserId, userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "User blocked successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{userId}/block")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, String>> unblockUser(
            @PathVariable String userId) {
        
        String currentUserId = getUserId();
        userService.unblockUser(currentUserId, userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "User unblocked successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/blocked/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, Boolean>> checkBlocked(
            @PathVariable String userId) {
        
        String currentUserId = getUserId();
        boolean isBlocking = userService.isBlocking(currentUserId, userId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("blocking", isBlocking);
        return ResponseEntity.ok(response);
    }

    // ==================== Stats Updates (Internal API) ====================

    @PostMapping("/internal/users/{userId}/meme-count/increment")
    public ResponseEntity<UserStats> incrementMemeCount(@PathVariable String userId) {
        return ResponseEntity.ok(userService.incrementMemeCount(userId));
    }

    @PostMapping("/internal/users/{userId}/meme-count/decrement")
    public ResponseEntity<UserStats> decrementMemeCount(@PathVariable String userId) {
        return ResponseEntity.ok(userService.decrementMemeCount(userId));
    }

    @PostMapping("/internal/users/{userId}/likes-received/add")
    public ResponseEntity<UserStats> addLikeReceived(@PathVariable String userId) {
        return ResponseEntity.ok(userService.addLikeReceived(userId));
    }

    @PostMapping("/internal/users/{userId}/likes-received/remove")
    public ResponseEntity<UserStats> removeLikeReceived(@PathVariable String userId) {
        return ResponseEntity.ok(userService.removeLikeReceived(userId));
    }

    @PostMapping("/internal/users/{userId}/game-score")
    public ResponseEntity<UserStats> addGameScore(
            @PathVariable String userId,
            @RequestParam int points) {
        return ResponseEntity.ok(userService.addGameScore(userId, points));
    }

    // ==================== Helper Methods ====================

    private ResponseEntity<UserProfile> createProfileFromAuth(UserPrincipal user) {

            UserProfile profile = userService.getOrCreateUserProfile(user.userId(), user.fullName(), user.email(), user.givenName(), user.familyName());
            return ResponseEntity.ok(profile);
    }
}
