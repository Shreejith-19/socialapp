# Post Appeal Feature Documentation

## Overview
The post appeal feature enables users to appeal against the removal of their posts. When a post is removed (marked as REMOVED status), the post author can submit an appeal for moderation review.

## Feature Requirements
✓ Users can appeal only posts they authored  
✓ Appeals can only be submitted for posts with REMOVED status  
✓ Each post can have only one active appeal (duplicate appeals prevented)  
✓ Appeals follow the same moderation workflow: PENDING → Moderator Review → Admin Decision  
✓ Approved appeals restore the post to PUBLISHED status  
✓ Rejected appeals keep the post in REMOVED status  

## API Endpoints

### Submit Post Appeal
**Endpoint:** `POST /api/v1/appeals/posts`  
**Authentication:** Required (USER, MODERATOR, ADMIN roles)  
**Request Body:**
```json
{
  "postId": "uuid",
  "reason": "string"
}
```

**Response (201 Created):**
```json
{
  "id": "uuid",
  "appealType": "POST",
  "banId": null,
  "postId": "uuid",
  "reason": "My post was removed incorrectly",
  "status": "PENDING",
  "moderatorReview": null,
  "adminDecision": null,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**Error Responses:**
- `400 Bad Request`: Missing postId or reason
- `403 Forbidden`: User is not the post author or lacks permission
- `404 Not Found`: Post not found
- `409 Conflict`: Post is not in REMOVED status, or appeal already exists
- `409 Conflict`: User is not the post author

### Get Appeal by ID
**Endpoint:** `GET /api/v1/appeals/{appealId}`  
**Authentication:** MODERATOR or ADMIN roles  
**Response (200 OK):** Returns full Appeal DTO

### Get Pending Appeals
**Endpoint:** `GET /api/v1/appeals/pending`  
**Authentication:** MODERATOR or ADMIN roles  
**Response (200 OK):** Array of pending appeals

### Moderator Review
**Endpoint:** `PUT /api/v1/appeals/{appealId}/review`  
**Authentication:** MODERATOR or ADMIN roles  
**Request Body:**
```json
{
  "review": "The content appears to violate policy X, but the appeal has merit"
}
```

### Admin Decision
**Endpoint:** `PUT /api/v1/appeals/{appealId}/decision`  
**Authentication:** ADMIN role only  
**Request Body:**
```json
{
  "approved": true,
  "decision": "Appeal approved. Post was removed in error and has been restored."
}
```

**Effects:**
- **Approved (true):** Post status changed from REMOVED to PUBLISHED
- **Rejected (false):** Post remains in REMOVED status

## Workflow

### Timeline
1. **User Action**: User submits post appeal (contains postId + reason)
2. **Service Validation**:
   - Verify post exists
   - Verify post is in REMOVED status
   - Verify current user is the post author
   - Verify no appeal already exists for this post
3. **Appeal Created**: Appeal entity created with:
   - `appealType = POST`
   - `post` reference set
   - `status = PENDING`
4. **Moderator Review**: Moderator adds review comments (optional but recommended)
5. **Admin Decision**: Admin approves or rejects
   - If approved: Post restored to PUBLISHED
   - If rejected: Post remains REMOVED

### Status Flow
```
PENDING → (Moderator Review) → PENDING → (Admin Decision) → APPROVED or REJECTED
```

## Database Schema

### Appeals Table Changes
Added columns:
- `appeal_type` (ENUM): Distinguishes between BAN and POST appeals
- `post_id` (UUID): Foreign key to posts table (nullable, only set for POST appeals)

Existing columns untouched:
- `ban_id` (UUID): Foreign key to bans table (nullable, only set for BAN appeals)

## Implementation Details

### Service Layer
**AppealService Interface**:
- `submitPostAppeal(UUID postId, String reason)` - New method for post appeals

**AppealServiceImpl**:
- Extracts current user from Spring Security context
- Validates user is the post author
- Handles both BAN and POST appeals via `AppealType` enum
- On approval: Post status changed to PUBLISHED in `adminDecision` method

### Repository
New query methods in AppealRepository:
- `findByPostId(UUID postId)` - Find appeal by post ID
- `existsByPostId(UUID postId)` - Check if appeal exists for post

### Controller
New endpoint in AppealController:
- `POST /api/v1/appeals/posts` - Submit post appeal
- New `PostAppealRequest` DTO for post appeals

### DTOs
**AppealDTO** updated with:
- `appealType: AppealType` - Type of appeal (BAN or POST)
- `postId: UUID` - Post ID (for POST appeals)
- `banId: UUID` - Ban ID (for BAN appeals) - nullable

## Error Handling

| Scenario | HTTP Status | Error Message |
|----------|-------------|---------------|
| Post not found | 404 | Post not found with id: {postId} |
| Post not REMOVED | 409 | Only posts with REMOVED status can be appealed |
| User not author | 403 | You can only appeal posts you authored |
| Appeal exists | 409 | An appeal for this post already exists |
| Missing postId | 400 | Post ID is required |
| Missing reason | 400 | Reason is required |

## Security

### Authorization
- **Submit Appeal**: USER, MODERATOR, ADMIN roles (must be post author for POST appeals)
- **View Appeal**: MODERATOR, ADMIN roles
- **Moderator Review**: MODERATOR, ADMIN roles
- **Admin Decision**: ADMIN role only

### Validation
- Current user extracted from Spring Security context
- Post author verification prevents non-authors from appealing
- Post status validation ensures only REMOVED posts can be appealed
- Duplicate appeal prevention via database query

## Testing Recommendations

### Unit Tests
- Test post appeal submission with valid data
- Test rejection of appeals for non-existent posts
- Test rejection of appeals for non-REMOVED posts
- Test rejection when user is not the post author
- Test duplicate appeal prevention

### Integration Tests
- Test full workflow: appeal submission → moderator review → approval → post restored
- Test rejection workflow: appeal submission → moderator review → rejection → post stays REMOVED
- Test authorization checks for all endpoints

### Test Data
```java
// Create test post with REMOVED status
Post removedPost = Post.builder()
    .content("Test content")
    .status(PostStatus.REMOVED)
    .author(testUser)
    .build();

// Submit appeal
POST /api/v1/appeals/posts
{
  "postId": "removed-post-uuid",
  "reason": "This post was removed incorrectly"
}

// Approve appeal
PUT /api/v1/appeals/{appealId}/decision
{
  "approved": true,
  "decision": "Appeal granted. Post restored."
}

// Verify post status is PUBLISHED
```

## Migration Notes

### Database
- Hibernate `ddl-auto: update` automatically adds new columns
- No manual migration script required for H2 (development)
- For production databases, create migration script:
  ```sql
  ALTER TABLE appeals ADD COLUMN appeal_type VARCHAR(10) NOT NULL DEFAULT 'BAN';
  ALTER TABLE appeals ADD COLUMN post_id VARCHAR(36);
  ALTER TABLE appeals ADD FOREIGN KEY (post_id) REFERENCES posts(id);
  ALTER TABLE appeals ADD INDEX idx_post_id (post_id);
  ALTER TABLE appeals ADD INDEX idx_appeal_type (appeal_type);
  ```

### Code Changes Summary
1. New enum: `AppealType` (BAN, POST)
2. Modified entity: `Appeal` (added appealType, postId)
3. Updated DTOs: `AppealDTO` (added appealType, postId)
4. Updated service: `AppealService` + `AppealServiceImpl` (new method)
5. Updated controller: `AppealController` (new endpoint + request DTO)
6. Updated repository: `AppealRepository` (new query methods)

## Future Enhancements

- **Appeal History**: Track appeal revisions/history
- **Appeals By Type**: Separate endpoints for ban vs. post appeals
- **Appeal Metrics**: Track approval rates, average resolution time
- **Notifications**: Notify users when appeal status changes
- **Appeal Comments**: Allow moderators to add detailed feedback
- **Content Review**: Allow moderators to view the removed post content during review
- **Rate Limiting**: Implement cooldown period before user can appeal again
