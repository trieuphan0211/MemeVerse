package vn.stephenphan.memeservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meme_comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemeComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "meme_id", nullable = false)
    private String memeId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "username")
    private String username;

    @Column(name = "parent_id")
    private String parentId; // For reply comments

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "likes_count")
    @Builder.Default
    private Integer likesCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public void incrementLikesCount() {
        this.likesCount++;
    }

    public void decrementLikesCount() {
        if (this.likesCount > 0) this.likesCount--;
    }

    public boolean isReply() {
        return parentId != null;
    }
}
