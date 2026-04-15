# Development Guidelines

This document provides best practices, coding standards, and conventions for the Social Media Content Moderation System project.

## Table of Contents
1. [Code Organization](#code-organization)
2. [Naming Conventions](#naming-conventions)
3. [Java Coding Standards](#java-coding-standards)
4. [Spring Framework Best Practices](#spring-framework-best-practices)
5. [Database & JPA](#database--jpa)
6. [Security Best Practices](#security-best-practices)
7. [Testing Guidelines](#testing-guidelines)
8. [Git Workflow](#git-workflow)
9. [Documentation](#documentation)

---

## Code Organization

### Package Structure

```
com.example.socialapp
├── config          # Configuration classes (Spring, Security, Database)
├── constant        # Application constants and enums
├── controller      # REST controllers
├── dto             # Data Transfer Objects
├── entity          # JPA entities
├── exception       # Custom exceptions
├── repository      # Data access layer (Spring Data JPA)
├── security        # Security components (JWT, authentication)
├── service         # Business logic (interfaces and implementations)
│   └── impl        # Service implementations
├── util            # Utility classes
└── view            # View/template related classes (if applicable)
```

### Single Responsibility Principle
- Each class should have a single, well-defined responsibility
- Controllers should only handle HTTP requests/responses
- Services should contain business logic
- Repositories should handle data persistence only

---

## Naming Conventions

### Classes
```java
// Controllers
public class UserController { }
public class AuthController { }

// Services (Interface + Implementation)
public interface UserService { }
public class UserServiceImpl implements UserService { }

// Repositories
public interface UserRepository extends JpaRepository<User, Long> { }

// Entities
public class User { }
public class Role { }

// DTOs
public class UserDTO { }
public class AuthRequest { }
public class AuthResponse { }

// Exceptions (end with "Exception")
public class ResourceNotFoundException extends RuntimeException { }
public class AuthenticationException extends RuntimeException { }

// Configuration
public class SecurityConfig { }
public class JpaConfig { }

// Utilities (end with "Util" or "Utilities")
public class SecurityUtil { }
public class DateUtil { }
```

### Variables and Methods
```java
// Use camelCase for variables and methods
private String firstName;
private List<User> activeUsers;

public UserDTO getUserById(Long id) { }
public void validateUserEmail(String email) { }

// Constants use UPPER_SNAKE_CASE
public static final String ROLE_ADMIN = "ROLE_ADMIN";
public static final long DEFAULT_TIMEOUT = 5000L;
```

### Database
```java
// Table names: lowercase with underscores
@Table(name = "users")
@Table(name = "user_roles")

// Column names: lowercase with underscores
@Column(name = "first_name")
@Column(name = "email_verified")

// Foreign keys: table_id
@JoinColumn(name = "user_id")
@JoinColumn(name = "role_id")
```

---

## Java Coding Standards

### Code Style
- Use 4 spaces for indentation (no tabs)
- Maximum line length: 120 characters
- Use Lombok annotations to reduce boilerplate:
  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public class User {
      // fields only
  }
  ```

### Null Safety
```java
// Use Optional instead of null checks
public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
}

// Use Lombok's @NonNull
public void processUser(@NonNull User user) {
    // user is guaranteed non-null
}
```

### Exception Handling
```java
// Create custom exceptions for specific cases
public class ResourceNotFoundException extends RuntimeException {
    // implementation
}

// Use specific exception types
try {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
} catch (ResourceNotFoundException ex) {
    log.error("User not found", ex);
    // handle appropriately
}
```

### Logging
```java
@Slf4j
public class UserService {
    public UserDTO getUser(Long id) {
        log.debug("Fetching user with ID: {}", id);
        UserDTO user = userRepository.findById(id)
            .orElseThrow(() -> {
                log.error("User not found: {}", id);
                return new ResourceNotFoundException("User", "id", id);
            });
        log.info("User fetched successfully: {}", id);
        return user;
    }
}
```

**Logging Levels:**
- `DEBUG`: Development and detailed diagnostic info
- `INFO`: General informational messages (service start, user login)
- `WARN`: Warning conditions (deprecated usage, missing optional config)
- `ERROR`: Error conditions (exceptions, failures)

---

## Spring Framework Best Practices

### Dependency Injection
```java
// ✅ Constructor injection (preferred)
@Service
public class UserService {
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// ❌ Field injection (avoid)
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
}
```

### Bean Configuration
```java
// ✅ Use @Bean methods for explicit configuration
@Configuration
public class AppConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// ✅ Use @Component for simple auto-wiring
@Component
public class EmailService {
    // implementation
}
```

### Service Layer
```java
// Services should use @Transactional
@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    // Use @Transactional(readOnly = true) for queries
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        return userRepository.findById(id)
            .map(this::mapToDTO)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
    
    // Transactional with write operations
    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        // Create and save user
    }
}
```

### Controllers
```java
// Use @RestController for JSON responses
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    // Use appropriate HTTP methods
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        // implementation
    }
    
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) {
        // implementation
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UserDTO userDTO) {
        // implementation
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        // implementation
        return ResponseEntity.noContent().build();
    }
}
```

---

## Database & JPA

### Entity Design
```java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Use @Temporal or java.time.LocalDateTime (preferred)
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Use @ManyToMany for many-to-many relationships
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
```

### Repository Methods
```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Derived query methods
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    // For complex queries, use @Query
    @Query("SELECT u FROM User u WHERE u.enabled = true")
    List<User> findAllEnabledUsers();
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = ?1")
    List<User> findByRole(Role.RoleType roleName);
}
```

### Pagination & Sorting
```java
// Use Page for pagination
@Repository
public interface UserRepository extends JpaRepository<User, Long>, PagingAndSortingRepository<User, Long> {
    Page<User> findAll(Pageable pageable);
}

// Usage in service
public Page<UserDTO> getAllUsers(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return userRepository.findAll(pageable)
        .map(this::mapToDTO);
}
```

---

## Security Best Practices

### Password Security
```java
// Always use PasswordEncoder
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// Encode password before saving
@Service
public class AuthService {
    public UserDTO register(UserDTO userDTO) {
        User user = new User();
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        // ...
    }
}
```

### JWT Security
```java
// Change secret in production
jwt:
  secret: ${JWT_SECRET:change-this-in-production}
  
// Use environment variables
jwt:
  secret: ${JWT_SECRET}  # Must be set in environment
```

### Authorization
```java
// Use role-based access control
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) {
    // Only admins can execute
}

// Check ownership when needed
@PreAuthorize("hasRole('USER') and @userService.isOwner(#id, authentication.principal.username)")
public void updateUser(Long id, UserDTO userDTO) {
    // User can only update their own profile
}
```

### Sensitive Data
```java
// Don't log sensitive data
log.debug("Processing user: {}", user.getEmail()); // OK
log.debug("User password: {}", user.getPassword()); // ❌ NEVER log passwords

// Don't return sensitive data in responses
@JsonIgnore
private String password; // Don't include in DTO
```

---

## Testing Guidelines

### Unit Tests
```java
// Test a single method in isolation
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    void testGetUserById_Success() {
        // Arrange
        Long userId = 1L;
        User user = buildTestUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        // Act
        UserDTO result = userService.getUserById(userId);
        
        // Assert
        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail());
    }
}
```

### Integration Tests
```java
// Test the full request-response cycle
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testLoginSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(authRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists());
    }
}
```

### Test Naming
```java
// testMethodName_ExpectedBehavior_GivenCondition
testGetUserById_ReturnsUser_WhenUserExists()
testGetUserById_ThrowsException_WhenUserNotFound()
testCreateUser_ReturnsCreatedUser_WithValidData()
testCreateUser_ThrowsException_WhenEmailDuplicate()
```

### Test Data Builders
```java
// Use builders for test data
private User buildTestUser() {
    return User.builder()
        .id(1L)
        .email("test@example.com")
        .firstName("Test")
        .lastName("User")
        .enabled(true)
        .build();
}
```

---

## Git Workflow

### Branch Naming
```
feature/user-authentication  # New feature
bugfix/jwt-token-expiration  # Bug fix
hotfix/security-issue        # Urgent fix
refactor/database-schema     # Code refactoring
docs/api-documentation       # Documentation
```

### Commit Messages
```
# Format: [Type] Brief description

[FEAT] Add JWT authentication
[FIX] Fix null pointer in user service
[REFACTOR] Reorganize security package
[DOCS] Update API documentation
[TEST] Add integration tests for authentication

# Detailed commit (optional)
[FEAT] Add JWT authentication

- Implement JwtTokenProvider for token generation
- Add JwtAuthenticationFilter for request validation
- Configure SecurityConfig for JWT integration
- Add JWT configuration to application.yml

Related: #123
```

### Pull Request Process
1. Create feature branch from `develop`
2. Make commits with clear messages
3. Push branch and create pull request
4. Ensure CI/CD pipeline passes
5. Request code review
6. Address feedback
7. Merge to `develop`
8. Delete feature branch

---

## Documentation

### JavaDoc
```java
/**
 * Retrieve user by ID.
 * 
 * @param id the user ID
 * @return UserDTO if found
 * @throws ResourceNotFoundException if user doesn't exist
 * @since 1.0.0
 */
public UserDTO getUserById(Long id) {
    // implementation
}
```

### README
- Link to project setup guide
- Link to API documentation
- Quick start instructions
- Technology stack
- Contributing guidelines

### Inline Comments
```java
// ✅ Good: Explains "why"
// Validate token expiration before processing request
if (isTokenExpired(token)) {
    throw new AuthenticationException("Token expired");
}

// ❌ Bad: Explains "what" (obvious from code)
// Check if token is expired
if (isTokenExpired(token)) {
    throw new AuthenticationException("Token expired");
}
```

---

## Common Patterns

### DTO Mapping
```java
private UserDTO mapToDTO(User user) {
    return UserDTO.builder()
        .id(user.getId())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .enabled(user.getEnabled())
        .roles(user.getRoles().stream()
            .map(role -> role.getName().toString())
            .collect(Collectors.toSet()))
        .build();
}
```

### Validation
```java
@Service
public class UserService {
    
    public UserDTO createUser(UserDTO userDTO) {
        // Input validation
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new ConflictException(
                "Email already exists", "email", userDTO.getEmail());
        }
        
        // Business logic
        User user = new User();
        // ... set properties
        User savedUser = userRepository.save(user);
        
        return mapToDTO(savedUser);
    }
}
```

### Response Building
```java
// Use ResponseEntity for flexible responses
@PostMapping
public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) {
    UserDTO createdUser = userService.createUser(userDTO);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(createdUser);
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
}
```

---

## Code Review Checklist

- [ ] Code follows naming conventions
- [ ] Single responsibility principle maintained
- [ ] Proper error handling with custom exceptions
- [ ] Logging at appropriate levels
- [ ] Security considerations addressed
- [ ] Input validation in place
- [ ] Unit tests written
- [ ] Integration tests for endpoints
- [ ] Documentation updated
- [ ] No hardcoded values (use constants/properties)
- [ ] No sensitive data in logs or responses
- [ ] Performance considerations addressed
- [ ] Database indexes added if needed

---

## Performance Tips

1. **Use readOnly = true** for queries
   ```java
   @Transactional(readOnly = true)
   public UserDTO getUser(Long id) { }
   ```

2. **Batch database operations**
   ```yaml
   hibernate:
     jdbc:
       batch_size: 20
   ```

3. **Use appropriate fetch strategies**
   ```java
   @ManyToMany(fetch = FetchType.LAZY)
   ```

4. **Add database indexes on frequently queried fields**
   ```java
   @Column(unique = true, nullable = false)
   @Index(name = "idx_email")
   private String email;
   ```

---

## Useful Commands

```bash
# Build project
mvn clean install

# Run tests
mvn test

# Run specific test
mvn test -Dtest=UserServiceTest

# Run application
mvn spring-boot:run

# Check code quality
mvn checkstyle:check

# Build Docker image
mvn clean package docker:build
```

---

**Last Updated:** April 9, 2024
