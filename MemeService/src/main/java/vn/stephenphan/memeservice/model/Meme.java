package vn.stephenphan.memeservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "memes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meme {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "username")
    private String username;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "caption", length = 500)
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private MemeStatus status = MemeStatus.PENDING;

    @Column(name = "vote_score")
    @Builder.Default
    private Integer voteScore = 0;

    @Column(name = "up_votes")
    @Builder.Default
    private Integer upVotes = 0;

    @Column(name = "down_votes")
    @Builder.Default
    private Integer downVotes = 0;

    @Column(name = "comment_count")
    @Builder.Default
    private Integer commentCount = 0;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "meme_tags",
            joinColumns = @JoinColumn(name = "meme_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods for vote management
    public void upvote() {
        this.upVotes++;
        this.voteScore = this.upVotes - this.downVotes;
    }

    public void downvote() {
        this.downVotes++;
        this.voteScore = this.upVotes - this.downVotes;
    }

    public void removeUpvote() {
        if (this.upVotes > 0) {
            this.upVotes--;
            this.voteScore = this.upVotes - this.downVotes;
        }
    }

    public void removeDownvote() {
        if (this.downVotes > 0) {
            this.downVotes--;
            this.voteScore = this.upVotes - this.downVotes;
        }
    }

    public void changeUpvoteToDownvote() {
        removeUpvote();
        downvote();
    }

    public void changeDownvoteToUpvote() {
        removeDownvote();
        upvote();
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) this.commentCount--;
    }
}
