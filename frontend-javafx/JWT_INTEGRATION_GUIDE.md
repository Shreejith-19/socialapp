/**
 * JWT TOKEN INTEGRATION GUIDE FOR DEVELOPERS
 * ============================================
 * 
 * This guide explains how JWT tokens are handled in this JavaFX frontend
 * and provides best practices for adding new authenticated API endpoints.
 * 
 * 
 * QUICK START: JWT IN ACTION
 * ==========================
 * 
 * 1. User Logs In:
 *    LoginController.handleLogin()
 *    └─ Calls restApiClient.authenticate(email, password)
 *       └─ Returns AuthResponse with token field
 *    
 * 2. Token Stored:
 *    RestApiClient.setAuthToken(token)
 *    └─ Stores in: private static String authToken
 *    SessionManager.authToken = token
 *    └─ Also stores in SessionManager for redundancy
 * 
 * 3. Token Attached to Requests:
 *    Any API call in RestApiClient checks:
 *    if (authToken != null) {
 *        conn.setRequestProperty("Authorization", "Bearer " + authToken);
 *    }
 * 
 * 4. Token Sent to Server:
 *    GET/POST request includes header:
 *    Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * 
 * 5. Token Cleared on Logout:
 *    LoginController.handleLogout()
 *    └─ Calls SessionManager.clearSession()
 *       └─ Sets authToken = null
 *       └─ Sets currentUser = null
 * 
 * 6. Session Expired Handling:
 *    If server returns 401:
 *    ErrorHandler.handleApiException()
 *    └─ Shows: "Your session has expired"
 *    └─ Clears: SessionManager.clearSession()
 *    └─ Redirects: To login.fxml
 * 
 * 
 * ARCHITECTURE: WHERE JWT IS STORED
 * ==================================
 * 
 * class RestApiClient {
 *     private static String authToken;  ← JWT stored here
 *     
 *     public static void setAuthToken(String token) {
 *         authToken = token;
 *     }
 *     
 *     private static void get(String endpoint, ...) {
 *         ...
 *         if (authToken != null) {
 *             conn.setRequestProperty("Authorization", "Bearer " + authToken);
 *         }
 *         ...
 *     }
 * }
 * 
 * How get() and post() Work:
 * 
 * 1. Generic Methods:
 *    public static <T> T get(String endpoint, Class<T> responseType)
 *    public static <T> T post(String endpoint, Object body, Class<T> responseType)
 *    └─ These methods ALWAYS attach JWT if authToken != null
 * 
 * 2. Specific Endpoint Methods:
 *    public static void createPost(String content)
 *    └─ Calls: post("posts", new PostCreateRequest(content), ...)
 *       └─ Which calls generic post() method
 *          └─ Which attaches JWT automatically
 * 
 * 3. Example Flow:
 *    createPost("My new post")
 *    └─ Calls: post("posts", postData, PostDTO.class)
 *       └─ Creates: URLConnection to "http://localhost:8080/api/v1/posts"
 *       └─ Sets Header: "Authorization: Bearer {authToken}"
 *       └─ Sets Body: {"content": "My new post"}
 *       └─ Returns: PostDTO with new post data
 * 
 * 
 * HOW TO: Add a New Authenticated Endpoint
 * =========================================
 * 
 * SCENARIO: You need to add a new API call "Favorite a post"
 * Endpoint: POST /posts/{id}/favorite
 * 
 * Step 1: Create DTO (if needed)
 * File: frontend/model/FavoriteDTO.java
 * 
 *     @Data
 *     @Builder
 *     public class FavoriteDTO {
 *         private Long postId;
 *         private Boolean isFavorited;
 *     }
 * 
 * Step 2: Add method to RestApiClient
 * File: frontend/service/RestApiClient.java
 * 
 *     public static FavoriteDTO favoritePost(Long postId) throws ApiException {
 *         String endpoint = "posts/" + postId + "/favorite";
 *         return post(endpoint, new FavoriteRequest(postId), FavoriteDTO.class);
 *         //                 ↑              ↑                                 ↑
 *         //         Uses generic post()   Request body            Response type
 *         //         which AUTOMATICALLY
 *         //         attaches JWT token!
 *     }
 * 
 * Step 3: Add method to ApiService
 * File: frontend/service/ApiService.java
 * 
 *     public FavoriteDTO favoritePost(Long postId) throws ApiException {
 *         return restApiClient.favoritePost(postId);
 *     }
 * 
 * Step 4: Add method to Controller
 * File: frontend/controller/DashboardController.java
 * 
 *     @FXML
 *     private void handleFavoriteClick(Long postId) {
 *         try {
 *             FavoriteDTO result = apiService.favoritePost(postId);
 *             // Update UI with result
 *             loadFeed();  // Reload to show favorite status
 *         } catch (ApiException e) {
 *             handleApiException(e);  // Inherited from BaseController
 *         }
 *     }
 * 
 * IMPORTANT: 
 * ✅ ALWAYS use the generic post() or get() methods
 * ✅ NEVER create URLConnection directly
 * ✅ NEVER try to attach JWT manually - it's automatic!
 * ✅ Always handle ApiException for proper error handling
 * ✅ Call controllers' handleApiException() for consistent UI
 * 
 * 
 * AUTHENTICATION FLOW DIAGRAM
 * ============================
 * 
 * User Input (Email/Password)
 *     ↓
 * LoginController.handleLogin()
 *     ↓
 * restApiClient.authenticate(email, password)
 *     ↓
 * POST /auth/login with credentials
 *     ↓
 * Backend validates, returns AuthResponse
 * {
 *     "user": { "id": 1, "email": "user@test.com", ... },
 *     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 * }
 *     ↓
 * RestApiClient.setAuthToken(response.token)
 * SessionManager.setSession(response.user, response.token)
 *     ↓
 * authToken is now available for ALL future API calls
 *     ↓
 * Any call to post() or get():
 * if (authToken != null) {
 *     conn.setRequestProperty("Authorization", "Bearer " + authToken);
 * }
 *     ↓
 * Request sent with JWT header
 *     ↓
 * Backend validates JWT, processes request
 *     ↓
 * Returns response with data
 * 
 * 
 * ERROR SCENARIOS: JWT Handling
 * =============================
 * 
 * Scenario 1: Token Expired (Server returns 401)
 * ------
 * Request: GET /posts with Authorization: Bearer oldtoken
 * Response: 401 Unauthorized
 * 
 * RestApiClient catches 401
 *     ↓
 * Throws: ApiException(401, "Unauthorized")
 *     ↓
 * Controller catches ApiException
 *     ↓
 * handleApiException(e) → ErrorHandler.handleApiException(e)
 *     ↓
 * ErrorHandler detects 401 → ErrorType.SESSION_EXPIRED
 *     ↓
 * AlertUtils.showSessionExpired()
 * SessionManager.clearSession()  ← Token cleared!
 * Navigation to login.fxml
 * 
 * 
 * Scenario 2: Access Denied (Server returns 403)
 * ------
 * Request: POST /admin/users with user token (not admin)
 * Response: 403 Forbidden
 * 
 * RestApiClient catches 403
 *     ↓
 * Throws: ApiException(403, "Access Denied")
 *     ↓
 * ErrorHandler detects 403 → ErrorType.ACCESS_DENIED
 *     ↓
 * AlertUtils.showAccessDenied()
 * (Token is NOT cleared - still valid for other operations)
 * 
 * 
 * Scenario 3: Network Error (No connection)
 * ------
 * Request: GET /posts (network down)
 * Response: java.io.IOException
 * 
 * RestApiClient catches IOException
 *     ↓
 * Throws: ApiException(HttpURLConnection.HTTP_CLIENT_ERROR, "Connection failed")
 *     ↓
 * ErrorHandler detects network error
 *     ↓
 * AlertUtils.showNetworkError()
 * (Token is NOT cleared - user can retry when network recovers)
 * 
 * 
 * BEST PRACTICES FOR JWT HANDLING
 * ================================
 * 
 * ✅ DO:
 * 
 * 1. Always use post()/get() generic methods:
 *    ✓ Good:  post("endpoint", body, ResponseType.class)
 *    ✗ Bad:   new URL(...).openConnection()
 * 
 * 2. Call setAuthToken() after login:
 *    ✓ restApiClient.setAuthToken(response.getToken())
 * 
 * 3. Handle 401 errors specially:
 *    ✓ ErrorHandler automatically redirects to login
 * 
 * 4. Call handleApiException() in controllers:
 *    ✓ catch (ApiException e) { handleApiException(e); }
 * 
 * 5. Use SessionManager for role checks:
 *    ✓ if (SessionManager.getInstance().isAdmin()) { ... }
 * 
 * 6. Always clear session on logout:
 *    ✓ SessionManager.getInstance().clearSession()
 * 
 * 7. Trust the ErrorHandler:
 *    ✓ It detects error types and shows appropriate alerts
 * 
 * 
 * ✘ DON'T:
 * 
 * 1. Never store token in local fields:
 *    ✗ private String myToken;  // NO! Store in RestApiClient
 * 
 * 2. Never manually construct Authorization header:
 *    ✗ conn.setRequestProperty("Authorization", "Bearer " + token);
 *       // NO! Let post()/get() do it
 * 
 * 3. Never hardcode base URL in controllers:
 *    ✗ String url = "http://localhost/api/posts";
 *       // NO! Use RestApiClient methods
 * 
 * 4. Never try to parse JWT manually:
 *    ✗ String[] parts = token.split("\\.");
 *       // NO! Trust backend, use roles from UserDTO
 * 
 * 5. Never skip error handling:
 *    ✗ apiService.createPost(content);  // NO! Ignores errors
 *       // YES: try { ... } catch (ApiException e) { ... }
 * 
 * 6. Never assume token is always present:
 *    ✗ conn.setRequestProperty("Authorization", "Bearer " + authToken);
 *       // NO! Post/get check "if (authToken != null)"
 * 
 * 7. Never expose token in logs or errors:
 *    ✗ throw new RuntimeException("Token: " + authToken);
 *       // NO! Log messages should NOT include token
 * 
 * 
 * TESTING JWT AUTHENTICATION
 * ===========================
 * 
 * Test Case 1: Login and API Call
 * ------
 * 1. Start application
 * 2. Login with valid credentials
 * 3. Verify token stored:
 *    - Check SessionManager.getInstance().getAuthToken() is not null
 * 4. Create a post
 * 5. Verify JWT was sent:
 *    - Check server logs for Authorization header
 * 
 * 
 * Test Case 2: Session Expiration
 * ------
 * 1. Login successfully
 * 2. Manually expire token on server (delete from DB, etc.)
 * 3. Try to create a post
 * 4. Verify behavior:
 *    - Should show "Session expired" alert
 *    - Should clear SessionManager
 *    - Should redirect to login
 * 
 * 
 * Test Case 3: Access Denied
 * ------
 * 1. Login as regular user
 * 2. Try to access admin panel (simulate via direct API call)
 * 3. Verify behavior:
 *    - Should show "Access denied" alert
 *    - Should NOT clear session (token still valid)
 *    - Should stay on current screen
 * 
 * 
 * Test Case 4: Network Error
 * ------
 * 1. Disconnect network or shutdown server
 * 2. Try to make any API call
 * 3. Verify behavior:
 *    - Should show "Network error" alert
 *    - Should NOT clear session (user can retry)
 * 
 * 
 * DEBUGGING TIPS
 * ==============
 * 
 * To debug JWT issues:
 * 
 * 1. Check if token is stored:
 *    Add to RestApiClient:
 *    System.out.println("Token: " + authToken);
 * 
 * 2. Check if token is attached:
 *    Add to post()/get() before API call:
 *    System.out.println("Auth Header: " + conn.getRequestProperty("Authorization"));
 * 
 * 3. Check token format:
 *    Should look like: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 *    Three parts separated by dots: header.payload.signature
 * 
 * 4. Check server response:
 *    Add to RestApiClient response handling:
 *    System.out.println("Response Code: " + responseCode);
 *    System.out.println("Response: " + responseBody);
 * 
 * 5. Enable verbose logging:
 *    Set Java system property:
 *    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
 *    System.setProperty("jdk.httpclient.allowRestrictedHeaders", "true");
 * 
 * 
 * FREQUENTLY ASKED QUESTIONS
 * ==========================
 * 
 * Q: How long does the token last?
 * A: That's determined by the backend. The frontend doesn't know the expiration.
 *    If backend says 401, we redirect to login. Simple!
 * 
 * Q: Where is the token stored?
 * A: In RestApiClient.authToken (static field) and SessionManager.authToken.
 *    Both are cleared on logout.
 * 
 * Q: Can I refresh the token without re-logging in?
 * A: Yes, if the backend provides a /auth/refresh endpoint, add it to
 *    RestApiClient and call it when needed. The token would be updated
 *    via setAuthToken() as usual.
 * 
 * Q: What if I need to add a public (non-authenticated) endpoint?
 * A: The generic post()/get() methods already handle this!
 *    They only add the Authorization header if authToken != null.
 *    So unauthenticated endpoints just work normally.
 * 
 * Q: Can I test the frontend without a backend?
 * A: Not easily - the frontend uses real HTTP calls. You'd need either:
 *    1. Mock RestApiClient (complex)
 *    2. Run a test backend
 *    3. Use a REST API testing tool to mock responses
 * 
 * Q: How do I handle token in headers for other HTTP methods (PUT, DELETE)?
 * A: Same way! Update the put() and delete() methods (if they exist) to check:
 *    if (authToken != null) {
 *        conn.setRequestProperty("Authorization", "Bearer " + authToken);
 *    }
 * 
 * 
 * SUMMARY
 * =======
 * 
 * JWT is automatically managed by RestApiClient:
 * ✅ Stored after login
 * ✅ Attached to all POST/GET requests
 * ✅ Cleared on logout
 * ✅ Cleared on 401 error
 * 
 * When adding new endpoints:
 * ✅ Use post()/get() methods
 * ✅ Never create URLConnection manually
 * ✅ Always handle ApiException
 * ✅ Call handleApiException() in controllers
 * 
 * The architecture ensures consistency and security across all API calls.
 */
