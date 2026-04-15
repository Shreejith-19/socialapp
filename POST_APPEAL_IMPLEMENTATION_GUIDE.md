# Post Appeal Feature - Implementation Guide

## Feature Summary
This implementation extends the Social Moderation System to support appeals for removed posts, in addition to the existing ban appeal functionality. Users can now appeal the removal of their posts through a moderation workflow.

## Architecture Overview

### Polymorphic Appeal System
The appeal system has been refactored to support multiple appeal types:

```
Appeal Entity
├── Appeal Type (BAN or POST)
├── Ban Reference (nullable)
└── Post Reference (nullable)
```

This design allows a single Appeal entity and workflow to handle both ban and post appeals without code duplication.

## Changes Made

### 1. New Enum: AppealType
**File:** `src/main/java/com/example/socialapp/enums/AppealType.java`

Distinguishes between ban and post appeals:
```java
public enum AppealType {
    BAN,    // Appeal against user ban
    POST    // Appeal for removed post
}
```

### 2. Modified Entity: Appeal
**File:** `src/main/java/com/example/socialapp/entity/Appeal.java`

**Changes:**
- Added `appealType: AppealType` (required) - Type of appeal
- Added `post: Post` (nullable) - Reference to post for POST appeals
- Changed `ban: Ban` from non-nullable to nullable
- Added database indexes:
  - `idx_post_id` on post_id column
  - `idx_appeal_type` on appeal_type column

**Schema Migration:**
```sql
-- New columns added automatically by Hibernate
ALTER TABLE appeals ADD COLUMN appeal_type VARCHAR(10) NOT NULL DEFAULT 'BAN';
ALTER TABLE appeals ADD COLUMN post_id VARCHAR(36) NULLABLE;
ALTER TABLE appeals ADD CONSTRAINT fk_appeals_post FOREIGN KEY (post_id) REFERENCES posts(id);
```

### 3. Updated DTO: AppealDTO
**File:** `src/main/java/com/example/socialapp/dto/AppealDTO.java`

**Changes:**
- Added `appealType: AppealType` - Type of appeal
- Added `postId: UUID` - Post ID (for POST appeals)
- Changed `banId: UUID` to nullable (was Non-nullable)

### 4. Updated Repository: AppealRepository
**File:** `src/main/java/com/example/socialapp/repository/AppealRepository.java`

**New Query Methods:**
```java
// Find appeal by post ID
Optional<Appeal> findByPostId(UUID postId);

// Check if appeal exists for post ID
boolean existsByPostId(UUID postId);
```

### 5. Extended Service: AppealService Interface
**File:** `src/main/java/com/example/socialapp/service/AppealService.java`

**New Method:**
```java
/**
 * Submit an appeal for a removed/rejected post.
 * User can only appeal posts they authored.
 */
AppealDTO submitPostAppeal(UUID postId, String reason);

/**
 * Get appeal by post ID.
 */
Optional<AppealDTO> getAppealByPostId(UUID postId);
```

### 6. Enhanced Service Implementation: AppealServiceImpl
**File:** `src/main/java/com/example/socialapp/service/impl/AppealServiceImpl.java`

**Key Changes:**
- Added PostRepository dependency injection
- Added UserRepository dependency injection (was missing)
- Implemented `submitPostAppeal()` with:
  - Post existence validation
  - REMOVED status verification
  - Post author verification (uses Spring Security context)
  - Duplicate appeal prevention
  - Appeal creation with `appealType = POST`
- Extended `adminDecision()` to:
  - Handle both BAN and POST appeal types
  - Restore post to PUBLISHED on approval (for POST appeals)
  - Lift ban to restore user status (for BAN appeals)

**Business Logic:**
```java
// Extract current user from Spring Security context
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String currentUsername = authentication.getName();
User currentUser = userRepository.findByUsername(currentUsername)...

// Verify user is post author
if (!post.getAuthor().getId().equals(currentUser.getId())) {
    throw new ConflictException("You can only appeal posts you authored");
}

// On approval: restore post to PUBLISHED
if (appeal.getAppealType() == AppealType.POST && approved) {
    post.setStatus(PostStatus.PUBLISHED);
    postRepository.save(post);
}
```

### 7. Extended Controller: AppealController
**File:** `src/main/java/com/example/socialapp/controller/AppealController.java`

**New Endpoint:**
```
POST /api/v1/appeals/posts
```

**Request Body:**
```json
{
  "postId": "uuid",
  "reason": "explanation why post was incorrectly removed"
}
```

**Response (201 Created):**
```json
{
  "id": "uuid",
  "appealType": "POST",
  "banId": null,
  "postId": "uuid",
  "reason": "string",
  "status": "PENDING",
  "moderatorReview": null,
  "adminDecision": null,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**New Request DTO:**
```java
public static class PostAppealRequest {
    @NotNull(message = "Post ID is required")
    private UUID postId;

    @NotBlank(message = "Reason is required")
    private String reason;
}
```

## Configuration

### No Additional Configuration Required
The feature uses existing Spring Security configuration:
- Spring Security context for user extraction
- Role-based access control (@PreAuthorize)
- Transaction management (@Transactional)

### Database Configuration
Uses existing Hibernate auto-ddl configuration:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # Schema updates automatically
```

## Testing

### Unit Tests
**File:** `src/test/java/com/example/socialapp/service/impl/AppealServiceImplPostAppealTest.java`

**Coverage:**
- ✓ Successful post appeal submission
- ✓ Post not found validation
- ✓ Post status validation (must be REMOVED)
- ✓ Duplicate appeal prevention
- ✓ Post author verification
- ✓ Current user extraction and validation
- ✓ Appeal retrieval by post ID
- ✓ Admin approval (post restored to PUBLISHED)
- ✓ Admin rejection (post remains REMOVED)
- ✓ DTO mapping for post and ban appeals

**Run Tests:**
```bash
mvn test -Dtest=AppealServiceImplPostAppealTest
```

### Integration Tests
**File:** `src/test/java/com/example/socialapp/controller/AppealControllerPostAppealIntegrationTest.java`

**Coverage:**
- ✓ POST /api/v1/appeals/posts with valid request
- ✓ Missing postId validation
- ✓ Blank reason validation
- ✓ Post not found (404)
- ✓ Post not REMOVED (409)
- ✓ User not author (403)
- ✓ Duplicate appeal (409)
- ✓ Authentication required (401)
- ✓ Role-based access (USER, MODERATOR, ADMIN)
- ✓ Response content type and structure

**Run Tests:**
```bash
mvn test -Dtest=AppealControllerPostAppealIntegrationTest
```

## Security Considerations

### Authentication
- All appeal endpoints require authentication
- Current user extracted from Spring Security context
- No assumption about authenticated user validity

### Authorization
- **Submit Appeal:** USER, MODERATOR, ADMIN roles
- **View Appeal:** MODERATOR, ADMIN roles
- **Moderator Review:** MODERATOR, ADMIN roles
- **Admin Decision:** ADMIN role only

### Post Author Verification
```java
if (!post.getAuthor().getId().equals(currentUser.getId())) {
    throw new ConflictException("You can only appeal posts you authored");
}
```
- Prevents non-authors from appealing posts
- Uses database entity reference comparison
- Logged for audit trail

### Duplicate Prevention
```java
if (appealRepository.existsByPostId(postId)) {
    throw new ConflictException("An appeal for this post already exists");
}
```
- Prevents multiple appeals for the same post
- Uses database query for consistency

## Error Handling

All errors follow consistent REST conventions:

| Scenario | HTTP Status | Error Type |
|----------|-------------|-----------|
| Post not found | 404 | ResourceNotFoundException |
| Post not REMOVED | 409 | IllegalStateException |
| Not post author | 409 | ConflictException |
| Appeal exists | 409 | ConflictException |
| Invalid input | 400 | Validation error |
| Not authenticated | 401 | UnauthorizedException |
| Wrong role | 403 | AccessDeniedException |

## Migration Guide

### For Existing Installations

#### Step 1: Code Changes
1. Merge pull request with all file changes
2. Update `pom.xml` dependencies (if any)
3. Rebuild application

#### Step 2: Database Schema
**For H2 (Development):**
- No manual migration needed
- Hibernate auto-ddl will create columns on startup

**For MySQL/PostgreSQL (Production):**
```sql
-- Backup appeals table
CREATE TABLE appeals_backup AS SELECT * FROM appeals;

-- Add new columns
ALTER TABLE appeals 
ADD COLUMN appeal_type VARCHAR(10) NOT NULL DEFAULT 'BAN',
ADD COLUMN post_id VARCHAR(36) NULL,
ADD CONSTRAINT fk_appeals_post FOREIGN KEY (post_id) REFERENCES posts(id);

-- Add indexes
CREATE INDEX idx_post_id ON appeals(post_id);
CREATE INDEX idx_appeal_type ON appeals(appeal_type);

-- Verify migration
SELECT COUNT(*) FROM appeals;
```

#### Step 3: Verification
1. Run unit tests: `mvn test -Dtest=AppealServiceImplPostAppealTest`
2. Run integration tests: `mvn test -Dtest=AppealControllerPostAppealIntegrationTest`
3. Test API manually with Swagger UI or Postman

#### Step 4: Data Validation
```sql
-- Verify existing appeals are still accessible
SELECT id, appeal_type, ban_id, post_id FROM appeals;

-- All existing appeals should have appeal_type = 'BAN' and post_id = NULL
```

### Rollback Plan

If needed, rollback changes:

```sql
-- Backup current state
CREATE TABLE appeals_new AS SELECT * FROM appeals WHERE appeal_type = 'POST';

-- Remove new constraints
ALTER TABLE appeals DROP FOREIGN KEY fk_appeals_post;

-- Drop new columns
ALTER TABLE appeals DROP COLUMN appeal_type;
ALTER TABLE appeals DROP COLUMN post_id;

-- Drop new indexes
DROP INDEX idx_post_id ON appeals;
DROP INDEX idx_appeal_type ON appeals;

-- Restore ban_id as NOT NULL
ALTER TABLE appeals MODIFY COLUMN ban_id VARCHAR(36) NOT NULL;
```

## Usage Examples

### Example 1: Submit Post Appeal
```bash
curl -X POST http://localhost:8080/api/v1/appeals/posts \
  -H "Authorization: Bearer token" \
  -H "Content-Type: application/json" \
  -d '{
    "postId": "550e8400-e29b-41d4-a716-446655440000",
    "reason": "This post was removed in error. It complies with community guidelines."
  }'
```

**Response (201 Created):**
```json
{
  "id": "650e8400-e29b-41d4-a716-446655440001",
  "appealType": "POST",
  "postId": "550e8400-e29b-41d4-a716-446655440000",
  "banId": null,
  "reason": "This post was removed in error. It complies with community guidelines.",
  "status": "PENDING",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Example 2: Moderator Review
```bash
curl -X PUT http://localhost:8080/api/v1/appeals/650e8400-e29b-41d4-a716-446655440001/review \
  -H "Authorization: Bearer token" \
  -H "Content-Type: application/json" \
  -d '{
    "review": "I reviewed the post content. The user has a valid point - the content does not violate guidelines X or Y."
  }'
```

### Example 3: Admin Decision (Approve)
```bash
curl -X PUT http://localhost:8080/api/v1/appeals/650e8400-e29b-41d4-a716-446655440001/decision \
  -H "Authorization: Bearer token" \
  -H "Content-Type: application/json" \
  -d '{
    "approved": true,
    "decision": "Appeal approved. Post restored to published status based on moderator review."
  }'
```

**Effect:** Post status changed from REMOVED to PUBLISHED

## Documentation

All features documented in:
- **Feature Guide:** [POST_APPEAL_FEATURE.md](./POST_APPEAL_FEATURE.md)
- **Implementation Guide:** This file
- **API Documentation:** [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
- **JavaDoc:** Source code comments in all modified classes

## Future Enhancements

1. **Appeal History:** Track appeal timeline and revisions
2. **Bulk Appeals:** Support batch appeal processing
3. **Appeal Metrics:** Dashboard showing appeal statistics
4. **User Notifications:** Notify users of appeal status changes
5. **Appeal Reasons:** Predefined appeal reason categories
6. **Rate Limiting:** Cooldown period between appeals
7. **Content Review:** Moderators can view flagged post during review
8. **Automated Appeals:** AI-powered pre-review of clear cases

## Support

For questions or issues:
1. Review [POST_APPEAL_FEATURE.md](./POST_APPEAL_FEATURE.md) for feature details
2. Check unit/integration tests for usage examples
3. Consult JavaDoc comments in source files
4. Review error messages and status codes

## Changelog

### Version 1.0.0 (Initial Release)
- Post appeal submission endpoint
- Post author verification
- Appeal workflow with moderator review and admin decision
- Automatic restoration of appealed posts on approval
- Comprehensive unit and integration tests
- Security validations and role-based access control
