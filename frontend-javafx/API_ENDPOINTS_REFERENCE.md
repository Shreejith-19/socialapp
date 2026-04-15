/**
 * API ENDPOINTS REFERENCE GUIDE
 * =============================
 * 
 * This document lists all API endpoints used by the JavaFX frontend.
 * Each endpoint shows the request/response format and JWT requirements.
 * 
 * BASE_URL: http://localhost:8080/api/v1 (update for production)
 * 
 * 
 * AUTHENTICATION ENDPOINTS (No JWT Required)
 * ==========================================
 * 
 * 1. Login
 * --------
 * Endpoint: POST /auth/login
 * JWT: NOT REQUIRED (public endpoint)
 * 
 * Request:
 *   {
 *     "email": "user@example.com",
 *     "password": "password123"
 *   }
 * 
 * Response (200 OK):
 *   {
 *     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *     "user": {
 *       "id": 1,
 *       "email": "user@example.com",
 *       "name": "John Doe",
 *       "roles": ["USER"],
 *       "enabled": true
 *     }
 *   }
 * 
 * Response (401 Unauthorized):
 *   {
 *     "error": "Invalid email or password"
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.authenticate(email, password)
 *   → Returns AuthResponse with token and user
 * 
 * 
 * 2. Sign Up
 * ----------
 * Endpoint: POST /auth/signup
 * JWT: NOT REQUIRED (public endpoint)
 * 
 * Request:
 *   {
 *     "email": "newuser@example.com",
 *     "name": "Jane Doe",
 *     "password": "password123"
 *   }
 * 
 * Response (201 Created):
 *   {
 *     "id": 2,
 *     "email": "newuser@example.com",
 *     "name": "Jane Doe",
 *     "roles": ["USER"],
 *     "enabled": true
 *   }
 * 
 * Response (400 Bad Request):
 *   {
 *     "error": "Email already registered"
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.signup(email, name, password)
 *   → Returns UserDTO
 * 
 * 
 * 
 * USER ENDPOINTS (JWT Required)
 * =============================
 * 
 * 3. Get Current User
 * -------------------
 * Endpoint: GET /users/me
 * JWT: REQUIRED
 * 
 * Request Headers:
 *   Authorization: Bearer {token}
 * 
 * Response (200 OK):
 *   {
 *     "id": 1,
 *     "email": "user@example.com",
 *     "name": "John Doe",
 *     "roles": ["USER", "ADMIN"],
 *     "enabled": true
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.getCurrentUser()
 *   → Returns UserDTO
 * 
 * 
 * 4. Get User By ID
 * -----------------
 * Endpoint: GET /users/{id}
 * JWT: REQUIRED
 * 
 * Path Parameters:
 *   id - User ID (e.g., 123)
 * 
 * Response (200 OK):
 *   {
 *     "id": 123,
 *     "email": "user@example.com",
 *     "name": "John Doe",
 *     "roles": ["USER"],
 *     "enabled": true
 *   }
 * 
 * Response (404 Not Found):
 *   {
 *     "error": "User not found"
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.getUserById(id)
 *   → Returns UserDTO
 * 
 * 
 * 
 * POST ENDPOINTS (JWT Required)
 * =============================
 * 
 * 5. Get All Posts
 * ----------------
 * Endpoint: GET /posts?page=0&size=10
 * JWT: REQUIRED
 * 
 * Query Parameters:
 *   page - Page number (0-indexed)
 *   size - Posts per page (default 10)
 * 
 * Response (200 OK):
 *   {
 *     "content": [
 *       {
 *         "id": 1,
 *         "content": "My first post",
 *         "author": {
 *           "id": 1,
 *           "name": "John Doe",
 *           "email": "john@example.com"
 *         },
 *         "createdAt": "2024-01-15T10:30:00",
 *         "likesCount": 5
 *       }
 *     ],
 *     "totalElements": 42,
 *     "totalPages": 5
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.getAllPosts(page, size)
 *   → Returns list of PostDTO
 * 
 * 
 * 6. Get Post By ID
 * -----------------
 * Endpoint: GET /posts/{id}
 * JWT: REQUIRED
 * 
 * Path Parameters:
 *   id - Post ID
 * 
 * Response (200 OK):
 *   {
 *     "id": 1,
 *     "content": "My first post",
 *     "author": {
 *       "id": 1,
 *       "name": "John Doe",
 *       "email": "john@example.com"
 *     },
 *     "createdAt": "2024-01-15T10:30:00",
 *     "likesCount": 5
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.getPostById(id)
 *   → Returns PostDTO
 * 
 * 
 * 7. Get Posts by Author
 * ----------------------
 * Endpoint: GET /posts/author/{authorId}?page=0&size=10
 * JWT: REQUIRED
 * 
 * Path Parameters:
 *   authorId - Author user ID
 * 
 * Response (200 OK):
 *   {
 *     "content": [...],
 *     "totalElements": 10,
 *     "totalPages": 1
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.getPostsByAuthor(authorId, page, size)
 *   → Returns list of PostDTO
 * 
 * 
 * 8. Create Post
 * --------------
 * Endpoint: POST /posts
 * JWT: REQUIRED
 * 
 * Request Body:
 *   {
 *     "content": "This is my new post"
 *   }
 * 
 * Response (201 Created):
 *   {
 *     "id": 42,
 *     "content": "This is my new post",
 *     "author": {
 *       "id": 1,
 *       "name": "John Doe"
 *     },
 *     "createdAt": "2024-01-15T11:00:00",
 *     "likesCount": 0
 *   }
 * 
 * Response (400 Bad Request):
 *   {
 *     "error": "Content cannot be empty"
 *   }
 * 
 * Response (403 Forbidden):
 *   {
 *     "error": "You are banned and cannot create posts"
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.createPost(content)
 *   → Returns PostDTO
 *   → May throw ApiException(403) if user is banned
 * 
 * 
 * 
 * LIKE/DISLIKE ENDPOINTS (JWT Required)
 * ======================================
 * 
 * 9. Like Post
 * ------------
 * Endpoint: POST /posts/{id}/like
 * JWT: REQUIRED
 * 
 * Response (200 OK):
 *   {
 *     "id": 1,
 *     "liked": true
 *   }
 * 
 * Response (400 Bad Request):
 *   {
 *     "error": "Already liked this post"
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.likePost(postId)
 *   → Returns LikeDTO
 * 
 * 
 * 10. Dislike Post
 * ----------------
 * Endpoint: POST /posts/{id}/dislike
 * JWT: REQUIRED
 * 
 * Response (200 OK):
 *   {
 *     "id": 1,
 *     "liked": false
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.dislikePost(postId)
 *   → Returns LikeDTO
 * 
 * 
 * 11. Get User Likes
 * ------------------
 * Endpoint: GET /users/{id}/likes
 * JWT: REQUIRED
 * 
 * Response (200 OK):
 *   {
 *     "likes": [1, 5, 7, 12, 33]
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.getUserLikes(userId)
 *   → Returns set of liked post IDs
 * 
 * 
 * 
 * MODERATION ENDPOINTS (JWT Required - MODERATOR+ role)
 * ========================================================
 * 
 * 12. Get Moderation Queue
 * -------------------------
 * Endpoint: GET /moderation/queue?page=0&size=10
 * JWT: REQUIRED
 * Role: MODERATOR or ADMIN
 * 
 * Response (200 OK):
 *   {
 *     "content": [
 *       {
 *         "id": 1,
 *         "content": "Inappropriate content",
 *         "author": {...},
 *         "flags": 3,
 *         "createdAt": "2024-01-15T10:00:00"
 *       }
 *     ],
 *     "totalElements": 5,
 *     "totalPages": 1
 *   }
 * 
 * Response (403 Forbidden):
 *   {
 *     "error": "Only moderators can view moderation queue"
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.getModerationQueue(page, size)
 *   → Returns list of flagged PostDTOs
 *   → May throw ApiException(403) if not moderator
 * 
 * 
 * 13. Submit Moderation Decision
 * --------------------------------
 * Endpoint: POST /moderation/decisions
 * JWT: REQUIRED
 * Role: MODERATOR or ADMIN
 * 
 * Request Body:
 *   {
 *     "postId": 1,
 *     "decision": "REMOVE",
 *     "reason": "Violates community guidelines"
 *   }
 * 
 * Decision values: APPROVE, REMOVE, ESCALATE
 * 
 * Response (200 OK):
 *   {
 *     "postId": 1,
 *     "decision": "REMOVE",
 *     "reason": "Violates community guidelines",
 *     "reviewedBy": 2,
 *     "reviewedAt": "2024-01-15T11:00:00"
 *   }
 * 
 * Response (403 Forbidden):
 *   {
 *     "error": "Only moderators can submit decisions"
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.submitDecision(postId, decision, reason)
 *   → Returns decision confirmation
 *   → May throw ApiException(403) if not moderator
 * 
 * 
 * 
 * ADMIN ENDPOINTS (JWT Required - ADMIN role only)
 * ==================================================
 * 
 * 14. Get All Users
 * ------------------
 * Endpoint: GET /admin/users?page=0&size=10
 * JWT: REQUIRED
 * Role: ADMIN
 * 
 * Response (200 OK):
 *   {
 *     "content": [
 *       {
 *         "id": 1,
 *         "email": "user@example.com",
 *         "name": "John Doe",
 *         "roles": ["USER"],
 *         "enabled": true
 *       }
 *     ],
 *     "totalElements": 50,
 *     "totalPages": 5
 *   }
 * 
 * Response (403 Forbidden):
 *   {
 *     "error": "Only admins can view all users"
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.getUsers(page, size)
 *   → Returns list of UserDTOs
 *   → May throw ApiException(403) if not admin
 * 
 * 
 * 15. Ban User
 * -------------
 * Endpoint: POST /admin/users/{id}/ban
 * JWT: REQUIRED
 * Role: ADMIN
 * 
 * Path Parameters:
 *   id - User ID to ban
 * 
 * Request Body:
 *   {
 *     "reason": "Violated community guidelines"
 *   }
 * 
 * Response (200 OK):
 *   {
 *     "id": 1,
 *     "email": "user@example.com",
 *     "name": "John Doe",
 *     "enabled": false,
 *     "banReason": "Violated community guidelines"
 *   }
 * 
 * Response (403 Forbidden):
 *   {
 *     "error": "Only admins can ban users"
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.banUser(userId, reason)
 *   → Returns updated UserDTO with enabled=false
 *   → May throw ApiException(403) if not admin
 * 
 * 
 * 16. Unban User
 * ---------------
 * Endpoint: POST /admin/users/{id}/unban
 * JWT: REQUIRED
 * Role: ADMIN
 * 
 * Response (200 OK):
 *   {
 *     "id": 1,
 *     "email": "user@example.com",
 *     "name": "John Doe",
 *     "enabled": true
 *   }
 * 
 * Response (403 Forbidden):
 *   {
 *     "error": "Only admins can unban users"
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.unbanUser(userId)
 *   → Returns updated UserDTO with enabled=true
 * 
 * 
 * 
 * APPEAL ENDPOINTS (JWT Required)
 * ===============================
 * 
 * 17. Get Appeals
 * ----------------
 * Endpoint: GET /admin/appeals?page=0&size=10
 * JWT: REQUIRED
 * Role: ADMIN
 * 
 * Response (200 OK):
 *   {
 *     "content": [
 *       {
 *         "id": 1,
 *         "userId": 1,
 *         "reason": "I didn't violate any rules",
 *         "status": "PENDING",
 *         "createdAt": "2024-01-15T10:00:00"
 *       }
 *     ],
 *     "totalElements": 3,
 *     "totalPages": 1
 *   }
 * 
 * Response (403 Forbidden):
 *   {
 *     "error": "Only admins can view appeals"
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.getAppeals(page, size)
 *   → Returns list of AppealDTOs
 *   → May throw ApiException(403) if not admin
 * 
 * 
 * 18. Submit Appeal
 * ------------------
 * Endpoint: POST /appeals
 * JWT: REQUIRED
 * 
 * Request Body:
 *   {
 *     "reason": "I didn't violate any rules. Please review my case."
 *   }
 * 
 * Response (201 Created):
 *   {
 *     "id": 1,
 *     "userId": 1,
 *     "reason": "I didn't violate any rules. Please review my case.",
 *     "status": "PENDING",
 *     "createdAt": "2024-01-15T10:30:00"
 *   }
 * 
 * Response (400 Bad Request):
 *   {
 *     "error": "Appeal reason must be between 10 and 1000 characters"
 *   }
 * 
 * Response (403 Forbidden):
 *   {
 *     "error": "Only banned users can submit appeals"
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.submitAppeal(reason)
 *   → Returns AppealDTO
 *   → May throw ApiException(403) if not banned
 * 
 * 
 * 19. Decide Appeal
 * ------------------
 * Endpoint: POST /admin/appeals/{id}/decide
 * JWT: REQUIRED
 * Role: ADMIN
 * 
 * Path Parameters:
 *   id - Appeal ID
 * 
 * Request Body:
 *   {
 *     "decision": "APPROVED",
 *     "reviewReason": "Case reviewed and overturned on appeal"
 *   }
 * 
 * Decision values: APPROVED, REJECTED
 * 
 * Response (200 OK):
 *   {
 *     "id": 1,
 *     "userId": 1,
 *     "reason": "...",
 *     "status": "APPROVED",
 *     "reviewReason": "Case reviewed and overturned on appeal",
 *     "reviewedAt": "2024-01-15T11:00:00"
 *   }
 * 
 * Response (403 Forbidden):
 *   {
 *     "error": "Only admins can decide appeals"
 *   }
 * 
 * Frontend Usage:
 *   restApiClient.decideAppeal(appealId, decision, reviewReason)
 *   → Returns updated AppealDTO
 *   → APPROVED decision unbans user (sets user.enabled = true)
 *   → REJECTED decision keeps user banned
 * 
 * 
 * 
 * HTTP STATUS CODES
 * =================
 * 
 * 200 OK
 * ------
 * Request succeeded. Response contains the requested data.
 * Example: Successfully created post, user logged in
 * 
 * 201 Created
 * -----------
 * Resource was successfully created.
 * Example: New post created, new user registered
 * 
 * 400 Bad Request
 * ---------------
 * Request is malformed or contains invalid data.
 * Frontend should: Show validation error to user
 * Example: Empty post content, invalid email format
 * 
 * 401 Unauthorized
 * ----------------
 * JWT token is missing, invalid, or expired.
 * Frontend should: Redirect to login, clear session
 * Example: No Authorization header, token expired
 * 
 * 403 Forbidden
 * ---------------
 * Request is valid but user lacks permission.
 * Frontend should: Show "Access Denied" message or role-specific message
 * Example: Non-admin accessing admin endpoint, banned user creating post
 * 
 * 404 Not Found
 * ---------------
 * Resource does not exist.
 * Frontend should: Show "Not found" message
 * Example: User with ID 999 doesn't exist
 * 
 * 500 Internal Server Error
 * --------------------------
 * Server error occurred.
 * Frontend should: Show "Server error" message
 * Example: Database connection failed, unexpected exception
 * 
 * 
 * 
 * ERROR RESPONSE FORMAT
 * =====================
 * 
 * All error responses follow this format:
 * 
 *   {
 *     "error": "Error message describing what went wrong"
 *   }
 * 
 * Or with more details:
 * 
 *   {
 *     "error": "Validation failed",
 *     "details": {
 *       "email": "Email already registered",
 *       "name": "Name is required"
 *     }
 *   }
 * 
 * Frontend ErrorHandler captures these and shows user-friendly messages.
 * 
 * 
 * 
 * PAGINATION FORMAT
 * =================
 * 
 * All endpoints that return multiple items use this format:
 * 
 *   {
 *     "content": [...],           // Array of items
 *     "totalElements": 100,       // Total number of items
 *     "totalPages": 10,           // Total number of pages
 *     "number": 0,                // Current page (0-indexed)
 *     "size": 10,                 // Items per page
 *     "empty": false,             // Is page empty?
 *     "first": true,              // Is this first page?
 *     "last": false,              // Is this last page?
 *     "numberOfElements": 10      // Items in current page
 *   }
 * 
 * Frontend automatically handles pagination by specifying ?page=X&size=Y
 * 
 * 
 * 
 * SUMMARY TABLE
 * =============
 * 
 * | Endpoint                     | Method | JWT? | Role     | Purpose
 * |------------------------------|--------|------|----------|----------------------------------
 * | /auth/login                  | POST   | No   | -        | User login
 * | /auth/signup                 | POST   | No   | -        | Register new user
 * | /users/me                    | GET    | Yes  | USER+    | Get current user info
 * | /users/{id}                  | GET    | Yes  | USER+    | Get user info
 * | /posts                       | GET    | Yes  | USER+    | Get all posts (paginated)
 * | /posts/{id}                  | GET    | Yes  | USER+    | Get single post
 * | /posts/author/{id}           | GET    | Yes  | USER+    | Get posts by author
 * | /posts                       | POST   | Yes  | USER+    | Create new post
 * | /posts/{id}/like             | POST   | Yes  | USER+    | Like a post
 * | /posts/{id}/dislike          | POST   | Yes  | USER+    | Dislike a post
 * | /users/{id}/likes            | GET    | Yes  | USER+    | Get user's likes
 * | /moderation/queue            | GET    | Yes  | MOD+     | Get flagged posts
 * | /moderation/decisions        | POST   | Yes  | MOD+     | Submit moderation decision
 * | /admin/users                 | GET    | Yes  | ADMIN    | Get all users
 * | /admin/users/{id}/ban        | POST   | Yes  | ADMIN    | Ban a user
 * | /admin/users/{id}/unban      | POST   | Yes  | ADMIN    | Unban a user
 * | /admin/appeals               | GET    | Yes  | ADMIN    | Get pending appeals
 * | /appeals                     | POST   | Yes  | BANNED   | Submit appeal
 * | /admin/appeals/{id}/decide   | POST   | Yes  | ADMIN    | Approve/reject appeal
 * 
 * Roles: USER (default), MOD (MODERATOR), ADMIN, BANNED (no enabled status)
 * 
 */
