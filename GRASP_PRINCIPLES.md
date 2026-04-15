# GRASP Principles in Social App Backend

## Overview

GRASP (General Responsibility Assignment Software Patterns) provides 9 patterns for assigning responsibilities to classes and creating better object-oriented designs.

---

## 1. Information Expert

**Definition:** Assign a responsibility to the class that has the information necessary to fulfill it.

### Why It Matters
- ✅ Classes encapsulate their own knowledge
- ✅ Related data and logic stay together
- ✅ Single place to update business rules

### Implementation in Social App

#### Example 1: PostService (Content Moderation Expert)
```java
public class PostServiceImpl implements PostService {
    
    private final PostRepository postRepository;
    private final ModerationService moderationService;
    private final UserRepository userRepository;
    
    // PostService is the expert on post creation rules
    @Override
    public PostDTO createPost(UUID authorId, String content) {
        // PostService knows:
        // - How to validate content
        // - What moderation rules apply
        // - When to flag vs publish
        
        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", authorId.toString()));
        
        // Business rule: Can user post?
        if (!canPost(author)) {
            throw new IllegalArgumentException("User cannot post");
        }
        
        // Create post with appropriate status
        Post post = Post.builder()
            .content(content)
            .author(author)
            .build();
        
        // Business rule: Does content need review?
        if (moderationService.reviewContent(content)) {
            post.setStatus(PostStatus.PUBLISHED);  // Safe content
        } else {
            post.setStatus(PostStatus.FLAGGED);    // Needs review
            moderationService.sendToQueue(post);
        }
        
        return mapToDTO(postRepository.save(post));
    }
    
    private boolean canPost(User user) {
        // PostService is the expert on post restrictions
        return !user.getStatus().equals("BANNED");
    }
}

// NOT in other classes:
// ❌ Controller.createPost() { ...moderation logic... }
// ❌ Post.isAllowedToPost() { ...user logic... }
// ✅ PostService.createPost() { ...all post logic... }
```

#### Example 2: UserService (User Status Expert)
```java
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final BanRepository banRepository;
    
    // UserService is the expert on user status management
    public void updateStatus(UUID userId, UserStatus status) {
        // UserService knows:
        // - Valid status transitions
        // - What happens when status changes
        // - Ban-related logic
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(...));
        
        // Business rule: Can transition to this status?
        if (!isValidTransition(user.getStatus(), status)) {
            throw new IllegalStateException("Invalid status transition");
        }
        
        // Update status
        user.setStatus(status);
        userRepository.save(user);
        
        // Side effects based on status change
        if (status == UserStatus.ACTIVE) {
            // Clear bans when user becomes active
            banRepository.deleteByUserId(userId);
        }
    }
    
    private boolean isValidTransition(UserStatus from, UserStatus to) {
        // UserService is the expert on valid transitions
        switch(from) {
            case ACTIVE -> // Can go to TEMP_BANNED or PERM_BANNED
                return to == UserStatus.TEMP_BANNED || to == UserStatus.PERM_BANNED;
            case TEMP_BANNED -> // Can go back to ACTIVE only
                return to == UserStatus.ACTIVE;
            case PERM_BANNED -> // No transitions from PERM_BANNED
                return false;
        }
        return false;
    }
}
```

#### Example 3: ModerationService (Content Review Expert)
```java
public class ModerationServiceImpl implements ModerationService {
    
    private final PostRepository postRepository;
    private final ModerationQueueRepository queueRepository;
    private final ModerationDecisionRepository decisionRepository;
    
    // ModerationService is the expert on content review
    @Override
    public boolean reviewContent(String content) {
        // ModerationService knows:
        // - What makes content inappropriate
        // - Which keywords to check
        // - ML/NLP rules
        
        if (content == null || content.isEmpty()) {
            return true;  // Valid (empty is OK)
        }
        
        // Check against banned keywords/patterns
        List<String> bannedKeywords = Arrays.asList(
            "spam", "abuse", "hate", "violence"
        );
        
        String contentLower = content.toLowerCase();
        for (String keyword : bannedKeywords) {
            if (contentLower.contains(keyword)) {
                return false;  // Inappropriate content
            }
        }
        
        return true;  // Content is safe
    }
    
    // ModerationService is expert on what happens after review
    @Override
    public ModerationDecision makeDecision(
        DecisionType decisionType, 
        UUID postId, 
        String reason
    ) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException(...));
        
        // ModerationService knows the consequences of each decision
        switch(decisionType) {
            case APPROVED:
                post.setStatus(PostStatus.PUBLISHED);
                queueRepository.deleteByPostId(postId);
                break;
            case REMOVED:
                post.setStatus(PostStatus.REMOVED);
                applyPenaltyToAuthor(post.getAuthor());
                queueRepository.deleteByPostId(postId);
                break;
            case ESCALATED:
                // Keep in queue for senior review
                break;
        }
        
        postRepository.save(post);
        return decisionRepository.save(new ModerationDecision(postId, decisionType, reason));
    }
    
    private void applyPenaltyToAuthor(User author) {
        // Penalty logic is known by ModerationService
        int currentPenalties = author.getPenalties();
        if (currentPenalties >= 3) {
            author.setStatus(UserStatus.TEMP_BANNED);
        }
        author.setPenalties(currentPenalties + 1);
    }
}
```

### Information Expert Checklist

- ✅ PostService knows post creation rules
- ✅ UserService knows user status transitions
- ✅ ModerationService knows content review rules
- ✅ AppealService knows appeal workflows
- ✅ Each service encapsulates its domain knowledge

---

## 2. Creator

**Definition:** Assign the responsibility for creating objects to a class that knows how to create them.

### Why It Matters
- ✅ Clear responsibility for object creation
- ✅ Centralized creation logic
- ✅ Easy to add creation rules

### Implementation in Social App

#### Example 1: PostService Creates PostDTO
```java
public class PostServiceImpl implements PostService {
    
    // PostService is the creator of PostDTOs
    private PostDTO mapToDTO(Post post) {
        return PostDTO.builder()
            .id(post.getId())
            .content(post.getContent())
            .status(post.getStatus())
            .authorId(post.getAuthor().getId())
            .authorUsername(post.getAuthor().getUsername())
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
    }
    
    @Override
    public PostDTO getPostById(UUID postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId.toString()));
        
        // PostService creates the DTO
        return mapToDTO(post);
    }
    
    @Override
    public PostDTO createPost(UUID authorId, String content) {
        // ... create post logic ...
        Post post = postRepository.save(newPost);
        
        // PostService creates the DTO from the entity
        return mapToDTO(post);
    }
}

// Controller doesn't create DTOs
@RestController
public class PostController {
    private final PostService postService;
    
    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPost(@PathVariable UUID id) {
        // Controller asks PostService for DTO
        // PostService is responsible for creation
        PostDTO post = postService.getPostById(id);
        return ResponseEntity.ok(post);
    }
}
```

#### Example 2: DataInitializationConfig Creates Test Data
```java
// DataInitializationConfig is the creator of test objects
@Configuration
public class DataInitializationConfig {
    
    @PostConstruct
    public void initializeData() {
        // Create admin user
        User admin = User.builder()
            .email("admin@test.com")
            .username("admin")
            .password(passwordEncoder.encode("admin123"))
            .status(UserStatus.ACTIVE)
            .build();
        
        // Create moderator user
        User moderator = User.builder()
            .email("moderator@test.com")
            .username("moderator")
            .password(passwordEncoder.encode("moderator123"))
            .status(UserStatus.ACTIVE)
            .build();
        
        // Create roles
        Role userRole = new Role(UUID.randomUUID(), "ROLE_USER");
        Role moderatorRole = new Role(UUID.randomUUID(), "ROLE_MODERATOR");
        Role adminRole = new Role(UUID.randomUUID(), "ROLE_ADMIN");
        
        // Save to database
        admin.setRoles(Arrays.asList(userRole, moderatorRole, adminRole));
        moderator.setRoles(Arrays.asList(userRole, moderatorRole));
        
        userRepository.save(admin);
        userRepository.save(moderator);
        roleRepository.saveAll(Arrays.asList(userRole, moderatorRole, adminRole));
    }
}

// DataInitializationConfig is the creator of test data
// Other classes don't create test fixtures
```

#### Example 3: AppealService Creates Appeal Objects
```java
public class AppealServiceImpl implements AppealService {
    
    private final AppealRepository appealRepository;
    private final BanRepository banRepository;
    
    // AppealService is the creator of Appeal objects
    @Override
    public AppealDTO submitAppeal(UUID banId, String reason) {
        Ban ban = banRepository.findById(banId)
            .orElseThrow(() -> new ResourceNotFoundException("Ban", "id", banId.toString()));
        
        // AppealService creates the Appeal
        Appeal appeal = Appeal.builder()
            .ban(ban)
            .reason(reason)
            .status(AppealStatus.PENDING)
            .submittedAt(LocalDateTime.now())
            .build();
        
        // Save and return DTO
        Appeal saved = appealRepository.save(appeal);
        return mapToDTO(saved);
    }
    
    private AppealDTO mapToDTO(Appeal appeal) {
        // AppealService creates AppealDTOs
        return AppealDTO.builder()
            .id(appeal.getId())
            .banId(appeal.getBan().getId())
            .userId(appeal.getBan().getUser().getId())
            .reason(appeal.getReason())
            .status(appeal.getStatus().toString())
            .submittedAt(appeal.getSubmittedAt())
            .build();
    }
}
```

### Creator Checklist

- ✅ PostService creates PostDTOs
- ✅ UserService creates UserDTOs
- ✅ AppealService creates AppealDTOs
- ✅ DataInitializationConfig creates test data
- ✅ Services create DTOs from entities

---

## 3. Controller (Mediator/Handler)

**Definition:** Assign the responsibility for handling requests and system events to a dedicated "Controller" object.

### Why It Matters
- ✅ Clear request handling responsibility
- ✅ Orchestration of business logic
- ✅ Clean separation of HTTP from business logic

### Implementation in Social App

#### Example 1: PostController Mediates
```java
// PostController is the mediator between HTTP and business logic
@RestController
@RequestMapping("/v1/posts")
public class PostController {
    
    private final PostService postService;
    private final UserRepository userRepository;
    private final LikeService likeService;
    
    // PostController mediates post creation request
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<PostDTO> createPost(@Valid @RequestBody PostRequest postRequest) {
        // 1. Extract HTTP request information
        String userEmail = SecurityUtil.getCurrentUserEmail();
        
        // 2. Delegate to business logic
        User currentUser = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        PostDTO createdPost = postService.createPost(currentUser.getId(), postRequest.getContent());
        
        // 3. Format HTTP response
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }
    
    // PostController mediates post retrieval request
    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable UUID id) {
        // 1. Get request parameter
        // 2. Delegate to service
        PostDTO post = postService.getPostById(id);
        // 3. Format response
        return ResponseEntity.ok(post);
    }
    
    // PostController mediates like action
    @PostMapping("/{postId}/like")
    public ResponseEntity<LikeDTO> likePost(@PathVariable UUID postId) {
        String userEmail = SecurityUtil.getCurrentUserEmail();
        User currentUser = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        LikeDTO like = likeService.likePost(postId, currentUser.getId());
        return ResponseEntity.ok(like);
    }
}

// Controller responsibilities:
// ✅ Extract HTTP request data
// ✅ Call appropriate service
// ✅ Format HTTP response
// ❌ NOT business logic (delegated to service)
// ❌ NOT database operations (delegated to repository)
```

#### Example 2: AppealController Mediates
```java
// AppealController is the mediator for appeal requests
@RestController
@RequestMapping("/v1/appeals")
public class AppealController {
    
    private final AppealService appealService;
    private final BanRepository banRepository;
    
    // AppealController mediates appeal submission
    @PostMapping("/submit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AppealDTO> submitAppeal(
        @Valid @RequestBody AppealRequest request) {
        
        // 1. Get authenticated user info
        String userEmail = SecurityUtil.getCurrentUserEmail();
        
        // 2. Delegate to service
        AppealDTO appeal = appealService.submitAppeal(
            request.getBanId(), 
            request.getReason()
        );
        
        // 3. Format response
        return ResponseEntity.status(HttpStatus.CREATED).body(appeal);
    }
    
    // AppealController mediates moderator review
    @PostMapping("/{appealId}/review")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<AppealDTO> moderatorReview(
        @PathVariable UUID appealId,
        @Valid @RequestBody ReviewRequest request) {
        
        // Delegate to service
        AppealDTO updated = appealService.moderatorReview(
            appealId, 
            request.getReview()
        );
        
        return ResponseEntity.ok(updated);
    }
    
    // AppealController mediates admin decision
    @PostMapping("/{appealId}/decide")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppealDTO> adminDecision(
        @PathVariable UUID appealId,
        @Valid @RequestBody DecisionRequest request) {
        
        AppealDTO decision = appealService.adminDecision(
            appealId,
            request.isApproved(),
            request.getDecision()
        );
        
        return ResponseEntity.ok(decision);
    }
}
```

#### Example 3: JwtAuthenticationFilter Mediates
```java
// JwtAuthenticationFilter is a controller/mediator for authentication
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    
    // Mediates authentication for each HTTP request
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 1. Extract JWT token from request header
            String jwt = getJwtFromRequest(request);
            
            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                // 2. Get user details from token
                String email = jwtTokenProvider.getEmailFromJWT(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                
                // 3. Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                // 4. Set in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication", ex);
        }
        
        // 5. Continue filter chain
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### Controller Checklist

- ✅ Extract HTTP request data
- ✅ Delegate to service/business logic
- ✅ Format HTTP response
- ✅ No business logic in controller
- ✅ No database queries in controller

---

## 4. Low Coupling

**Definition:** Classes should have minimal dependencies on each other.

### Why It Matters
- ✅ Classes are independent
- ✅ Easy to test (can mock dependencies)
- ✅ Easy to change (changes don't cascade)
- ✅ Better code reuse

### Implementation in Social App

#### Example 1: Using DTOs (Low Coupling)
```java
// ❌ High Coupling: Expose entity directly
@RestController
public class PostController {
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPost(@PathVariable UUID id) {
        Post post = postService.getPost(id);  // Returns entity
        return ResponseEntity.ok(post);
        // Client knows Post entity structure
        // Any entity change breaks client
    }
}

// ✅ Low Coupling: Use DTOs
@RestController
public class PostController {
    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPost(@PathVariable UUID id) {
        PostDTO post = postService.getPostById(id);  // Returns DTO
        return ResponseEntity.ok(post);
        // Client only knows PostDTO structure
        // Can change Post entity without breaking client
    }
}

// DTO decouples internal structure from external API
@Data
@Builder
public class PostDTO {
    private UUID id;
    private String content;
    private PostStatus status;
    private UUID authorId;
    private LocalDateTime createdAt;
    // Only necessary fields exposed
}
```

#### Example 2: Dependency Injection (Low Coupling)
```java
// ❌ High Coupling: Create dependencies internally
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository = new JpaPostRepository();  // Tightly coupled!
    
    // Problems:
    // - Can't test with mock repository
    // - Can't swap implementations
    // - Hard to change
}

// ✅ Low Coupling: Inject dependencies
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;  // Interface type (abstraction)
    
    // Constructor injection
    public PostServiceImpl(PostRepository postRepository) {
        this.postRepository = postRepository;  // Depends on INTERFACE, not implementation
    }
    
    // Benefits:
    // - Can test with mock repository
    // - Can swap implementations (PostgreSQL, MongoDB, Redis, etc.)
    // - Easy to change
}

// Usage
@Configuration
public class BeanConfig {
    @Bean
    public PostService postService(PostRepository repository) {
        return new PostServiceImpl(repository);  // Inject abstraction
    }
}
```

#### Example 3: Interface-Based (Low Coupling)
```java
// ❌ High Coupling: Depends on concrete class
public class PostController {
    private PostServiceImpl postService;  // Depends on IMPLEMENTATION
    
    // Can only use PostServiceImpl
    // Changes in PostServiceImpl affect controller
}

// ✅ Low Coupling: Depends on interface
public class PostController {
    private final PostService postService;  // Depends on INTERFACE
    
    public PostController(PostService postService) {
        this.postService = postService;  // Can be ANY implementation
    }
}

// Can use different implementations without changing controller
public interface PostService {
    PostDTO createPost(UUID authorId, String content);
}

public class PostServiceImpl implements PostService { }
public class CachedPostServiceImpl implements PostService { }
public class AuditedPostServiceImpl implements PostService { }
```

### Low Coupling Checklist

- ✅ Use DTOs instead of entities in APIs
- ✅ Depend on interfaces, not implementations
- ✅ Inject dependencies (don't create internally)
- ✅ Use abstractions (@Service, @Repository)
- ✅ Minimal calls between unrelated classes

---

## 5. High Cohesion

**Definition:** Related responsibilities should be part of the same class/module.

### Why It Matters
- ✅ Related logic is together
- ✅ Easy to understand and maintain
- ✅ Clear responsibility boundaries
- ✅ Better encapsulation

### Implementation in Social App

#### Example 1: Cohesive AppealService
```java
// ✅ High Cohesion: All appeal-related logic in AppealService
public interface AppealService {
    // All methods are related to appeals
    AppealDTO submitAppeal(UUID banId, String reason);
    AppealDTO submitPostAppeal(UUID postId, String reason);
    AppealDTO getAppealById(UUID appealId);
    AppealDTO moderatorReview(UUID appealId, String review);
    AppealDTO adminDecision(UUID appealId, boolean approved, String decision);
    List<AppealDTO> getPendingAppeals();
    long getPendingAppealCount();
}

// Implementation keeps related logic together
public class AppealServiceImpl implements AppealService {
    private final AppealRepository appealRepository;
    private final BanRepository banRepository;
    private final UserRepository userRepository;
    
    // All appeal operations are cohesive
    @Override
    public AppealDTO submitAppeal(UUID banId, String reason) {
        // Appeal creation logic
    }
    
    @Override
    public AppealDTO moderatorReview(UUID appealId, String review) {
        // Appeal review logic
    }
    
    @Override
    public AppealDTO adminDecision(UUID appealId, boolean approved, String decision) {
        // Appeal decision logic
    }
}

// ❌ Bad: Split appeal logic across services
// class UserService { submitAppeal() }
// class BanService { moderatorReview() }
// class AdminService { adminDecision() }
// This violates cohesion - related logic is scattered
```

#### Example 2: Cohesive PostService
```java
// ✅ High Cohesion: All post operations together
public interface PostService {
    // All related to posts
    PostDTO createPost(UUID authorId, String content);
    PostDTO getPostById(UUID postId);
    Page<PostDTO> getPostsByAuthor(UUID authorId, Pageable pageable);
    Page<PostDTO> getAllPublishedPosts(Pageable pageable);
    void deletePost(UUID postId);
}

// Keep post retrieval and creation in same service
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final ModerationService moderationService;
    
    @Override
    public PostDTO createPost(UUID authorId, String content) {
        // Create logic
    }
    
    @Override
    public PostDTO getPostById(UUID postId) {
        // Retrieval logic
    }
    
    @Override
    public Page<PostDTO> getPostsByAuthor(UUID authorId, Pageable pageable) {
        // Author posts logic
    }
}

// ❌ Bad: Split post operations across services
// class CreationService { createPost() }
// class RetrievalService { getPost() }
// class DeletionService { deletePost() }
// Violates cohesion - related operations split up
```

#### Example 3: Cohesive ModerationService
```java
// ✅ High Cohesion: All moderation operations together
public interface ModerationService {
    // All related to content moderation
    boolean reviewContent(String content);
    void flagPost(Post post);
    void sendToQueue(Post post);
    ModerationDecision makeDecision(DecisionType type, UUID postId, String reason);
    void approvePost(Post post);
    void rejectPost(Post post);
}

// Implementation keeps moderation logic cohesive
public class ModerationServiceImpl implements ModerationService {
    private final PostRepository postRepository;
    private final ModerationQueueRepository queueRepository;
    
    @Override
    public boolean reviewContent(String content) {
        // Content review logic
    }
    
    @Override
    public void flagPost(Post post) {
        // Flagging logic
    }
    
    @Override
    public ModerationDecision makeDecision(DecisionType type, UUID postId, String reason) {
        // Decision logic
    }
}

// ❌ Bad: Split moderation across services
// class ContentService { reviewContent() }
// class QueueService { flagPost(), sendToQueue() }
// class DecisionService { makeDecision() }
// Related logic scattered across services
```

### High Cohesion Checklist

- ✅ Related operations in same service
- ✅ Common dependencies shared
- ✅ Logical grouping by domain
- ✅ Methods in a class work together
- ✅ Single reason to change

---

## 6. Polymorphism

**Definition:** Use polymorphism instead of type checks (if-else or switch statements).

### Why It Matters
- ✅ No type checking code
- ✅ Easy to add new types
- ✅ Behavior determined by object type
- ✅ Extensible design

### Implementation in Social App

#### Example 1: Exception Handling (Polymorphism)
```java
// ✅ Polymorphism: Each exception has its own handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("Resource not found", ex.getMessage()));
    }
    
    @ExceptionHandler(BannedUserException.class)
    public ResponseEntity<BannedUserErrorResponse> handleBannedUserException(
            BannedUserException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new BannedUserErrorResponse("User banned", ex.getBanMessage()));
    }
    
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(
            UnauthorizedAccessException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("Access denied", ex.getMessage()));
    }
    
    // No if-else type checking
    // Each exception type has its handler
    // Easy to add new exception types
}

// ❌ Bad: Type checking
// @ExceptionHandler(Exception.class)
// public ResponseEntity<?> handleException(Exception ex) {
//     if (ex instanceof ResourceNotFoundException) { }
//     else if (ex instanceof BannedUserException) { }
//     else if (ex instanceof UnauthorizedAccessException) { }
// }
```

#### Example 2: Decision Making (Polymorphism via Strategy)
```java
// ✅ Polymorphism: Use enum strategy instead of if-else
public class ModerationServiceImpl implements ModerationService {
    
    @Override
    public ModerationDecision makeDecision(
        DecisionType decisionType,  // Enum strategy
        UUID postId, 
        String reason) {
        
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException(...));
        
        // Strategy pattern: each decision type knows what to do
        switch(decisionType) {
            case APPROVED -> postApproved(post);      // Polymorphic behavior
            case REMOVED -> postRemoved(post);        // Polymorphic behavior
            case ESCALATED -> postEscalated(post);    // Polymorphic behavior
        }
        
        // Easy to add new decision type: add enum value + case statement
    }
    
    private void postApproved(Post post) {
        // APPROVED behavior
        post.setStatus(PostStatus.PUBLISHED);
        queueRepository.deleteByPostId(post.getId());
    }
    
    private void postRemoved(Post post) {
        // REMOVED behavior
        post.setStatus(PostStatus.REMOVED);
        applyPenaltyToAuthor(post.getAuthor());
        queueRepository.deleteByPostId(post.getId());
    }
    
    private void postEscalated(Post post) {
        // ESCALATED behavior
        // Keep in queue for senior review
    }
}

// ❌ Bad: Type checking code
// if (decisionType == DecisionType.APPROVED) { }
// else if (decisionType == DecisionType.REMOVED) { }
// Hard to extend, easy to miss cases
```

#### Example 3: Service Implementations (Polymorphism)
```java
// ✅ Polymorphism: Different service implementations for same interface
public interface PostService {
    PostDTO createPost(UUID authorId, String content);
}

// Implementation 1: Standard service
public class PostServiceImpl implements PostService {
    @Override
    public PostDTO createPost(UUID authorId, String content) {
        // Standard logic
    }
}

// Implementation 2: With caching (decorator pattern)
public class CachedPostServiceImpl implements PostService {
    private final PostService delegate;
    private final Cache cache;
    
    @Override
    public PostDTO createPost(UUID authorId, String content) {
        // Add caching logic, then delegate
        return delegate.createPost(authorId, content);
    }
}

// Implementation 3: With auditing
public class AuditedPostServiceImpl implements PostService {
    private final PostService delegate;
    private final AuditLog auditLog;
    
    @Override
    public PostDTO createPost(UUID authorId, String content) {
        auditLog.log("Creating post");
        PostDTO result = delegate.createPost(authorId, content);
        auditLog.log("Post created: " + result.getId());
        return result;
    }
}

// Usage - polymorphic behavior
@Configuration
public class BeanConfig {
    @Bean
    public PostService postService(PostRepository repo, Cache cache) {
        // Can easily switch implementations
        PostService service = new PostServiceImpl(repo);
        service = new CachedPostServiceImpl(service, cache);
        service = new AuditedPostServiceImpl(service, auditLog);
        return service;
    }
}
```

### Polymorphism Checklist

- ✅ No if-else type checks
- ✅ Each type has its implementation
- ✅ Behavior determined by object type
- ✅ Easy to add new types (enums, implementations)
- ✅ Cleaner code without type casting

---

## 7. Pure Fabrication

**Definition:** Create classes that don't represent domain concepts but improve overall design.

### Why It Matters
- ✅ Better separation of concerns
- ✅ Improved cohesion
- ✅ Cleaner domain model
- ✅ Reusable utilities

### Implementation in Social App

#### Example 1: SecurityUtil (Pure Fabrication)
```java
// SecurityUtil doesn't represent a business concept
// But it improves design by encapsulating security details

public class SecurityUtil {
    
    // Pure fabrication: provides utility, not a domain object
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        return null;
    }
    
    public static UUID getCurrentUserId() {
        // Extract user ID from security context
        String email = getCurrentUserEmail();
        // ... lookup user in database
        return userId;
    }
    
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}

// Used in controllers to reduce coupling
@RestController
public class PostController {
    
    @PostMapping
    public ResponseEntity<PostDTO> createPost(@RequestBody PostRequest request) {
        String userEmail = SecurityUtil.getCurrentUserEmail();  // Clean abstraction
        // Instead of: SecurityContextHolder.getContext()...
    }
}

// Benefits:
// ✅ Hides Spring Security API details
// ✅ Controllers don't import security classes
// ✅ Easy to change security implementation
```

#### Example 2: GlobalExceptionHandler (Pure Fabrication)
```java
// GlobalExceptionHandler doesn't represent a business concept
// But provides necessary exception handling

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // Pure fabrication: handles system-wide concerns
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(...) { }
    
    @ExceptionHandler(BannedUserException.class)
    public ResponseEntity<BannedUserErrorResponse> handleBannedUserException(...) { }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(...) { }
}

// Benefits:
// ✅ Centralized error handling
// ✅ Consistent error responses
// ✅ Controllers don't handle exceptions
```

#### Example 3: JwtTokenProvider (Pure Fabrication)
```java
// JwtTokenProvider doesn't represent a domain concept
// But provides JWT operations

@Component
public class JwtTokenProvider {
    
    @Value("${app.auth.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.auth.jwt.expiration}")
    private long jwtExpiration;
    
    // Pure fabrication: encapsulates JWT details
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration * 1000);
        
        return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    public String getEmailFromJWT(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        return claims.getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

// Benefits:
// ✅ Encapsulates JWT library
// ✅ Easy to swap JWT implementation
// ✅ Services don't know JWT details
```

#### Example 4: DataInitializationConfig (Pure Fabrication)
```java
// DataInitializationConfig doesn't represent a business concept
// But provides test data initialization

@Configuration
public class DataInitializationConfig {
    
    @PostConstruct
    public void initializeData() {
        // Pure fabrication: creates test objects for development
        
        User admin = User.builder()
            .email("admin@test.com")
            .password(passwordEncoder.encode("admin123"))
            .build();
        
        userRepository.save(admin);
        // ... more test data
    }
}

// Benefits:
// ✅ Separates test data from business logic
// ✅ Easy to enable/disable
// ✅ Centralized initialization
```

### Pure Fabrication Checklist

- ✅ SecurityUtil - Encapsulates security details
- ✅ GlobalExceptionHandler - Centralized error handling
- ✅ JwtTokenProvider - Encapsulates JWT operations
- ✅ DataInitializationConfig - Test data initialization
- ✅ Mapper utilities - DTOto entity conversion

---

## 8. Indirection (Intermediaries)

**Definition:** Use intermediaries/indirection to reduce coupling between classes.

### Why It Matters
- ✅ Classes don't communicate directly
- ✅ Easy to add logic at intermediary point
- ✅ Changes isolated to intermediary
- ✅ Better flexibility

### Implementation in Social App

#### Example 1: Service Layer Indirection
```
❌ Direct coupling:
Controller → Repository → Database

✅ Indirection via Service:
Controller → Service → Repository → Database
```

```java
// ✅ Service layer provides indirection
@RestController
public class PostController {
    private final PostService postService;  // ← Indirection!
    
    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPost(@PathVariable UUID id) {
        // Controller doesn't know about repository
        PostDTO post = postService.getPostById(id);
        return ResponseEntity.ok(post);
    }
}

// Service is the intermediary
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    
    @Override
    public PostDTO getPostById(UUID id) {
        // Service adds business logic indirectly
        Post post = postRepository.findById(id)
            .orElseThrow(...);
        // Can add caching, logging, validation here
        return mapToDTO(post);
    }
}

// ❌ Without indirection (tight coupling):
// @RestController
// public class PostController {
//     private final PostRepository postRepository;  // Controller knows about DB!
//     
//     @GetMapping("/{id}")
//     public Post getPost(@PathVariable UUID id) {
//         return postRepository.findById(id).orElse(null);
//     }
// }
```

#### Example 2: DTO Indirection
```java
// ✅ DTOs provide indirection between API and entities
@RestController
public class PostController {
    
    @PostMapping
    public ResponseEntity<PostDTO> createPost(@RequestBody PostRequest request) {
        // DTOs are intermediaries
        PostDTO createdPost = postService.createPost(...);
        return ResponseEntity.ok(createdPost);  // Return DTO, not entity
    }
}

// DTOs decouple entity structure from API contract
@Data
@Builder
public class PostDTO {
    // Only expose necessary fields
    private UUID id;
    private String content;
    private PostStatus status;
}

// Entity can have many internal fields
@Entity
public class Post {
    private UUID id;
    private String content;
    private PostStatus status;
    private User author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Comment> comments;
    private List<Like> likes;
    // ... many more internal fields
}

// ❌ Without indirection:
// @RestController
// public class PostController {
//     @GetMapping("/{id}")
//     public Post getPost(@PathVariable UUID id) {
//         return postRepository.findById(id).orElse(null);  // Returns entity
//     }
// }
// Problems:
// - API exposes all internal fields
// - API contract tied to entity
// - Entity changes break clients
```

#### Example 3: Authentication Filter Indirection
```java
// JwtAuthenticationFilter is an intermediary for auth
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        
        // Filter is the intermediary
        try {
            String jwt = getJwtFromRequest(request);
            
            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                String email = jwtTokenProvider.getEmailFromJWT(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                
                // Set authentication indirectly via SecurityContext
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Auth error", ex);
        }
        
        filterChain.doFilter(request, response);  // Continue chain
    }
}

// ✅ Benefits:
// - Controllers don't handle auth directly
// - Auth logic is centralized
// - Easy to add new auth mechanisms
```

### Indirection Checklist

- ✅ Service layer between Controller and Repository
- ✅ DTOs between entities and API responses
- ✅ Filters/interceptors for cross-cutting concerns
- ✅ No direct coupling between distant layers
- ✅ Changes isolated to intermediary

---

## 9. Protected Variations (Stability)

**Definition:** Identify points of variation/instability and protect against them.

### Why It Matters
- ✅ System stable despite external changes
- ✅ Encapsulate unstable details
- ✅ Easy to swap implementations
- ✅ Future-proof design

### Implementation in Social App

#### Example 1: Entity-Database Abstraction
```java
// ❌ Unprotected variation: Depends on specific database
// If database changes from PostgreSQL to MongoDB, code breaks

// ✅ Protected variation: Repository abstracts database
public interface PostRepository extends JpaRepository<Post, UUID> {
    Page<Post> findByAuthorId(UUID authorId, Pageable pageable);
    Optional<Post> findById(UUID id);
}

// Service doesn't know database technology
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;  // Interface protects against DB changes
    
    @Override
    public PostDTO getPostById(UUID id) {
        Post post = postRepository.findById(id).orElseThrow(...);
        return mapToDTO(post);
        // Works with PostgreSQL, MongoDB, Redis, Files, etc.
        // Just need different Repository implementation
    }
}

// Can swap database without changing service code
// Spring can provide different PostRepository implementations
```

#### Example 2: DTO-Entity Variation
```java
// ❌ Unprotected variation: Entity structure exposed
// If add fields to Entity, API contract changes

// ✅ Protected variation: DTOs stable, entities flexible
@Data
@Builder
public class PostDTO {
    // API contract - stable
    private UUID id;
    private String content;
    private PostStatus status;
}

@Entity
public class Post {
    // Internal structure - can vary
    private UUID id;
    private String content;
    private PostStatus status;
    private User author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Comment> comments;
    private List<Like> likes;
    private int viewCount;           // Add new field
    private boolean isFeatured;      // Add new field
    // ... entity variations don't affect API
}

// Mapper isolates variations
private PostDTO mapToDTO(Post post) {
    return PostDTO.builder()
        .id(post.getId())
        .content(post.getContent())
        .status(post.getStatus())
        // Only expose specified fields
        .build();
}
```

#### Example 3: Configuration Protection
```java
// ✅ Configuration file protects variations
// application.yml
```yaml
app:
  auth:
    jwt:
      secret: ${JWT_SECRET:default-secret-key}
      expiration: ${JWT_EXPIRATION:86400}
  mail:
    smtp:
      host: ${MAIL_HOST:localhost}
      port: ${MAIL_PORT:8025}
  database:
    url: ${DB_URL:jdbc:h2:mem:testdb}
    username: ${DB_USER:admin}
    password: ${DB_PASSWORD:password}
```

```java
// Service uses injected config
@Component
public class JwtTokenProvider {
    
    @Value("${app.auth.jwt.secret}")
    private String jwtSecret;  // Protected by configuration
    
    @Value("${app.auth.jwt.expiration}")
    private long jwtExpiration;
    
    // Implementation protected from environment variations
}

// ✅ Benefits:
// - Change environment without code changes
// - Different configs for dev/test/prod
// - No hardcoded values
```

#### Example 4: Exception Hierarchy Protection
```java
// ✅ Custom exceptions protect against library variations
// If change JWT library, only JwtTokenProvider needs update

public abstract class AppException extends RuntimeException {
    // Abstract exception protects against variations
}

public class AuthenticationException extends AppException {
    // Custom exception - service knows this
    public AuthenticationException(String message) {
        super(message);
    }
}

// Service throws custom exception
public class AuthServiceImpl implements AuthService {
    @Override
    public AuthResponse authenticate(AuthRequest authRequest) {
        try {
            // Authentication logic
            User user = userRepository.findByEmail(authRequest.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));
            
            // Password check
            if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
                throw new AuthenticationException("Invalid credentials");
            }
            
            // Generate token
            String token = jwtTokenProvider.generateToken(user.getEmail());
            return new AuthResponse(user.getId(), token);
        } catch (AppException ex) {
            // Service knows AppException, not JWT library exceptions
            throw ex;
        }
    }
}

// ❌ Bad: Service throws JWT library exceptions
// throw new JwtException(...)  // Couples service to JWT library
```

### Protected Variations Checklist

- ✅ DTOs protect from entity changes
- ✅ Repository interface protects from database changes
- ✅ Configuration protects from environment variations
- ✅ Custom exceptions protect from library variations
- ✅ Service interfaces protect from implementation changes

---

## Summary

| Pattern | Benefit | Key Practice |
|---|---|---|
| **Information Expert** | Encapsulation | Class knows its domain |
| **Creator** | Clear responsibility | Creator makes objects |
| **Controller** | Mediation | Handles requests/events |
| **Low Coupling** | Independence | Minimal dependencies |
| **High Cohesion** | Organization | Related logic together |
| **Polymorphism** | Flexibility | No type checking |
| **Pure Fabrication** | Better design | Create utility classes |
| **Indirection** | Flexibility | Use intermediaries |
| **Protected Variations** | Stability | Shield against change |

---

**See Also:** [SOLID Principles](./SOLID_PRINCIPLES.md)

**Last Updated:** April 15, 2026
