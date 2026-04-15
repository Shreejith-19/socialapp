# My Posts Appeal Button - Quick Reference Guide

## What Changed?

The "My Posts Status" screen now has an **Appeal** button that appears only for REMOVED posts.

## UI Flow

```
┌─────────────────────────────────────────┐
│           My Posts Status               │
├─────────────────────────────────────────┤
│ Content | Status | Created Date | Act. │
├─────────────────────────────────────────┤
│ "Hello world..." │ PUBLISHED │ 2024-01-01 │     │
│ "Test post xyz" │ FLAGGED   │ 2024-01-02 │     │
│ "Removed post"  │ REMOVED   │ 2024-01-03 │Appeal│
└─────────────────────────────────────────┘
         ▲                            ▲
    3+ hidden rows              Appeal button
    (no button shown)           (visible)
```

## User Actions

### 1. View Posts
- Navigate to "My Posts Status"
- See all posts with their status

### 2. Check Status
- REMOVED status → Appeal button visible ✓
- Other status → No button ✗

### 3. Click Appeal Button
- Opens dialog
- Shows post preview
- Shows text area for reason

### 4. Enter Reason & Submit
- Type appeal reason
- Click OK button
- API submission happens
- Success message shows
- Table refreshes

## Code Summary

### File: MyPostsController.java

**Key Changes:**
```java
// 1. Added field
@FXML private TableColumn<PostDTO, Void> actionsColumn;

// 2. Call in initialize()
setupActionsColumn();

// 3. New method for custom cell rendering
private void setupActionsColumn() {
    actionsColumn.setCellFactory(column -> new TableCell<PostDTO, Void>() {
        protected void updateItem(Void item, boolean empty) {
            if ("REMOVED".equals(post.getStatus())) {
                appealButton.setVisible(true);
                setGraphic(cellContent);
            } else {
                setGraphic(null);
            }
        }
    });
}

// 4. New method to open dialog
private void openPostAppealDialog(PostDTO post) { ... }

// 5. New method to submit appeal
private void submitPostAppeal(PostDTO post, String reason) { ... }
```

### File: my-posts.fxml

**Key Change:**
```xml
<!-- Added Actions column -->
<TableColumn fx:id="actionsColumn" prefWidth="120.0" text="Actions" />
```

### File: ApiService.java

**New Method:**
```java
public Optional<Object> submitPostAppeal(String postId, String reason) {
    return restClient.submitPostAppeal(postId, reason);
}
```

### File: RestApiClient.java

**New Method:**
```java
public Optional<Object> submitPostAppeal(String postId, String reason) {
    // POST /appeals/posts
    // { "postId": "...", "reason": "..." }
}
```

## Button Behavior

### Shows When
- Post status = "REMOVED"
- Cell is not empty
- Cell index >= 0

### Hides When
- Post status = PUBLISHED
- Post status = FLAGGED
- Cell is empty
- Table is loading

### On Click
1. Get post from table row
2. Open appeal dialog
3. User enters reason
4. Dialog shows buttons (OK, Cancel)
5. On OK: submit appeal
6. On Cancel: dismiss dialog

## Dialog Content

```
┌──────────────────────────────────────┐
│    Appeal Post Removal               │
├──────────────────────────────────────┤
│ Post: "Removed post text preview..." │
│                                      │
│ Explain why this post should be      │
│ restored:                            │
│ ┌──────────────────────────────────┐ │
│ │ [TextArea for appeal reason]     │ │
│ │ • 6 rows                         │ │
│ │ • Text wrapping enabled          │ │
│ │ • White background               │ │
│ └──────────────────────────────────┘ │
│                                      │
│    [OK]                    [Cancel]  │
└──────────────────────────────────────┘
```

## Error Messages

| Scenario | Message |
|----------|---------|
| Empty reason | "Validation Error: Please provide a reason for your appeal" |
| API fails | "Error: Failed to submit appeal. Please try again." |
| Post not found | Error from API |
| Post not REMOVED | Error from API |
| Success | "Success: Your appeal has been submitted successfully!" |

## Validation

- **Reason**: Must not be empty or whitespace
- **Post**: Must have status = "REMOVED"
- **Authentication**: User must be logged in

## API Integration

### Endpoint
```
POST /api/v1/appeals/posts
Authorization: Bearer <token>
Content-Type: application/json

{
  "postId": "uuid",
  "reason": "string"
}
```

### Success Response
```
201 Created or 200 OK
```

### Error Responses
```
400 Bad Request - Invalid input
401 Unauthorized - Not logged in
403 Forbidden - Insufficient permission
404 Not Found - Post not found
409 Conflict - Post not REMOVED or appeal exists
```

## Testing Checklist

- [ ] Post with REMOVED status shows Appeal button
- [ ] Post with PUBLISHED status hides button
- [ ] Post with FLAGGED status hides button
- [ ] Clicking Appeal button opens dialog
- [ ] Dialog shows post preview
- [ ] Dialog has TextArea for reason
- [ ] Dialog has OK and Cancel buttons
- [ ] Empty reason shows validation error
- [ ] Valid submission shows success message
- [ ] Table refreshes after successful appeal
- [ ] API endpoint receives correct data

## Styling Reference

### Appeal Button
- Font size: 11px
- Padding: 5px 10px
- Text: "Appeal"
- Cursor: pointer

### Dialog
- Width: 400px
- Spacing: 10px
- Resizable: true

### TextArea
- Row count: 6
- Wrapping: true
- Background: white
- Padding: 5px

## Implementation Time: ~2 hours

**Breakdown:**
- Modify FXML: 15 min
- Update controller: 45 min
- Add API methods: 15 min
- Testing: 45 min

## Known Limitations

- Only one appeal per post at a time
- No appeal history shown in this view
- No cooldown between appeals (backend enforces)
- Post preview limited to 100 characters

## Related Files

- Backend appeal endpoint: `/api/v1/appeals/posts`
- Backend post removal: Moderation system
- Frontend appeal screen: `AppealScreenController.java`
- Backend appeal model: `AppealServiceImpl.java`

## Support

For issues:
1. Check logs in controller
2. Verify API endpoint exists
3. Check authentication token
4. Verify post status in database
5. Check backend error responses
