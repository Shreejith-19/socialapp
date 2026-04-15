# My Posts Screen - Appeal Button Implementation

## Overview
The "My Posts Status" JavaFX screen has been updated to show conditional "Appeal" buttons for removed posts. Users can now click the Appeal button to submit an appeal for a post that has been removed.

## Implementation Details

### 1. FXML Changes
**File:** `my-posts.fxml`

**Change:** Added a new `actionsColumn` TableColumn
```xml
<TableColumn fx:id="actionsColumn" prefWidth="120.0" text="Actions" />
```

### 2. Controller Changes
**File:** `MyPostsController.java`

#### Added Field
```java
@FXML private TableColumn<PostDTO, Void> actionsColumn;
```

#### New Methods

##### setupActionsColumn()
Sets up a custom cell factory for the actions column:
- Creates a custom `TableCell<PostDTO, Void>` with embedded Appeal button
- Button is styled with `-fx-font-size: 11; -fx-padding: 5 10;`
- Button is **visible only** when post status equals "REMOVED"
- Button is **hidden** for all other statuses (PUBLISHED, FLAGGED, etc.)
- On click, opens the appeal dialog

```java
private void setupActionsColumn() {
    actionsColumn.setCellFactory(column -> new TableCell<PostDTO, Void>() {
        // Custom cell rendering with conditional visibility
        @Override
        protected void updateItem(Void item, boolean empty) {
            if ("REMOVED".equals(post.getStatus())) {
                appealButton.setVisible(true);
                setGraphic(cellContent);
            } else {
                appealButton.setVisible(false);
                setGraphic(null);
            }
        }
    });
}
```

##### openPostAppealDialog(PostDTO post)
Opens a dialog where user can enter appeal reason:
- Dialog title: "Appeal Post Removal"
- Shows post preview (first 100 chars)
- TextArea for user to enter appeal reason
- OK and Cancel buttons
- On OK: calls `submitPostAppeal()`
- On Cancel: closes dialog

```java
private void openPostAppealDialog(PostDTO post) {
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle("Appeal Post Removal");
    // ... setup content ...
    Optional<String> result = dialog.showAndWait();
    result.ifPresent(reason -> submitPostAppeal(post, reason));
}
```

##### submitPostAppeal(PostDTO post, String reason)
Submits the appeal through the API:
- Validates that reason is not empty
- Calls `apiService.submitPostAppeal(postId, reason)`
- Shows success alert on success
- Shows error alert on failure
- Refreshes the posts table after successful submission

### 3. API Service Changes
**File:** `ApiService.java`

**New Method:**
```java
public Optional<Object> submitPostAppeal(String postId, String reason) {
    log.info("Submitting appeal for post: {}", postId);
    return restClient.submitPostAppeal(postId, reason);
}
```

### 4. REST Client Changes
**File:** `RestApiClient.java`

**New Method:**
```java
public Optional<Object> submitPostAppeal(String postId, String reason) {
    // POST request to /appeals/posts
    // Request body:
    // {
    //   "postId": "uuid",
    //   "reason": "string"
    // }
    
    // Returns response on success (201/200)
    // Returns empty Optional on failure
    // Clears auth token on 401/403
}
```

## User Experience Flow

### 1. View Posts
User navigates to "My Posts Status" screen and sees their posts in a table:
- Content column: First 100 characters of post
- Status column: PUBLISHED, FLAGGED, or REMOVED
- Created Date column: Post creation date
- **Actions column: Appeal button (conditionally shown)**

### 2. Identify Removed Post
User sees a post with status = "REMOVED"
- Appeal button is automatically visible for that row
- Appeal button is hidden for posts with other statuses

### 3. Click Appeal Button
User clicks the Appeal button
- Appeal dialog opens
- Dialog shows:
  - Post preview (truncated content)
  - Text area to enter appeal reason
  - OK and Cancel buttons

### 4. Submit Appeal
User enters reason and clicks OK
- Appeal is sent to backend via API
- Success message shown: "Your appeal has been submitted successfully!"
- Posts table is refreshed to reflect any changes
- Dialog closes

### 5. Failure Handling
If appeal submission fails:
- Error message shown with details
- Dialog closes
- Posts table remains unchanged

## Styling

### Button Styling
```css
Appeal Button:
  - Font size: 11px
  - Padding: 5px 10px
  - Centered in cell
  - Clickable only when visible
```

### Dialog Styling
```css
Dialog Content:
  - Spacing: 10px between elements
  - Preferred width: 400px
  - TextArea: 6 rows, wrapped text
  - Post preview: bold, wrapped text
```

## Status Visibility Rules

| Post Status | Appeal Button | Reason |
|------------|---------------|--------|
| PUBLISHED  | Hidden        | Cannot appeal published posts |
| FLAGGED    | Hidden        | Post is awaiting review |
| REMOVED    | **Visible**   | **User can appeal removal** |

## Error Handling

### Validation Errors
- **Empty reason:** Shows "Validation Error: Please provide a reason for your appeal"

### API Errors
- **404 Not Found:** "Post not found"
- **409 Conflict:** "Post is not REMOVED" or "Appeal already exists"
- **401 Unauthorized:** "Unauthorized to submit appeal"
- **Generic error:** "Could not submit appeal: [error message]"

### Success Cases
- **201/200 Response:** "Your appeal has been submitted successfully!"
- Posts table is refreshed to show updated status

## Code Organization

### Controller Structure
```
MyPostsController
├── initialize()
├── setupTableColumns()
├── setupActionsColumn()          [NEW]
├── setupPagination()
├── setupBackButton()
├── setupRefreshButton()
├── loadMyPosts()
├── openPostAppealDialog()         [NEW]
├── submitPostAppeal()             [NEW]
├── goBackToFeed()
└── showAlert()
```

### Cell Factory Implementation
```
TableCell<PostDTO, Void>
├── HBox cellContent
│   └── Button appealButton
├── updateItem()
│   ├── Check if empty or no index
│   ├── Get post from table items
│   ├── If status == "REMOVED"
│   │   ├── Set visible(true)
│   │   └── Set graphic(cellContent)
│   └── Else
│       ├── Set visible(false)
│       └── Set graphic(null)
└── Button onClick handler
    └── openPostAppealDialog()
```

## Key Features

✓ **Conditional Rendering:** Appeal button only shows for REMOVED posts
✓ **Custom Dialog:** User-friendly appeal submission dialog
✓ **API Integration:** Submits appeal through REST API
✓ **Error Handling:** Comprehensive error messages
✓ **Table Refresh:** Auto-refreshes posts after successful appeal
✓ **User Feedback:** Success/error alerts inform user
✓ **Validation:** Ensures reason is not empty before submission

## Testing Recommendations

### Unit Tests
- Verify `setupActionsColumn()` creates proper cell factory
- Test `openPostAppealDialog()` with various post data
- Test `submitPostAppeal()` with valid and invalid input

### Integration Tests
- Load "My Posts" screen with mixed post statuses
- Verify Appeal button visible only for REMOVED posts
- Click Appeal button and submit form
- Verify API call is made with correct parameters
- Verify success alert shows on successful submission
- Verify table refreshes after submission

### Manual Testing
1. Create a post
2. Have admin remove the post
3. Navigate to "My Posts Status"
4. Verify post shows with REMOVED status
5. Click Appeal button
6. Enter appeal reason
7. Click OK
8. Verify success message
9. Verify posts table refreshes

## Files Modified
1. `my-posts.fxml` - Added actionsColumn
2. `MyPostsController.java` - Added appeal functionality
3. `ApiService.java` - Added submitPostAppeal() method
4. `RestApiClient.java` - Added submitPostAppeal() method

## Backend Integration

The implementation assumes the backend provides:
- `POST /api/v1/appeals/posts` endpoint
- Request body: `{ "postId": "uuid", "reason": "string" }`
- Response: AppealDTO or success response (201 Created or 200 OK)
- Error responses: 404, 409, 401, 403, etc.

This functionality integrates with the post appeal feature implemented in the backend.

## Dependencies

### JavaFX
- `javafx.scene.control.Dialog`
- `javafx.scene.control.TextArea`
- `javafx.scene.control.TableCell`
- `javafx.scene.layout.HBox`
- `javafx.geometry.Pos`

### Logging
- `lombok.extern.slf4j.Slf4j`

## Future Enhancements

- [ ] Show previous appeals for a post
- [ ] Display appeal status when viewing posts
- [ ] Add keyboard shortcut for Appeal button
- [ ] Save draft appeals locally
- [ ] Show estimated review time
- [ ] Appeal history view
- [ ] Multiple appeals per post (with cooldown)
