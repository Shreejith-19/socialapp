# Post Appeal Feature - Quick Reference

## What's New?
Users can now appeal the removal of their posts through a moderation workflow.

## Key Changes

### New Files Created
| File | Purpose |
|------|---------|
| `AppealType.java` | Enum for appeal type (BAN, POST) |
| `AppealServiceImplPostAppealTest.java` | Unit tests for post appeals |
| `AppealControllerPostAppealIntegrationTest.java` | Integration tests for API |
| `POST_APPEAL_FEATURE.md` | Feature documentation |
| `POST_APPEAL_IMPLEMENTATION_GUIDE.md` | Implementation guide |

### Modified Files
| File | Changes |
|------|---------|
| `Appeal.java` | Added appealType, post; made ban nullable |
| `AppealDTO.java` | Added appealType, postId |
| `AppealRepository.java` | New methods: findByPostId, existsByPostId |
| `AppealService.java` | New method: submitPostAppeal, getAppealByPostId |
| `AppealServiceImpl.java` | Implement post appeal logic |
| `AppealController.java` | New endpoint POST /api/v1/appeals/posts |

## API Endpoint

### Submit Post Appeal
```
POST /api/v1/appeals/posts
Content-Type: application/json
Authorization: Bearer <token>

{
  "postId": "uuid",
  "reason": "explanation"
}
```

**Response:** 201 Created with AppealDTO

## Business Rules

1. **Only REMOVED posts can be appealed** - Post must have REMOVED status
2. **Only post author can appeal** - User must be the post creator
3. **One appeal per post** - Duplicate appeals rejected  
4. **Approval restores post** - Post changes from REMOVED to PUBLISHED
5. **Rejection keeps post removed** - Post stays REMOVED status

## Workflow

```
User submits appeal
    ↓
Service validates (post exists, REMOVED status, user is author)
    ↓
Appeal created with status = PENDING
    ↓
Moderator reviews and adds comments
    ↓
Admin approves or rejects
    ↓
If approved: Post restored to PUBLISHED
If rejected: Post stays REMOVED
```

## Error Codes

| Error | Status | Reason |
|-------|--------|--------|
| Post not found | 404 | Post doesn't exist |
| Post not REMOVED | 409 | Only REMOVED posts can be appealed |
| Not author | 409 | Can only appeal own posts |
| Appeal exists | 409 | Post already has an appeal |
| Invalid input | 400 | Missing postId or reason |
| Not authenticated | 401 | No auth token provided |
| Wrong role | 403 | Insufficient permissions |

## Code Examples

### Service Layer
```java
// Submit post appeal
AppealDTO appeal = appealService.submitPostAppeal(postId, "reason");

// Get appeal by post ID
Optional<AppealDTO> appeal = appealService.getAppealByPostId(postId);

// Approve post appeal
AppealDTO approved = appealService.adminDecision(appealId, true, "Approved");
// Effect: Post status changed to PUBLISHED

// Reject post appeal
AppealDTO rejected = appealService.adminDecision(appealId, false, "Rejected");
// Effect: Post remains REMOVED
```

### Controller Test
```java
@WithMockUser(roles = "USER")
@Test
void submitPostAppeal() throws Exception {
    AppealController.PostAppealRequest request = 
        new AppealController.PostAppealRequest(postId, "reason");
    
    mockMvc.perform(post("/v1/appeals/posts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.appealType").value("POST"));
}
```

## Testing

### Run Tests
```bash
# Run all tests
mvn test

# Run post appeal unit tests
mvn test -Dtest=AppealServiceImplPostAppealTest

# Run post appeal integration tests
mvn test -Dtest=AppealControllerPostAppealIntegrationTest
```

### Test Scenarios
- ✓ Submit appeal for valid REMOVED post
- ✓ Reject appeal if post not REMOVED
- ✓ Prevent non-author from appealing
- ✓ Prevent duplicate appeals
- ✓ Restore post on approval
- ✓ Keep post removed on rejection

## Security

### Authentication Required
All endpoints require valid JWT token

### Authorization
- **Submit:** USER, MODERATOR, ADMIN roles
- **View:** MODERATOR, ADMIN roles
- **Review:** MODERATOR, ADMIN roles
- **Decide:** ADMIN role only

### Validation
- Current user extracted from Spring Security context
- Post author verified from database
- Post status verified before appeal
- Duplicate appeals prevented

## Database Schema

### New Columns in `appeals` Table
| Column | Type | Nullable | Note |
|--------|------|----------|------|
| appeal_type | VARCHAR(10) | N | BAN or POST |
| post_id | VARCHAR(36) | Y | Foreign key to posts |

### New Indexes
- `idx_appeal_type` on appeal_type
- `idx_post_id` on post_id

## Configuration

No additional configuration needed. Uses existing:
- Spring Security
- JPA/Hibernate
- Transaction management

## Troubleshooting

### "Only posts with REMOVED status can be appealed"
**Solution:** Post must have status = REMOVED. Check post status before appealing.

### "You can only appeal posts you authored"  
**Solution:** Signed-in user must be the post author. Use correct user account.

### "An appeal for this post already exists"
**Solution:** Post already has an appeal. Cannot submit another appeal.

### Post not restored after approval
**Solution:** Check `/api/v1/appeals/{appealId}` to verify status is APPROVED. 
Verify admin actually set `approved=true`.

## Related Features

### Ban Appeals (Existing)
- Same workflow structure
- Appeals against user bans
- Endpoint: `POST /api/v1/appeals`

### Post Moderation (Existing)
- Posts flagged or removed via moderation
- Status: PUBLISHED → FLAGGED → REMOVED
- Standalone feature, complements appeals

## Documentation

- **Feature Overview:** [POST_APPEAL_FEATURE.md](./POST_APPEAL_FEATURE.md)
- **Implementation Details:** [POST_APPEAL_IMPLEMENTATION_GUIDE.md](./POST_APPEAL_IMPLEMENTATION_GUIDE.md)
- **Code Comments:** JavaDoc in source files
- **Tests:** Unit and integration tests in `src/test/java`

## Key Classes & Methods

### Service
- `AppealService.submitPostAppeal(UUID postId, String reason)`
- `AppealService.getAppealByPostId(UUID postId)`
- `AppealService.adminDecision(UUID appealId, boolean approved, String decision)`

### Controller
- `POST /api/v1/appeals/posts` - Submit post appeal
- `GET /api/v1/appeals/{appealId}` - View appeal
- `PUT /api/v1/appeals/{appealId}/review` - Moderator review
- `PUT /api/v1/appeals/{appealId}/decision` - Admin decision

### Entity
- `Appeal.appealType` - Type of appeal
- `Appeal.post` - Reference to appealed post
- `Appeal.ban` - Reference to ban (if BAN appeal)
- `PostStatus.REMOVED` - Post can be appealed

### Repository
- `AppealRepository.findByPostId(UUID)`
- `AppealRepository.existsByPostId(UUID)`

## Performance Considerations

- Indexes on `post_id` and `appeal_type` for quick lookups
- Eager loading of post and author relationships
- Database query validates post author (vs. code-level check)
- No N+1 query issues with proper JPA configuration

## What's Next?

1. Deploy to test environment
2. Run integration tests against database
3. Test API with Postman/Swagger UI
4. Monitor logs for errors
5. Gather user feedback
6. Plan enhancements

## Quick Checklist

- [ ] Pull latest code
- [ ] Run `mvn clean install`
- [ ] Run tests: `mvn test`
- [ ] Start application: `mvn spring-boot:run`
- [ ] Access Swagger UI: http://localhost:8080/swagger-ui.html
- [ ] Test POST /api/v1/appeals/posts endpoint
- [ ] Verify database schema updated
- [ ] Check logs for errors
