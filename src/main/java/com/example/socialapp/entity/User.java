package com.example.socialapp.entity;

import com.example.socialapp.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * JPA Entity for User.
 * Represents a user in the system with authentication, authorization, and social features.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "username"),
    @UniqueConstraint(columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column
    private Integer age;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Long followersCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long followingCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 255)
    private String lastLogin;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    /**
     * Self-referencing many-to-many relationship for followers.
     * This user follows other users.
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "user_followers",
        joinColumns = @JoinColumn(name = "follower_id"),
        inverseJoinColumns = @JoinColumn(name = "following_id")
    )
    @Builder.Default
    private Set<User> following = new HashSet<>();

    /**
     * Self-referencing many-to-many relationship for followers.
     * Users that follow this user.
     */
    @ManyToMany(mappedBy = "following", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> followers = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * One-to-Many relationship with Post.
     * A user can create multiple posts.
     */
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Post> posts = new java.util.ArrayList<>();

    /**
     * Determines if the user can post content.
     * Users cannot post if they are banned (temporarily or permanently).
     *
     * @return true if user is ACTIVE and can post, false otherwise
     */
    public boolean canPost() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * Check if user is banned (temporarily or permanently).
     *
     * @return true if user status is TEMP_BANNED or PERM_BANNED
     */
    public boolean isBanned() {
        return status == UserStatus.TEMP_BANNED || status == UserStatus.PERM_BANNED;
    }

    /**
     * Get the full name of the user.
     *
     * @return concatenated first and last name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Add a role to the user.
     *
     * @param role the role to add
     */
    public void addRole(Role role) {
        this.roles.add(role);
    }

    /**
     * Remove a role from the user.
     *
     * @param role the role to remove
     */
    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    /**
     * Add a user to the following list.
     *
     * @param user the user to follow
     */
    public void follow(User user) {
        this.following.add(user);
        user.followers.add(this);
        incrementFollowingCount();
        user.incrementFollowersCount();
    }

    /**
     * Remove a user from the following list.
     *
     * @param user the user to unfollow
     */
    public void unfollow(User user) {
        this.following.remove(user);
        user.followers.remove(this);
        decrementFollowingCount();
        user.decrementFollowersCount();
    }

    /**
     * Check if this user follows another user.
     *
     * @param user the user to check
     * @return true if this user follows the given user
     */
    public boolean isFollowing(User user) {
        return following.contains(user);
    }

    /**
     * Increment the followers count.
     */
    private void incrementFollowersCount() {
        this.followersCount = (this.followersCount != null ? this.followersCount : 0L) + 1;
    }

    /**
     * Decrement the followers count.
     */
    private void decrementFollowersCount() {
        this.followersCount = Math.max(0L, (this.followersCount != null ? this.followersCount : 0L) - 1);
    }

    /**
     * Increment the following count.
     */
    private void incrementFollowingCount() {
        this.followingCount = (this.followingCount != null ? this.followingCount : 0L) + 1;
    }

    /**
     * Decrement the following count.
     */
    private void decrementFollowingCount() {
        this.followingCount = Math.max(0L, (this.followingCount != null ? this.followingCount : 0L) - 1);
    }
}
