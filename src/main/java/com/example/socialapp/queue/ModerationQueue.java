package com.example.socialapp.queue;

import com.example.socialapp.entity.Post;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.util.UUID;

/**
 * Moderation Queue Component.
 * In-memory queue for posts awaiting moderation review.
 * Uses thread-safe ConcurrentLinkedQueue for concurrent access.
 */
@Slf4j
@Component
public class ModerationQueue {
    
    private final Queue<Post> queue = new ConcurrentLinkedQueue<>();

    /**
     * Add a post to the moderation queue.
     *
     * @param post the post to add to the queue
     */
    public void addPost(Post post) {
        if (post == null) {
            log.warn("Attempted to add null post to moderation queue");
            return;
        }
        queue.offer(post);
        log.info("Post added to moderation queue. Queue size: {}", queue.size());
    }

    /**
     * Get the next post from the moderation queue without removing it.
     *
     * @return the next post in queue, or null if queue is empty
     */
    public Post peekNextPost() {
        Post post = queue.peek();
        if (post != null) {
            log.debug("Peeked post from moderation queue. Queue size: {}", queue.size());
        }
        return post;
    }

    /**
     * Get and remove the next post from the moderation queue.
     *
     * @return the next post in queue, or null if queue is empty
     */
    public Post getNextPost() {
        Post post = queue.poll();
        if (post != null) {
            log.info("Retrieved post from moderation queue. Remaining queue size: {}", queue.size());
        } else {
            log.debug("Moderation queue is empty");
        }
        return post;
    }

    /**
     * Remove a specific post from the moderation queue.
     *
     * @param post the post to remove
     * @return true if the post was removed, false otherwise
     */
    public boolean removePost(Post post) {
        boolean removed = queue.remove(post);
        if (removed) {
            log.info("Post removed from moderation queue. Queue size: {}", queue.size());
        } else {
            log.warn("Post not found in moderation queue");
        }
        return removed;
    }

    /**
     * Remove a post from the moderation queue by its ID.
     * This method avoids lazy initialization exceptions by matching IDs
     * instead of using equals() which triggers entity relationship loading.
     *
     * @param postId the ID of the post to remove
     * @return true if the post was removed, false otherwise
     */
    public boolean removePostById(UUID postId) {
        boolean removed = queue.removeIf(post -> post.getId().equals(postId));
        if (removed) {
            log.info("Post removed from moderation queue by ID: {}. Queue size: {}", postId, queue.size());
        } else {
            log.warn("Post not found in moderation queue. ID: {}", postId);
        }
        return removed;
    }

    /**
     * Get the current size of the moderation queue.
     *
     * @return the number of posts in the queue
     */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * Check if the moderation queue is empty.
     *
     * @return true if queue is empty, false otherwise
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Clear all posts from the moderation queue.
     */
    public void clear() {
        queue.clear();
        log.info("Moderation queue cleared");
    }
}
