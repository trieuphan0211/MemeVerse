package vn.stephenphan.memeservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "meme_votes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"meme_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemeVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "meme_id", nullable = false)
    private String memeId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false)
    private VoteType voteType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
