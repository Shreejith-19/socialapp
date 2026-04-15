# API Documentation

## Base URL
```
http://localhost:8080/api
```

## Authentication
All protected endpoints require a JWT token in the `Authorization` header:
```
Authorization: Bearer <access_token>
```

---

## Authentication Endpoints

### 1. Register User

**Endpoint:** `POST /v1/auth/register`

**Description:** Register a new user in the system

**Request Body:**
```json
{
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Validation Rules:**
- `email`: Required, valid email format
- `firstName`: Required, max 50 characters
- `lastName`: Required, max 50 characters

**Response:** `201 Created`
```json
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["USER"],
  "enabled": true,
  "createdAt": "2024-04-09T10:30:00",
  "updatedAt": "2024-04-09T10:30:00"
}
```

**Error Responses:**
- `400 Bad Request` - Validation error
- `409 Conflict` - Email already exists

---

### 2. Login (Authenticate)

**Endpoint:** `POST /v1/auth/login`

**Description:** Authenticate user and receive JWT tokens

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Validation Rules:**
- `email`: Required, valid email format
- `password`: Required, minimum 6 characters

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["USER"],
    "enabled": true,
    "createdAt": "2024-04-09T10:30:00",
    "updatedAt": "2024-04-09T10:30:00"
  }
}
```

**Error Responses:**
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Invalid credentials

---

### 3. Refresh Token

**Endpoint:** `POST /v1/auth/refresh-token`

**Description:** Get a new access token using refresh token

**Headers:**
```
Authorization: Bearer <refresh_token>
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["USER"],
    "enabled": true
  }
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid or expired token

---

### 4. Validate Token

**Endpoint:** `POST /v1/auth/validate-token`

**Description:** Validate the current access token

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:** `200 OK`
```
true
```

**Error Responses:**
- `401 Unauthorized` - Invalid token

---

### 5. Health Check

**Endpoint:** `GET /v1/auth/health`

**Description:** Check if authentication service is running

**Response:** `200 OK`
```
Authentication service is running
```

---

## User Endpoints

### 1. Get Current User

**Endpoint:** `GET /v1/users/me`

**Description:** Get information of the logged-in user

**Headers:**
```
Authorization: Bearer <access_token>
```

**Required Role:** USER, MODERATOR, or ADMIN

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["USER"],
  "enabled": true,
  "createdAt": "2024-04-09T10:30:00",
  "updatedAt": "2024-04-09T10:30:00"
}
```

**Error Responses:**
- `401 Unauthorized` - Missing or invalid token

---

### 2. Get User by ID

**Endpoint:** `GET /v1/users/{id}`

**Description:** Get user information by ID (Admin only or own profile)

**Parameters:**
- `id` (path, required): User ID

**Headers:**
```
Authorization: Bearer <access_token>
```

**Required Role:** ADMIN (or USER for own profile)

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["USER"],
  "enabled": true,
  "createdAt": "2024-04-09T10:30:00",
  "updatedAt": "2024-04-09T10:30:00"
}
```

**Error Responses:**
- `401 Unauthorized` - Missing or invalid token
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - User not found

---

### 3. Update Current User

**Endpoint:** `PUT /v1/users/me`

**Description:** Update current user's profile information

**Headers:**
```
Authorization: Bearer <access_token>
```

**Request Body:**
```json
{
  "firstName": "Jane",
  "lastName": "Smith"
}
```

**Required Role:** USER, MODERATOR, or ADMIN

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "roles": ["USER"],
  "enabled": true,
  "createdAt": "2024-04-09T10:30:00",
  "updatedAt": "2024-04-09T10:45:00"
}
```

**Error Responses:**
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Missing or invalid token

---

### 4. Update User (Admin)

**Endpoint:** `PUT /v1/users/{id}`

**Description:** Update user profile (Admin only)

**Parameters:**
- `id` (path, required): User ID

**Headers:**
```
Authorization: Bearer <admin_token>
```

**Request Body:**
```json
{
  "firstName": "Jane",
  "lastName": "Smith"
}
```

**Required Role:** ADMIN

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "roles": ["USER"],
  "enabled": true,
  "createdAt": "2024-04-09T10:30:00",
  "updatedAt": "2024-04-09T10:45:00"
}
```

**Error Responses:**
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Missing or invalid token
- `403 Forbidden` - Not an admin
- `404 Not Found` - User not found

---

### 5. Delete User (Admin)

**Endpoint:** `DELETE /v1/users/{id}`

**Description:** Delete a user (Admin only)

**Parameters:**
- `id` (path, required): User ID

**Headers:**
```
Authorization: Bearer <admin_token>
```

**Required Role:** ADMIN

**Response:** `204 No Content`

**Error Responses:**
- `401 Unauthorized` - Missing or invalid token
- `403 Forbidden` - Not an admin
- `404 Not Found` - User not found

---

### 6. Check User Exists

**Endpoint:** `GET /v1/users/exists/{email}`

**Description:** Check if a user with given email exists

**Parameters:**
- `email` (path, required): Email address to check

**Response:** `200 OK`
```
true
```
or
```
false
```

---

## Error Response Format

All error responses follow this format:

```json
{
  "status": 400,
  "message": "Descriptive error message",
  "error": "ERROR_CODE",
  "path": "/api/v1/endpoint",
  "timestamp": "2024-04-09T10:30:00",
  "fieldErrors": {
    "fieldName": "Field error message"
  }
}
```

### Common Error Codes

| Code | Status | Description |
|------|--------|-------------|
| VALIDATION_ERROR | 400 | Request validation failed |
| AUTHENTICATION_FAILED | 401 | Invalid credentials |
| UNAUTHORIZED_ACCESS | 403 | Insufficient permissions |
| RESOURCE_NOT_FOUND | 404 | Resource doesn't exist |
| CONFLICT | 409 | Resource already exists (e.g., duplicate email) |
| INTERNAL_SERVER_ERROR | 500 | Server error |

---

## HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Request successful |
| 201 | Created - Resource created |
| 204 | No Content - Request successful, no response body |
| 400 | Bad Request - Invalid request |
| 401 | Unauthorized - Missing/invalid authentication |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource not found |
| 409 | Conflict - Resource conflict (e.g., duplicate) |
| 500 | Internal Server Error |

---

## Rate Limiting

Currently, the API does not have rate limiting. This feature is planned for production deployment.

---

## Pagination

Pagination support is planned for list endpoints in future versions.

---

## CORS Configuration

The API is configured to accept requests from:
- http://localhost:3000
- http://localhost:4200
- http://localhost:8080

Modify `SecurityConfig.java` to add additional origins.

---

## JWT Token Details

### Access Token
- **Expiration:** 24 hours (configurable via `jwt.expiration`)
- **Algorithm:** HS512 (HMAC with SHA-512)
- **Contains:** User email as subject

### Refresh Token
- **Expiration:** 7 days (configurable via `jwt.refresh-token-expiration`)
- **Algorithm:** HS512
- **Purpose:** Obtain new access tokens without re-authenticating

---

## Example cURL Requests

### Register User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

### Get Current User
```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <access_token>"
```

### Update Current User
```bash
curl -X PUT http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Smith"
  }'
```

---

## API Versioning

The API uses URL versioning (e.g., `/v1/`). Future versions will be available as `/v2/`, `/v3/`, etc.

---

**Last Updated:** April 9, 2024
**API Version:** 1.0.0
