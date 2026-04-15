package com.example.socialapp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for Like.
 * Represents a like interaction between a user and a post.
 */
@Entity
@Table(name = "likes", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_post_id", columnList = "post_id"),
    @Index(name = "idx_user_post", columnList = "user_id,post_id")
},
uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Like {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    /**
     * Many-to-One relationship with User.
     * A like belongs to exactly one user (who clicked like).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Many-to-One relationship with Post.
     * A like belongs to exactly one post (that was liked).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /**
     * Flag to indicate if this is a like (true) or dislike (false).
     */
    @Column(nullable = false)
    private Boolean isLike;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
