# SOLID Principles in Social App Backend

## Overview

SOLID is an acronym for five design principles that help create maintainable, scalable, and flexible software.

---

## 1. Single Responsibility Principle (SRP)

**Definition:** A class should have only one reason to change, meaning each class should have a single responsibility.

### Why It Matters
- ✅ Easier to test (mock one responsibility)
- ✅ Easier to understand (focused class)
- ✅ Easier to maintain (change one thing without breaking others)
- ✅ Better code reuse

### Implementation in Social App

#### PostController
**Responsibility:** Handle HTTP requests for posts

```java
@RestController
@RequestMapping("/v1/posts")
public class PostController {
    private final PostService postService;
    private final UserRepository userRepository;
    private final LikeService likeService;
    
    // Only handles HTTP request/response
    // Delegates business logic to services
    @PostMapping
    public ResponseEntity<PostDTO> createPost(@Valid @RequestBody PostRequest postRequest) {
        // Get current user
        String userEmail = SecurityUtil.getCurrentUserEmail();
        User currentUser = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        // Delegate to service (SRP - controller doesn't know HOW to create)
        PostDTO createdPost = postService.createPost(currentUser.getId(), postRequest.getContent());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }
}
```

#### PostService Interface & Implementation
**Responsibility:** Business logic for post operations

```java
// Service Interface
public interface PostService {
    PostDTO createPost(UUID authorId, String content);
    PostDTO getPostById(UUID postId);
    Page<PostDTO> getPostsByAuthor(UUID authorId, Pageable pageable);
    void deletePost(UUID postId);
}

// Service Implementation
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final ModerationService moderationService;
    
    // Knows business rules about posts
    @Override
    public PostDTO createPost(UUID authorId, String content) {
        // Business logic here: validate, check bans, moderate content
        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", authorId.toString()));
        
        // Check if user can post
        if (!canPost(author)) {
            throw new IllegalArgumentException("User cannot post");
        }
        
        // Create post
        Post post = Post.builder()
            .content(content)
            .author(author)
            .status(PostStatus.PUBLISHED)
            .build();
        
        // Check if needs moderation
        if (moderationService.reviewContent(content)) {
            post.setStatus(PostStatus.FLAGGED);
            moderationService.sendToQueue(post);
        }
        
        return mapToDTO(postRepository.save(post));
    }
    
    private boolean canPost(User user) {
        // Check ban status, permissions, etc.
        return !user.getStatus().equals("BANNED");
    }
}
```

#### PostRepository
**Responsibility:** Database operations for posts

```java
@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    Page<Post> findByAuthorId(UUID authorId, Pageable pageable);
    Page<Post> findByStatus(PostStatus status, Pageable pageable);
    List<Post> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);
}
```

#### GlobalExceptionHandler
**Responsibility:** Centralized exception handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .message(ex.getMessage())
            .error("RESOURCE_NOT_FOUND")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(BannedUserException.class)
    public ResponseEntity<BannedUserErrorResponse> handleBannedUserException(
            BannedUserException ex, WebRequest request) {
        BannedUserErrorResponse errorResponse = BannedUserErrorResponse.builder()
            .status(HttpStatus.FORBIDDEN.value())
            .message(ex.getMessage())
            .error("USER_BANNED")
            .banMessage(ex.getBanMessage())
            .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
}
```

#### UserDTO
**Responsibility:** Transfer user data

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private UUID id;
    private String email;
    private String username;
    private String status;
    private LocalDateTime createdAt;
    // Only contains user data - nothing else
}
```

### SRP Checklist

- ✅ PostController - Only handles HTTP
- ✅ PostService - Only business logic
- ✅ PostRepository - Only database
- ✅ GlobalExceptionHandler - Only exception handling
- ✅ UserDTO - Only data transfer
- ✅ SecurityUtil - Only security utilities
- ✅ JwtTokenProvider - Only JWT operations
- ✅ ModerationService - Only moderation logic
- ✅ AppealService - Only appeal handling

---

## 2. Open/Closed Principle (OCP)

**Definition:** Software entities should be open for extension but closed for modification.

### Why It Matters
- ✅ Add new features without changing existing code
- ✅ Less risk of breaking existing functionality
- ✅ Better testability
- ✅ More flexible design

### Implementation in Social App

#### Service Interfaces (Open for Extension)
```java
// Closed for modification - this interface doesn't change
public interface PostService {
    PostDTO createPost(UUID authorId, String content);
    PostDTO getPostById(UUID postId);
    Page<PostDTO> getPostsByAuthor(UUID authorId, Pageable pageable);
}

// Open for extension - can create new implementations
// Implementation 1: Standard post service
public class PostServiceImpl implements PostService {
    @Override
    public PostDTO createPost(UUID authorId, String content) { /*...*/ }
}

// Implementation 2: Cached post service (new feature)
public class CachedPostServiceImpl implements PostService {
    private final PostService delegate;
    private final Cache cache;
    
    @Override
    public PostDTO createPost(UUID authorId, String content) {
        // Add caching logic without modifying original service
        return delegate.createPost(authorId, content);
    }
}

// Implementation 3: Audited post service (new feature)
public class AuditedPostServiceImpl implements PostService {
    private final PostService delegate;
    private final AuditLog auditLog;
    
    @Override
    public PostDTO createPost(UUID authorId, String content) {
        PostDTO result = delegate.createPost(authorId, content);
        auditLog.log("Post created by " + authorId);
        return result;
    }
}

// Use any implementation without changing client code
public class PostController {
    // Can be any implementation of PostService
    private final PostService postService;  // Interface type
    
    public ResponseEntity<PostDTO> createPost(@Valid @RequestBody PostRequest request) {
        return ResponseEntity.ok(postService.createPost(userId, request.getContent()));
        // Works with PostServiceImpl, CachedPostServiceImpl, AuditedPostServiceImpl, etc.
    }
}
```

#### Exception Handling (Open for Extension)
```java
// GlobalExceptionHandler is open for extension
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // Handle existing exception type
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFoundException(...) { /*...*/ }
    
    // Add new handler without modifying existing ones
    @ExceptionHandler(BannedUserException.class)
    public ResponseEntity<?> handleBannedUserException(...) { /*...*/ }
    
    // Add another handler
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> handleConflictException(...) { /*...*/ }
    
    // Add more handlers as needed without modifying existing handlers
}
```

#### Enums for Strategy (Open for Extension)
```java
// Use enums instead of if-else chains
public enum DecisionType {
    APPROVED,
    REMOVED,
    ESCALATED
}

// Service uses enum - open for adding new decision types
public interface ModerationService {
    ModerationDecision makeDecision(
        DecisionType decisionType,  // Open for extension
        UUID postId,
        String reason
    );
}

// Implementation uses strategy pattern
public class ModerationServiceImpl implements ModerationService {
    @Override
    public ModerationDecision makeDecision(DecisionType decisionType, UUID postId, String reason) {
        switch(decisionType) {
            case APPROVED -> approvePost(postId);
            case REMOVED -> rejectPost(postId);
            case ESCALATED -> escalatePost(postId);
        }
        // To add new decision type: add enum value + case statement
        // Don't need to change interface or other logic
    }
}
```

### OCP Checklist

- ✅ Service interfaces allow multiple implementations
- ✅ Exception handlers can be added without modifying existing ones
- ✅ Enums allow new types without changing base logic
- ✅ Configuration classes allow parameter changes
- ✅ Custom exceptions can be added to exception hierarchy

---

## 3. Liskov Substitution Principle (LSP)

**Definition:** Objects of a superclass should be replaceable with objects of its subclasses without breaking the application.

### Why It Matters
- ✅ Derived classes must be usable in place of base classes
- ✅ Polymorphic code behaves predictably
- ✅ Contract must be honored by all implementations

### Implementation in Social App

#### UserDetails Interface (Spring Security)
```java
// Spring Security's UserDetails interface
public interface UserDetails extends Serializable {
    String getUsername();
    String getPassword();
    Collection<? extends GrantedAuthority> getAuthorities();
    // ... other methods
}

// Our implementation must honor the contract
@Entity
public class User implements UserDetails {
    private UUID id;
    private String email;
    private String password;
    private List<Role> roles;
    
    @Override
    public String getUsername() {
        return email;  // Returns valid username
    }
    
    @Override
    public String getPassword() {
        return password;  // Returns valid password
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority(role.getName()))
            .collect(Collectors.toList());  // Returns valid authorities
    }
}

// Used in Spring Security
AuthenticationProvider authProvider = new DaoAuthenticationProvider();
authProvider.setUserDetailsService(customUserDetailsService);
// Can substitute any UserDetails implementation

// Our User class works because it honors the UserDetails contract
UserDetails user = userRepository.findByEmail("user@test.com");
String password = user.getPassword();  // Contract honored
List<GrantedAuthority> roles = user.getAuthorities();  // Contract honored
```

#### Repository Pattern (JpaRepository)
```java
// Interface defining repository contract
public interface PostRepository extends JpaRepository<Post, UUID> {
    Page<Post> findByAuthorId(UUID authorId, Pageable pageable);
    Optional<Post> findById(UUID id);
}

// Spring provides implementations - substitutable without code changes
// Could be:
// - Spring Data JPA implementation
// - MongoDB implementation
// - In-memory implementation for testing
// All honor the same contract

// Service uses repository through interface
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;  // Interface type
    
    @Override
    public PostDTO getPostById(UUID postId) {
        Post post = postRepository.findById(postId)  // Works with any implementation
            .orElseThrow(() -> new ResourceNotFoundException(...));
        return mapToDTO(post);
    }
}
```

#### Custom Exceptions (Substitutability)
```java
// Base exception
public abstract class AppException extends RuntimeException {
    // Contract: all AppExceptions are runtime exceptions
}

// Derived exceptions honor the contract
public class ResourceNotFoundException extends AppException {
    // Substitutable for AppException
    public ResourceNotFoundException(String resource, String field, String value) {
        super(String.format("%s not found with %s: %s", resource, field, value));
    }
}

public class BannedUserException extends AppException {
    // Substitutable for AppException
    private String banMessage;
    public BannedUserException(String message, String banMessage) {
        super(message);
        this.banMessage = banMessage;
    }
}

// Handler accepts base type - works with all derived types
@ExceptionHandler(AppException.class)
public ResponseEntity<?> handleAppException(AppException ex, WebRequest request) {
    // Works for ResourceNotFoundException, BannedUserException, etc.
    return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
}
```

### LSP Checklist

- ✅ All service implementations honor the interface contract
- ✅ Custom exceptions are properly derived
- ✅ Repository implementations are substitutable
- ✅ Derived classes don't violate base class expectations
- ✅ No surprising behavior when using polymorphism

---

## 4. Interface Segregation Principle (ISP)

**Definition:** Clients should not be forced to depend on interfaces they don't use.

### Why It Matters
- ✅ Minimize dependencies
- ✅ Each interface is focused
- ✅ Easier to implement (don't need unused methods)
- ✅ Cleaner contracts

### Implementation in Social App

#### Segregated Service Interfaces

**PostService** - Only post operations
```java
public interface PostService {
    PostDTO createPost(UUID authorId, String content);
    PostDTO getPostById(UUID postId);
    Page<PostDTO> getPostsByAuthor(UUID authorId, Pageable pageable);
    void deletePost(UUID postId);
}
```

**UserService** - Only user operations
```java
public interface UserService {
    UserDTO getUserById(UUID id);
    UserDTO getUserByEmail(String email);
    UserDTO updateUser(UUID id, UserDTO userDTO);
    void deleteUser(UUID id);
}
```

**ModerationService** - Only moderation operations
```java
public interface ModerationService {
    boolean reviewContent(String content);
    void flagPost(Post post);
    void sendToQueue(Post post);
    ModerationDecision makeDecision(DecisionType type, UUID postId, String reason);
}
```

**AppealService** - Only appeal operations
```java
public interface AppealService {
    AppealDTO submitAppeal(UUID banId, String reason);
    AppealDTO submitPostAppeal(UUID postId, String reason);
    AppealDTO adminDecision(UUID appealId, boolean approved, String decision);
}
```

#### Controller Dependencies (ISP Applied)
```java
// ✅ Good - depends only on needed interface
@RestController
@RequestMapping("/v1/posts")
public class PostController {
    private final PostService postService;       // Only needs post operations
    private final LikeService likeService;       // Only needs like operations
    
    @PostMapping
    public ResponseEntity<PostDTO> createPost(...) {
        return ResponseEntity.ok(postService.createPost(...));
    }
}

// ✅ Good - different controller, different dependencies
@RestController
@RequestMapping("/v1/moderation")
public class ModeratorController {
    private final ModerationService moderationService;  // Only needs moderation
    private final AppealService appealService;          // Only needs appeals
    
    @PostMapping("/posts/{postId}/approve")
    public ResponseEntity<?> approvePost(...) {
        return ResponseEntity.ok(moderationService.approvePost(...));
    }
}

// ❌ Bad - would force controller to implement unused methods
// public interface SuperService extends PostService, UserService, 
//     AppealService, ModerationService, ReportService { }
// This would be Interface Segregation violation
```

#### Repository Interfaces (ISP Applied)
```java
// Focused interface for users
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
}

// Focused interface for posts
public interface PostRepository extends JpaRepository<Post, UUID> {
    Page<Post> findByAuthorId(UUID authorId, Pageable pageable);
    Page<Post> findByStatus(PostStatus status, Pageable pageable);
}

// Focused interface for appeals
public interface AppealRepository extends JpaRepository<Appeal, UUID> {
    List<Appeal> findByStatus(AppealStatus status);
    Optional<Appeal> findByBanId(UUID banId);
}

// Service depends on specific repository, not generic one
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;  // Only post operations
    // Doesn't need UserRepository, AppealRepository, etc.
}
```

### ISP Checklist

- ✅ Each service interface has focused responsibility
- ✅ Controllers depend only on needed services
- ✅ No "fat interfaces" that force unused methods
- ✅ Repositories are focused on their entity
- ✅ Client code only depends on what it uses

---

## 5. Dependency Inversion Principle (DIP)

**Definition:** High-level modules should not depend on low-level modules. Both should depend on abstractions.

### Why It Matters
- ✅ Low coupling between modules
- ✅ Easy to swap implementations (testing, new features)
- ✅ Dependency flow is clear
- ✅ Better testability (use mocks/stubs)

### Implementation in Social App

#### High-Level Module (Controller) → Abstraction (Service Interface)
```
❌ Bad: Controller → ServiceImpl (depends on concrete class)
✅ Good: Controller → ServiceInterface → ServiceImpl (depends on abstraction)
```

```java
// Layer structure - all depend on abstractions
// Controller uses Service interface
@RestController
public class PostController {
    private final PostService postService;  // ← Depends on INTERFACE (abstraction)
    
    public PostController(PostService postService) {  // ← Injected
        this.postService = postService;
    }
    
    @PostMapping
    public ResponseEntity<PostDTO> createPost(@RequestBody PostRequest request) {
        PostDTO result = postService.createPost(...);  // Doesn't know implementation
        return ResponseEntity.ok(result);
    }
}

// Service interface
public interface PostService {
    PostDTO createPost(UUID authorId, String content);
}

// Service implementation depends on Repository interface
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;  // ← Depends on INTERFACE
    
    public PostServiceImpl(PostRepository postRepository) {  // ← Injected
        this.postRepository = postRepository;
    }
    
    @Override
    public PostDTO createPost(UUID authorId, String content) {
        Post post = new Post(...);
        postRepository.save(post);  // Doesn't know if DB is PostgreSQL, MongoDB, etc.
        return mapToDTO(post);
    }
}

// Repository interface
public interface PostRepository extends JpaRepository<Post, UUID> {
    // Abstract operations - no implementation details
}

// Repository implementation (low-level)
@Repository
public class JpaPostRepository implements PostRepository {
    // Spring provides implementation
}
```

#### Dependency Injection (DIP Pattern)
```java
// Configuration - connects abstractions to implementations
@Configuration
public class BeanConfig {
    
    // DIP: Provide abstraction, not concrete class
    @Bean
    public PostService postService(PostRepository repository) {
        return new PostServiceImpl(repository);  // Inject abstraction
    }
    
    @Bean
    public UserService userService(UserRepository repository) {
        return new UserServiceImpl(repository);  // Inject abstraction
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // Inject abstraction
    }
}

// Usage - high-level doesn't know about the dependency details
public class PostController {
    private final PostService postService;  // High-level depends on abstraction
    
    public PostController(PostService postService) {
        this.postService = postService;  // Injected - can be any PostService implementation
    }
}
```

#### Testing Benefits (DIP Advantage)
```java
// Easy to test because of DIP
// Can substitute real service with mock service

@Test
public void testCreatePost() {
    // Create mock repository
    PostRepository mockRepo = mock(PostRepository.class);
    
    // Create service with mock (instead of real database)
    PostService postService = new PostServiceImpl(mockRepo);
    
    // Test without a real database
    PostDTO result = postService.createPost(UUID.randomUUID(), "Hello");
    
    // Verify behavior
    verify(mockRepo).save(any(Post.class));
}
```

#### Constructor Injection (DIP Best Practice)
```java
// ✅ Good - DIP via constructor injection
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    public PostServiceImpl(
        PostRepository postRepository,
        UserRepository userRepository
    ) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }
}

// ❌ Bad - Service creates its own dependencies (tightly coupled)
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository = new JpaPostRepository();  // Tightly coupled!
}

// ❌ Bad - Using @Autowired field injection (less clear)
public class PostServiceImpl implements PostService {
    @Autowired
    private PostRepository postRepository;  // Implicit dependency
}
```

### DIP Checklist

- ✅ Controllers depend on Service interfaces
- ✅ Services depend on Repository interfaces
- ✅ All dependencies are injected
- ✅ No `new` keyword for creating dependencies
- ✅ Easy to mock for testing
- ✅ Configuration class connects abstractions to implementations

---

## Summary

| Principle | Benefit | Key Practice |
|---|---|---|
| **SRP** | Easy to test & maintain | One responsibility per class |
| **OCP** | Easy to extend | Interfaces + implementations |
| **LSP** | Predictable polymorphism | Honor interface contracts |
| **ISP** | Focused dependencies | Segregate interfaces by use |
| **DIP** | Loose coupling | Depend on abstractions |

---

**See Also:** [GRASP Principles](./GRASP_PRINCIPLES.md)

**Last Updated:** April 15, 2026
