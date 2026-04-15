# My Posts Appeal Button - Technical Architecture

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      My Posts Screen (FXML)                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                  TableView<PostDTO>                      │   │
│  │  ┌─────────────────────────────────────────────────────┐ │   │
│  │  │  Content  │  Status   │  Created  │  Action        │ │   │
│  │  ├─────────────────────────────────────────────────────┤ │   │
│  │  │           │           │           │ [Hidden]       │ │   │
│  │  │  PUBLISHED│           │           │                │ │   │
│  │  ├─────────────────────────────────────────────────────┤ │   │
│  │  │           │           │           │ [Hidden]       │ │   │
│  │  │  FLAGGED  │           │           │                │ │   │
│  │  ├─────────────────────────────────────────────────────┤ │   │
│  │  │           │           │           │ [Appeal Btn]   │ │   │
│  │  │  REMOVED  │ ← Status  │           │ ← Custom Cell  │ │   │
│  │  └─────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Custom Cell Rendering Flow

```
TableCell<PostDTO, Void> updateItem()
│
├─ Check: empty || getIndex() < 0
│  │
│  ├─ YES → setGraphic(null)
│  │        return
│  │
│  └─ NO → Continue
│
├─ Get PostDTO post from TableView.getItems()
│
├─ Check: post.getStatus().equals("REMOVED")
│  │
│  ├─ YES → Appeal button should be visible
│  │        │
│  │        ├─ appealButton.setVisible(true)
│  │        ├─ appealButton.setManaged(true)
│  │        └─ setGraphic(cellContent)
│  │            └─ cellContent = HBox containing appealButton
│  │
│  └─ NO → Appeal button should be hidden
│           │
│           ├─ appealButton.setVisible(false)
│           ├─ appealButton.setManaged(false)
│           └─ setGraphic(null)
│
└─ End
```

## User Interaction Flow

```
User Views My Posts Screen
          │
          ▼
    Table Loads
          │
          ▼
    For Each Post (Row)
          │
          ├─ Create TableCell
          │
          ├─ Get PostDTO
          │
          ├─ Check Status
          │  │
          │  ├─ REMOVED → Show Appeal Button
          │  │            │
          │  │            ▼
          │  │         User Sees: [Appeal]
          │  │            │
          │  │            ▼
          │  │         User Clicks Appeal
          │  │            │
          │  │            ▼
          │  │         Dialog Opens
          │  │            │
          │  │            ├─ Show Post Preview
          │  │            ├─ Show TextArea
          │  │            ├─ Show OK/Cancel
          │  │            │
          │  │            ▼
          │  │         User Enter Reason
          │  │            │
          │  │            ▼
          │  │         User Clicks OK
          │  │            │
          │  │            ▼
          │  │         API: submitPostAppeal()
          │  │            │
          │  │            ├─ POST /appeals/posts
          │  │            ├─ {"postId": "...", "reason": "..."}
          │  │            │
          │  │            ▼
          │  │         Response Check
          │  │            │
          │  │            ├─ Success → Show "Success!" Alert
          │  │            │           Refresh Table
          │  │            │
          │  │            └─ Error → Show "Error" Alert
          │  │
          │  └─ Other Status → No Button
          │                   User Sees: (empty)
          │
          └─ Next Post

End
```

## Code Structure

```
MyPostsController.java
│
├─ @FXML Fields
│  ├─ TableView<PostDTO> myPostsTable
│  ├─ TableColumn<PostDTO, String> contentColumn
│  ├─ TableColumn<PostDTO, String> statusColumn
│  ├─ TableColumn<PostDTO, String> createdAtColumn
│  ├─ TableColumn<PostDTO, Void> actionsColumn ← NEW
│  ├─ Button backButton
│  ├─ Button refreshButton
│  └─ Spinner<Integer> pageSpinner
│
├─ initialize()
│  ├─ setupTableColumns()
│  ├─ setupActionsColumn() ← NEW
│  ├─ setupPagination()
│  ├─ setupBackButton()
│  ├─ setupRefreshButton()
│  └─ loadMyPosts()
│
├─ setupActionsColumn() ← NEW
│  │
│  └─ actionsColumn.setCellFactory(...)
│     │
│     └─ new TableCell<PostDTO, Void>() {
│        │
│        ├─ Button appealButton
│        ├─ HBox cellContent
│        │
│        ├─ init block { /* setup button */ }
│        │
│        └─ updateItem() { /* conditional rendering */ }
│
├─ openPostAppealDialog(PostDTO post) ← NEW
│  │
│  ├─ Dialog<String> creation
│  ├─ Dialog content setup
│  └─ Submit handler
│
├─ submitPostAppeal(PostDTO post, String reason) ← NEW
│  │
│  ├─ Validation
│  ├─ API call via apiService
│  ├─ Success handling
│  └─ Error handling
│
├─ loadMyPosts()
├─ goBackToFeed()
└─ showAlert()
```

## Cell Content Structure

```
HBox cellContent (Centered)
│
└─ Button appealButton
   │
   ├─ Text: "Appeal"
   ├─ Style: "-fx-font-size: 11; -fx-padding: 5 10;"
   ├─ OnAction: openPostAppealDialog()
   │
   └─ Visibility Control
      │
      ├─ If Status == REMOVED
      │  └─ visible(true)
      │     managed(true)
      │     graphic shown
      │
      └─ Else
         └─ visible(false)
            managed(false)
            graphic null
```

## Dialog Structure

```
Dialog<String> "Appeal Post Removal"
│
└─ DialogPane
   │
   ├─ Header: "Appeal the removal of your post"
   │
   ├─ Content: VBox (spacing: 10, width: 400)
   │  ├─ Label: "Post: [summary]"
   │  │  └─ Style: bold, wrap text
   │  │
   │  ├─ Label: "Explain why this post should be restored:"
   │  │
   │  └─ TextArea: reasonTextArea
   │     ├─ Rows: 6
   │     ├─ Wrap Text: true
   │     ├─ Editable: true
   │     └─ Style: white background, 5px padding
   │
   ├─ Button Types
   │  ├─ OK
   │  └─ Cancel
   │
   └─ Return Value
      └─ String (reason text)
         If OK: reason from TextArea
         If Cancel: empty Optional
```

## API Integration

```
Frontend                          Backend
   │                                │
   ├─ User clicks Appeal            │
   │  button (status==REMOVED)      │
   │                                │
   ├─ Dialog opens                  │
   │  User enters reason            │
   │                                │
   ├─ OK button clicked             │
   │                                │
   ├─ submitPostAppeal()            │
   │  ├─ Validation check           │
   │  ├─ API call                   │
   │  └──────────────────────────────► POST /appeals/posts
   │     POST /api/v1/appeals/posts │
   │     {                           │
   │       "postId": "uuid",         │
   │       "reason": "string"        │
   │     }                           │
   │                                │
   │                                ├─ Validate request
   │                                ├─ Verify post exists
   │                                ├─ Verify post status
   │                                ├─ Verify user is author
   │                                ├─ Prevent duplicates
   │                                ├─ Create appeal entity
   │                                ├─ Save to database
   │                                │
   │     ◄─────────────────────────┤─ 201 Created
   │     AppealDTO response          │ or
   │                                │ 409 Conflict
   │                                │ or
   │                                │ 404 Not Found
   │                                │
   ├─ Process response              │
   │  ├─ If 201/200                 │
   │  │  ├─ Show "Success!" alert   │
   │  │  ├─ loadMyPosts()           │
   │  │  └─ Refresh table           │
   │  │                             │
   │  └─ If Error                   │
   │     ├─ Show error alert        │
   │     └─ Keep table as-is        │
   │                                │
   └─ Dialog closes                 │
```

## State Management

```
MyPostsController State
│
├─ currentPage: int (pagination)
│
├─ pageSize: int (items per page)
│
└─ myPostsTable: TableView<PostDTO>
   │
   └─ Items: ObservableList<PostDTO>
      │
      └─ For Each PostDTO Item
         │
         ├─ id: UUID
         ├─ content: String
         ├─ status: String ← Determines button visibility
         ├─ createdAt: LocalDateTime
         └─ ... other fields

         If status == "REMOVED"
         │
         └─ actionsColumn renders
            │
            └─ TableCell shows [Appeal] button
```

## Event Handling

```
Appeal Button Click
│
├─ Event: EventHandler<ActionEvent>
│  │
│  └─ onClick()
│     │
│     ├─ Get TableCell index
│     ├─ Get PostDTO from table items
│     └─ Call openPostAppealDialog(post)
│
└─ In Dialog
   │
   ├─ User enters reason
   │
   ├─ OK Click → dialog.showAndWait() returns Optional<String>
   │  │
   │  ├─ ifPresent() → submitPostAppeal(post, reason)
   │  │
   │  └─ Cancel Click → empty Optional
   │
   └─ Dialog closes

submitPostAppeal(post, reason)
│
├─ Validate reason not empty
├─ Call apiService.submitPostAppeal(postId, reason)
├─ Handle response
│  ├─ Success → Refresh table
│  └─ Failure → Show error
└─ Close dialog
```

## Performance Considerations

1. **Cell Factory**: Created once per column, reused for all cells
2. **Custom Cell**: Only updates when table items change or cell becomes visible
3. **Lazy Loading**: Posts loaded paginated (10 per page by default)
4. **Status Check**: Simple string equality check (O(1))
5. **Button Click**: Does not block UI (dialog is modal)
6. **API Call**: Asynchronous (future implementation consideration)

## Accessibility

- Button has descriptive text: "Appeal"
- Dialog has header text explaining purpose
- TextArea is clearly labeled
- Buttons are standard types (OK, Cancel)
- TextField validation before submission

## Browser/Framework Compatibility

- **JavaFX Version**: 21+ (required for Dialog and TableView features)
- **JDK**: 17+ (for Lombok and Stream API)
- **Platform**: Cross-platform (Windows, Linux, macOS)

## Memory Usage

- **Per Table Row**: ~200 bytes (PostDTO + cell references)
- **Custom Cell**: ~1KB (Button, HBox, ListenerHandlers)
- **Dialog**: ~2KB (created on-demand, disposed after close)
- **Total Typical**: ~50KB for 10 posts on screen

## Testing Strategy

### Unit Tests
```
✓ Test setupActionsColumn() renders correctly
✓ Test openPostAppealDialog() shows dialog
✓ Test submitPostAppeal() validates input
✓ Test status == "REMOVED" shows button
✓ Test status != "REMOVED" hides button
```

### Integration Tests
```
✓ Test full appeal flow from click to submission
✓ Test API error handling
✓ Test table refresh after appeal
✓ Test dialog cancellation
✓ Test validation messages
```

### UI Tests
```
✓ Load My Posts screen
✓ Verify button visibility by status
✓ Click Appeal button
✓ Submit appeal form
✓ Verify success/error message
```
