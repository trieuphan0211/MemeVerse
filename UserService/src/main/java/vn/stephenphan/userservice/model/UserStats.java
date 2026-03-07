package vn.stephenphan.userservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStats {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "total_memes")
    @Builder.Default
    private Integer totalMemes = 0;

    @Column(name = "total_likes_received")
    @Builder.Default
    private Integer totalLikesReceived = 0;

    @Column(name = "total_comments")
    @Builder.Default
    private Integer totalComments = 0;

    @Column(name = "game_score")
    @Builder.Default
    private Integer gameScore = 0;

    @Column(name = "followers_count")
    @Builder.Default
    private Integer followersCount = 0;

    @Column(name = "following_count")
    @Builder.Default
    private Integer followingCount = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods to increment counters
    public void incrementTotalMemes() {
        this.totalMemes++;
    }

    public void decrementTotalMemes() {
        if (this.totalMemes > 0) this.totalMemes--;
    }

    public void incrementTotalLikesReceived() {
        this.totalLikesReceived++;
    }

    public void decrementTotalLikesReceived() {
        if (this.totalLikesReceived > 0) this.totalLikesReceived--;
    }

    public void incrementTotalComments() {
        this.totalComments++;
    }

    public void decrementTotalComments() {
        if (this.totalComments > 0) this.totalComments--;
    }

    public void incrementFollowersCount() {
        this.followersCount++;
    }

    public void decrementFollowersCount() {
        if (this.followersCount > 0) this.followersCount--;
    }

    public void incrementFollowingCount() {
        this.followingCount++;
    }

    public void decrementFollowingCount() {
        if (this.followingCount > 0) this.followingCount--;
    }

    public void addGameScore(int points) {
        this.gameScore += points;
    }
}
