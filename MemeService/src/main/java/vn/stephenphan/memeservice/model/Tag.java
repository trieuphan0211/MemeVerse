package vn.stephenphan.memeservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "name", unique = true, nullable = false, length = 50)
    private String name;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Helper method to increment usage count
    public void incrementUsageCount() {
        this.usageCount++;
    }

    public void decrementUsageCount() {
        if (this.usageCount > 0) this.usageCount--;
    }
}
