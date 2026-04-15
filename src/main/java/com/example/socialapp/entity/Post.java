package com.example.socialapp.entity;

import com.example.socialapp.enums.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for Post.
 * Represents a user-generated post that can be published, flagged, or removed.
 */
@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Many-to-One relationship with User.
     * A post belongs to exactly one user (the author).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    /**
     * Publish the post.
     * Changes post status to PUBLISHED.
     */
    public void publish() {
        this.status = PostStatus.PUBLISHED;
    }

    /**
     * Mark the post as flagged.
     * Changes post status to FLAGGED for moderator review.
     */
    public void markFlagged() {
        this.status = PostStatus.FLAGGED;
    }

    /**
     * Remove the post from the platform.
     * Changes post status to REMOVED.
     */
    public void remove() {
        this.status = PostStatus.REMOVED;
    }

    /**
     * Check if the post is published.
     *
     * @return true if post status is PUBLISHED
     */
    public boolean isPublished() {
        return status == PostStatus.PUBLISHED;
    }

    /**
     * Check if the post is flagged.
     *
     * @return true if post status is FLAGGED
     */
    public boolean isFlagged() {
        return status == PostStatus.FLAGGED;
    }

    /**
     * Check if the post is removed.
     *
     * @return true if post status is REMOVED
     */
    public boolean isRemoved() {
        return status == PostStatus.REMOVED;
    }

    /**
     * Get post summary (first 100 characters of content).
     *
     * @return post summary
     */
    public String getSummary() {
        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
}
