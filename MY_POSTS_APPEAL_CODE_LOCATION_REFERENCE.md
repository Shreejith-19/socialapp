# My Posts Appeal Button - Code Location Reference

## Quick File Navigation

### File 1: my-posts.fxml
**Path:** `frontend-javafx/src/main/resources/com/example/socialapp/frontend/view/my-posts.fxml`

**Location of Change:**
```
Line ~62-68: TableView columns definition

BEFORE:
  <TableColumn fx:id="contentColumn" prefWidth="400.0" text="Content" />
  <TableColumn fx:id="statusColumn" prefWidth="150.0" text="Status" />
  <TableColumn fx:id="createdAtColumn" prefWidth="150.0" text="Created Date" />
  </columns>

AFTER:
  <TableColumn fx:id="contentColumn" prefWidth="400.0" text="Content" />
  <TableColumn fx:id="statusColumn" prefWidth="150.0" text="Status" />
  <TableColumn fx:id="createdAtColumn" prefWidth="150.0" text="Created Date" />
  <TableColumn fx:id="actionsColumn" prefWidth="120.0" text="Actions" />    ← ADDED
  </columns>

Search: Look for "</columns>" closing tag in TableView
```

---

### File 2: MyPostsController.java
**Path:** `frontend-javafx/src/main/java/com/example/socialapp/frontend/controller/MyPostsController.java`

**Change 1: Import Statements (Line ~8-16)**
```
Location: Top of file with other imports

ADD:
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
```

**Change 2: Field Declaration (Line ~25)**
```
Location: In @FXML fields section

BEFORE:
@FXML private TableColumn<PostDTO, String> createdAtColumn;
@FXML private Button backButton;

AFTER:
@FXML private TableColumn<PostDTO, String> createdAtColumn;
@FXML private TableColumn<PostDTO, Void> actionsColumn;      ← ADDED
@FXML private Button backButton;
```

**Change 3: initialize() Method (Line ~35-43)**
```
Location: initialize() method

BEFORE:
public void initialize(URL location, ResourceBundle resources) {
    setupTableColumns();
    setupPagination();
    setupBackButton();
    setupRefreshButton();
    loadMyPosts();
}

AFTER:
public void initialize(URL location, ResourceBundle resources) {
    setupTableColumns();
    setupActionsColumn();                   ← ADDED
    setupPagination();
    setupBackButton();
    setupRefreshButton();
    loadMyPosts();
}
```

**Change 4: New Method setupActionsColumn() (After line ~75)**
```
Location: After setupTableColumns() method

ADD NEW METHOD:
private void setupActionsColumn() {
    actionsColumn.setCellFactory(column -> new TableCell<PostDTO, Void>() {
        private final Button appealButton = new Button("Appeal");
        private final HBox cellContent = new HBox();

        {
            appealButton.setStyle("-fx-font-size: 11; -fx-padding: 5 10;");
            appealButton.setOnAction(event -> {
                PostDTO post = getTableView().getItems().get(getIndex());
                openPostAppealDialog(post);
            });
            
            cellContent.setAlignment(Pos.CENTER);
            cellContent.setSpacing(5);
            cellContent.getChildren().add(appealButton);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || getIndex() < 0) {
                setGraphic(null);
            } else {
                PostDTO post = getTableView().getItems().get(getIndex());
                
                if ("REMOVED".equals(post.getStatus())) {
                    appealButton.setVisible(true);
                    appealButton.setManaged(true);
                    setGraphic(cellContent);
                } else {
                    appealButton.setVisible(false);
                    appealButton.setManaged(false);
                    setGraphic(null);
                }
            }
        }
    });
}
```

**Change 5: New Method openPostAppealDialog() (After setupPagination())**
```
Location: After setupRefreshButton() method, ~line 120

ADD NEW METHOD: openPostAppealDialog(PostDTO post) { ... }

~60 lines, creates and shows appeal dialog
```

**Change 6: New Method submitPostAppeal() (After openPostAppealDialog())**
```
Location: After openPostAppealDialog() method, ~line 180

ADD NEW METHOD: submitPostAppeal(PostDTO post, String reason) { ... }

~25 lines, submits appeal through API
```

---

### File 3: ApiService.java
**Path:** `frontend-javafx/src/main/java/com/example/socialapp/frontend/service/ApiService.java`

**Location of Change:** After submitAppeal() method

```
Line ~111-113: submitAppeal() method exists

AFTER THIS, ADD:

/**
 * Submit a post appeal.
 */
public Optional<Object> submitPostAppeal(String postId, String reason) {
    log.info("Submitting appeal for post: {}", postId);
    return restClient.submitPostAppeal(postId, reason);
}
```

**Search:** Look for "submitAppeal" method, add new method right after it

---

### File 4: RestApiClient.java
**Path:** `frontend-javafx/src/main/java/com/example/socialapp/frontend/service/RestApiClient.java`

**Location of Change:** After submitAppeal() method

```
Line ~553: submitAppeal() method ends with "return Optional.empty();"

AFTER THIS, ADD NEW METHOD:

/**
 * Submit an appeal for a removed post.
 * 
 * @param postId the post ID
 * @param reason reason for appeal
 * @return Optional containing response if successful
 */
public Optional<Object> submitPostAppeal(String postId, String reason) {
    if (authToken == null) return Optional.empty();
    try {
        JsonObject request = new JsonObject();
        request.addProperty("postId", postId);
        request.addProperty("reason", reason);

        URL url = URI.create(BASE_URL + "/appeals/posts").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", CONTENT_TYPE);
        conn.setRequestProperty("Authorization", "Bearer " + authToken);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(request.toString().getBytes());
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
            String responseBody = readResponse(conn);
            log.info("Post appeal submitted for post: {}", postId);
            return Optional.of(responseBody);
        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || 
                   responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
            log.warn("Unauthorized to submit post appeal");
            this.authToken = null;
            return Optional.empty();
        }
    } catch (Exception e) {
        log.error("Error submitting post appeal", e);
    }
    return Optional.empty();
}
```

**Search:** Look for "submitAppeal" method end (~line 553), add new method right after

---

## Visual Code Map

### MyPostsController.java Structure
```
┌─ Lines 1-30: Package, imports, class declaration
├─ Lines 31-50: FXML fields @FXML
│  └─ Line ~28: ADD → @FXML private TableColumn<PostDTO, Void> actionsColumn;
├─ Lines 51-65: initialize() method
│  └─ Line ~54: ADD → setupActionsColumn();
├─ Lines 66-80: setupTableColumns() method
├─ Lines 81-105: NEW → setupActionsColumn() method ← ADD HERE
├─ Lines 106-120: setupPagination() method
├─ Lines 121-125: setupBackButton() method
├─ Lines 126-130: setupRefreshButton() method
├─ Lines 131-165: NEW → openPostAppealDialog() method ← ADD HERE
├─ Lines 166-200: NEW → submitPostAppeal() method ← ADD HERE
├─ Lines 201-220: loadMyPosts() method
├─ Lines 221-230: goBackToFeed() method
└─ Lines 231-237: showAlert() method
```

---

## Line-by-Line Edit Checklist

### Step 1: Update FXML
- [ ] Open my-posts.fxml
- [ ] Find: `</columns>` tag (around line 65)
- [ ] Before it, add: `<TableColumn fx:id="actionsColumn" prefWidth="120.0" text="Actions" />`
- [ ] Save file

### Step 2: Update MyPostsController
- [ ] Open MyPostsController.java
- [ ] At top with imports, add: `import javafx.geometry.Pos;`
- [ ] At top with imports, add: `import javafx.scene.layout.HBox;`
- [ ] Find: `@FXML private TableColumn<PostDTO, String> createdAtColumn;`
- [ ] After it, add: `@FXML private TableColumn<PostDTO, Void> actionsColumn;`
- [ ] Find: `setupTableColumns();` in initialize()
- [ ] After it, add: `setupActionsColumn();`
- [ ] Find: `setupTableColumns()` method closing brace (ends with `}`)
- [ ] After it, add entire new `setupActionsColumn()` method (lines 1-42 above)
- [ ] Find: `setupRefreshButton()` method closing brace
- [ ] After it, add entire new `openPostAppealDialog()` method
- [ ] After that, add entire new `submitPostAppeal()` method
- [ ] Save file

### Step 3: Update ApiService
- [ ] Open ApiService.java
- [ ] Find: `submitAppeal(String banId, String reason)` method
- [ ] Find where it ends: `return restClient.submitAppeal(banId, reason);`
- [ ] After the closing brace `}`, add new `submitPostAppeal()` method
- [ ] Save file

### Step 4: Update RestApiClient
- [ ] Open RestApiClient.java
- [ ] Find: `submitAppeal(String banId, String reason)` method
- [ ] Find where it ends: `return Optional.empty();` (around line 553)
- [ ] After the closing brace `}`, add new `submitPostAppeal()` method
- [ ] Save file

---

## Verification Checklist

After making changes:

- [ ] No compile errors
- [ ] FXML field matches FXML definition
- [ ] initialize() calls setupActionsColumn()
- [ ] All new methods have JavaDoc comments
- [ ] All imports are present
- [ ] Constructor/initialization code matches
- [ ] Try-catch blocks present for exception handling
- [ ] Logging statements present (log.info, log.error)
- [ ] No references to undefined classes
- [ ] Imports not duplicated

---

## Quick Diff Summary

| File | Change Type | Lines | Location |
|------|-------------|-------|----------|
| my-posts.fxml | Add column | 1 | Line ~65 |
| MyPostsController.java | Add imports | 2 | Top of file |
| MyPostsController.java | Add field | 1 | Line ~28 |
| MyPostsController.java | Modify method | 1 | initialize() |
| MyPostsController.java | Add method | 42 | After setupTableColumns() |
| MyPostsController.java | Add method | 45 | After setupPagination() |
| MyPostsController.java | Add method | 25 | After openPostAppealDialog() |
| ApiService.java | Add method | 5 | After submitAppeal() |
| RestApiClient.java | Add method | 40 | After submitAppeal() |

---

## IDE Quick Navigation

### IntelliJ IDEA
```
Ctrl+G → Go to Line
Ctrl+F → Find text
Ctrl+Shift+F → Find in files
Ctrl+Alt+L → Format code
```

### VS Code
```
Ctrl+G → Go to Line
Ctrl+F → Find text
Ctrl+Shift+F → Find in files
Shift+Alt+F → Format code
```

### Eclipse
```
Ctrl+G → Go to Line
Ctrl+H → Find in files
Ctrl+Shift+O → Organize imports
Ctrl+Shift+F → Format code
```

---

## Common Editing Mistakes to Avoid

1. ❌ Mismatching TableColumn type: `TableColumn<PostDTO, String>` 
   ✅ Correct type: `TableColumn<PostDTO, Void>` (for Void, we render custom content)

2. ❌ Forgetting imports: `Pos`, `HBox`, `Dialog`, `VBox`, `TextArea`
   ✅ Add all necessary imports

3. ❌ Wrong method position: Adding code in wrong class method
   ✅ Ensure code added to correct method/class

4. ❌ Missing closing braces: `}` in custom cell factory
   ✅ Match all opening/closing braces

5. ❌ Incorrect button status check: `"REMOVED" != "Removed"`
   ✅ Match exact string: `"REMOVED".equals(post.getStatus())`

---

## Post-Edit Compilation Test

Run in terminal:
```bash
cd frontend-javafx
mvn clean compile
```

Expected: `BUILD SUCCESS` with no errors

If errors:
- Check import statements
- Verify class/method names
- Check method signatures
- Ensure no syntax errors (extra brackets, semicolons, etc.)
