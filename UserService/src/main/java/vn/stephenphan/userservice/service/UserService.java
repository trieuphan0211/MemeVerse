package vn.stephenphan.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.stephenphan.userservice.model.Block;
import vn.stephenphan.userservice.model.Follower;
import vn.stephenphan.userservice.model.UserProfile;
import vn.stephenphan.userservice.model.UserStats;
import vn.stephenphan.userservice.repository.BlockRepository;
import vn.stephenphan.userservice.repository.FollowerRepository;
import vn.stephenphan.userservice.repository.UserProfileRepository;
import vn.stephenphan.userservice.repository.UserStatsRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserService {

    private final UserProfileRepository userProfileRepository;
    private final UserStatsRepository userStatsRepository;
    private final FollowerRepository followerRepository;
    private final BlockRepository blockRepository;

    // ==================== Profile Management ====================

    @Transactional
    public UserProfile getOrCreateUserProfile(String userId, String username, String email, 
                                              String firstName, String lastName) {
        return userProfileRepository.findById(userId)
                .map(existingProfile -> {
                    // Update user info if changed
                    boolean updated = false;
                    if (!username.equals(existingProfile.getUsername())) {
                        existingProfile.setUsername(username);
                        updated = true;
                    }
                    if (email != null && !email.equals(existingProfile.getEmail())) {
                        existingProfile.setEmail(email);
                        updated = true;
                    }
                    if (firstName != null && !firstName.equals(existingProfile.getFirstName())) {
                        existingProfile.setFirstName(firstName);
                        updated = true;
                    }
                    if (lastName != null && !lastName.equals(existingProfile.getLastName())) {
                        existingProfile.setLastName(lastName);
                        updated = true;
                    }
                    if (updated) {
                        return userProfileRepository.save(existingProfile);
                    }
                    return existingProfile;
                })
                .orElseGet(() -> {
                    // Create new user profile
                    UserProfile newProfile = UserProfile.builder()
                            .userId(userId)
                            .username(username)
                            .email(email)
                            .firstName(firstName)
                            .lastName(lastName)
                            .displayName(firstName != null && lastName != null 
                                    ? firstName + " " + lastName 
                                    : username)
                            .isActive(true)
                            .build();
                    
                    UserProfile saved = userProfileRepository.save(newProfile);
                    
                    // Create empty stats for new user
                    UserStats stats = UserStats.builder()
                            .userId(userId)
                            .build();
                    userStatsRepository.save(stats);
                    
                    return saved;
                });
    }

    public Optional<UserProfile> getUserProfile(String userId) {
        return userProfileRepository.findById(userId);
    }

    public Optional<UserProfile> getUserProfileByUsername(String username) {
        return userProfileRepository.findByUsername(username);
    }

    @Transactional
    public UserProfile updateProfile(String userId, String displayName, String bio, 
                                      String avatarUrl, String location, String website) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        if (displayName != null) {
            profile.setDisplayName(displayName);
        }
        if (bio != null) {
            profile.setBio(bio);
        }
        if (avatarUrl != null) {
            profile.setAvatarUrl(avatarUrl);
        }
        if (location != null) {
            profile.setLocation(location);
        }
        if (website != null) {
            profile.setWebsite(website);
        }

        return userProfileRepository.save(profile);
    }

    @Transactional
    public void deactivateUser(String userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));
        profile.setIsActive(false);
        userProfileRepository.save(profile);
    }

    // ==================== User Stats ====================

    public UserStats getUserStats(String userId) {
        return userStatsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserStats newStats = UserStats.builder()
                            .userId(userId)
                            .build();
                    return userStatsRepository.save(newStats);
                });
    }

    @Transactional
    public UserStats incrementMemeCount(String userId) {
        UserStats stats = getUserStats(userId);
        stats.incrementTotalMemes();
        return userStatsRepository.save(stats);
    }

    @Transactional
    public UserStats decrementMemeCount(String userId) {
        UserStats stats = getUserStats(userId);
        stats.decrementTotalMemes();
        return userStatsRepository.save(stats);
    }

    @Transactional
    public UserStats addLikeReceived(String userId) {
        UserStats stats = getUserStats(userId);
        stats.incrementTotalLikesReceived();
        return userStatsRepository.save(stats);
    }

    @Transactional
    public UserStats removeLikeReceived(String userId) {
        UserStats stats = getUserStats(userId);
        stats.decrementTotalLikesReceived();
        return userStatsRepository.save(stats);
    }

    @Transactional
    public UserStats addGameScore(String userId, int points) {
        UserStats stats = getUserStats(userId);
        stats.addGameScore(points);
        return userStatsRepository.save(stats);
    }

    // ==================== Follow System ====================

    @Transactional
    public void followUser(String followerId, String followingId) {
        if (followerId.equals(followingId)) {
            throw new RuntimeException("Cannot follow yourself");
        }

        // Check if already following
        if (followerRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new RuntimeException("Already following this user");
        }

        // Check if blocked
        if (blockRepository.existsBlockBetweenUsers(followerId, followingId)) {
            throw new RuntimeException("Cannot follow this user");
        }

        Follower follow = Follower.builder()
                .followerId(followerId)
                .followingId(followingId)
                .build();
        followerRepository.save(follow);

        // Update stats
        UserStats followerStats = getUserStats(followerId);
        followerStats.incrementFollowingCount();
        userStatsRepository.save(followerStats);

        UserStats followingStats = getUserStats(followingId);
        followingStats.incrementFollowersCount();
        userStatsRepository.save(followingStats);
    }

    @Transactional
    public void unfollowUser(String followerId, String followingId) {
        if (!followerRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new RuntimeException("Not following this user");
        }

        followerRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);

        // Update stats
        UserStats followerStats = getUserStats(followerId);
        followerStats.decrementFollowingCount();
        userStatsRepository.save(followerStats);

        UserStats followingStats = getUserStats(followingId);
        followingStats.decrementFollowersCount();
        userStatsRepository.save(followingStats);
    }

    public boolean isFollowing(String followerId, String followingId) {
        return followerRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public Page<Follower> getFollowing(String userId, Pageable pageable) {
        return followerRepository.findByFollowerId(userId, pageable);
    }

    public Page<Follower> getFollowers(String userId, Pageable pageable) {
        return followerRepository.findByFollowingId(userId, pageable);
    }

    public List<String> getFollowingIds(String userId) {
        return followerRepository.findFollowingIdsByFollowerId(userId);
    }

    // ==================== Block System ====================

    @Transactional
    public void blockUser(String blockerId, String blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new RuntimeException("Cannot block yourself");
        }

        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new RuntimeException("Already blocked this user");
        }

        // Remove follow relationship if exists
        if (followerRepository.existsByFollowerIdAndFollowingId(blockerId, blockedId)) {
            unfollowUser(blockerId, blockedId);
        }
        if (followerRepository.existsByFollowerIdAndFollowingId(blockedId, blockerId)) {
            unfollowUser(blockedId, blockerId);
        }

        Block block = Block.builder()
                .blockerId(blockerId)
                .blockedId(blockedId)
                .build();
        blockRepository.save(block);
    }

    @Transactional
    public void unblockUser(String blockerId, String blockedId) {
        if (!blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new RuntimeException("Not blocking this user");
        }

        blockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    public boolean isBlocking(String blockerId, String blockedId) {
        return blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    public boolean isBlocked(String userId, String otherUserId) {
        return blockRepository.existsBlockBetweenUsers(userId, otherUserId);
    }

    public List<String> getBlockedIds(String userId) {
        return blockRepository.findBlockedIdsByBlockerId(userId);
    }

    // ==================== Search ====================

    public Page<UserProfile> searchUsers(String search, Pageable pageable) {
        return userProfileRepository.searchActiveUsers(search, pageable);
    }
}
