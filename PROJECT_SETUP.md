# Social Media Content Moderation System

A production-grade Spring Boot application for managing social media content moderation with JWT authentication, role-based access control, and layered architecture.

## Project Features

✅ **Java 21** - Latest LTS Java version
✅ **Spring Boot 3.3.1** - Modern Spring Boot framework
✅ **Spring Security with JWT** - Secure token-based authentication
✅ **H2 Database** - In-memory database for development
✅ **Layered Architecture** - Clean separation of concerns
✅ **Role-Based Access Control** - USER, MODERATOR, ADMIN roles
✅ **Global Exception Handling** - Standardized error responses
✅ **Input Validation** - Request validation with annotations
✅ **CORS Configuration** - Cross-origin resource sharing enabled
✅ **Lombok** - Reduced boilerplate code
✅ **Maven** - Build and dependency management

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/example/socialapp/
│   │       ├── config/              # Configuration classes
│   │       │   ├── SecurityConfig.java
│   │       │   ├── GlobalExceptionHandler.java
│   │       │   └── DataInitializationConfig.java
│   │       ├── constant/            # Application constants
│   │       │   ├── ApiConstant.java
│   │       │   └── RoleConstant.java
│   │       ├── controller/          # REST Controllers
│   │       │   ├── AuthController.java
│   │       │   └── UserController.java
│   │       ├── dto/                 # Data Transfer Objects
│   │       │   ├── AuthRequest.java
│   │       │   ├── AuthResponse.java
│   │       │   ├── UserDTO.java
│   │       │   └── ErrorResponse.java
│   │       ├── entity/              # JPA Entities
│   │       │   ├── User.java
│   │       │   └── Role.java
│   │       ├── exception/           # Custom Exceptions
│   │       │   ├── ResourceNotFoundException.java
│   │       │   ├── ConflictException.java
│   │       │   ├── AuthenticationException.java
│   │       │   └── UnauthorizedAccessException.java
│   │       ├── repository/          # Data Access Layer
│   │       │   ├── UserRepository.java
│   │       │   └── RoleRepository.java
│   │       ├── security/            # Security Components
│   │       │   ├── JwtTokenProvider.java
│   │       │   ├── JwtAuthenticationFilter.java
│   │       │   └── CustomUserDetailsService.java
│   │       ├── service/             # Business Logic
│   │       │   ├── AuthService.java
│   │       │   ├── UserService.java
│   │       │   └── impl/
│   │       │       ├── AuthServiceImpl.java
│   │       │       └── UserServiceImpl.java
│   │       ├── util/                # Utility Classes
│   │       │   └── SecurityUtil.java
│   │       └── SocialappApplication.java
│   └── resources/
│       ├── application.yml          # Application configuration
│       ├── static/                  # Static resources
│       └── templates/               # HTML templates
└── test/
    └── java/                        # Unit and integration tests
```

## Technology Stack

| Component | Version |
|-----------|---------|
| Java | 21 |
| Spring Boot | 3.3.1 |
| Spring Security | 6.1.x |
| Spring Data JPA | 3.1.x |
| JWT (jjwt) | 0.12.3 |
| H2 Database | Latest |
| Lombok | 1.18.x |
| Maven | 3.8.x |

## Getting Started

### Prerequisites

- Java 21 JDK installed
- Maven 3.8.x or later
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourrepo/socialapp.git
   cd socialapp
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   The application will start on `http://localhost:8080/api`

### Application Configuration

The application is configured via `application.yml`:

```yaml
# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /api

# JWT Configuration
jwt:
  secret: your-super-secret-jwt-key-change-this-in-production
  expiration: 86400000 # 24 hours

# Database Configuration
spring:
  datasource:
    url: jdbc:h2:mem:socialappdb
    driver-class-name: org.h2.Driver
```

**Important:** Change the JWT secret in production!

## API Endpoints

### Authentication Endpoints

#### Register User
```
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe"
}

Response: 201 Created
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

#### Login (Authenticate)
```
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

Response: 200 OK
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
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

#### Refresh Token
```
POST /api/v1/auth/refresh-token
Authorization: Bearer <refresh_token>

Response: 200 OK
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "user": { ... }
}
```

#### Validate Token
```
POST /api/v1/auth/validate-token
Authorization: Bearer <token>

Response: 200 OK
true
```

### User Endpoints

#### Get Current User
```
GET /api/v1/users/me
Authorization: Bearer <access_token>

Response: 200 OK
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

#### Get User by ID (Admin Only)
```
GET /api/v1/users/{id}
Authorization: Bearer <admin_token>

Response: 200 OK
```

#### Update Current User
```
PUT /api/v1/users/me
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "firstName": "Jane",
  "lastName": "Smith"
}

Response: 200 OK
```

#### Delete User (Admin Only)
```
DELETE /api/v1/users/{id}
Authorization: Bearer <admin_token>

Response: 204 No Content
```

## User Roles

### USER
- Standard user role
- Can view and manage their own profile
- Default role assigned to new registrations

### MODERATOR
- Elevated permissions for content review
- Can access moderation endpoints
- Can review and approve/reject content

### ADMIN
- Full system administration access
- Can manage users and assign roles
- Can access all system resources

## Security Features

### JWT Authentication
- Token-based authentication without sessions
- Access tokens valid for 24 hours
- Refresh tokens for obtaining new access tokens
- Secure token signature using HS512 algorithm

### Password Security
- Passwords encoded using BCrypt
- Configurable password strength requirements
- Never stored in plain text

### Authorization
- Method-level security with `@PreAuthorize`
- Role-based access control (RBAC)
- Custom security context utilities

### CORS Configuration
- Configurable allowed origins
- Supports multiple frontend domains
- Pre-flight request handling

## Database

The application uses H2 in-memory database for development. The schema is automatically created on startup through Hibernate's `ddl-auto: update` setting.

### H2 Console
Access the H2 database console at: `http://localhost:8080/api/h2-console`

**Default Credentials:**
- JDBC URL: `jdbc:h2:mem:socialappdb`
- Username: `sa`
- Password: (empty)

## Error Handling

All errors return standardized JSON responses with appropriate HTTP status codes:

```json
{
  "status": 400,
  "message": "Validation failed",
  "error": "VALIDATION_ERROR",
  "path": "/api/v1/auth/register",
  "timestamp": "2024-04-09T10:30:00",
  "fieldErrors": {
    "email": "Email should be valid",
    "firstName": "First name is required"
  }
}
```

### Common Error Codes
- `400` - Bad Request / Validation Error
- `401` - Unauthorized (Invalid/Missing credentials)
- `403` - Forbidden (Insufficient permissions)
- `404` - Not Found
- `409` - Conflict (e.g., duplicate email)
- `500` - Internal Server Error

## Development

### Running Tests
```bash
mvn test
```

### Building for Production
```bash
mvn clean package
```

### Code Formatting
The project uses Maven and Lombok to reduce boilerplate code.

## Configuration Properties

Key properties that can be customized in `application.yml`:

```yaml
# JWT Settings
jwt:
  secret: ${JWT_SECRET:your-secret-key}
  expiration: ${JWT_EXPIRATION:86400000}
  refresh-token-expiration: ${JWT_REFRESH_EXPIRATION:604800000}

# Database
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:h2:mem:socialappdb}
    username: ${DATABASE_USER:sa}
    password: ${DATABASE_PASSWORD:}

# Server
server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: ${SERVER_CONTEXT_PATH:/api}
```

## Production Deployment Checklist

- [ ] Change JWT secret to a strong, random value
- [ ] Configure production database (PostgreSQL/MySQL)
- [ ] Set appropriate logging levels
- [ ] Enable HTTPS/TLS
- [ ] Configure CORS for production domains
- [ ] Set up environment-specific `application-prod.yml`
- [ ] Configure health check endpoints
- [ ] Set up monitoring and logging
- [ ] Run security vulnerability scans
- [ ] Configure database backups
- [ ] Set up CI/CD pipeline

## Contributing

1. Create a feature branch (`git checkout -b feature/amazing-feature`)
2. Commit changes (`git commit -m 'Add amazing feature'`)
3. Push to branch (`git push origin feature/amazing-feature`)
4. Open a Pull Request

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Support

For support, email dev@example.com or open an issue in the repository.

## Roadmap

- [ ] Database migration to production-grade systems
- [ ] Message queue integration (RabbitMQ/Kafka)
- [ ] Content analysis and moderation features
- [ ] Admin dashboard and management UI
- [ ] Advanced reporting and analytics
- [ ] Audit logging and compliance tracking
- [ ] API rate limiting and throttling
- [ ] Multi-tenancy support
- [ ] Webhook integrations

---

**Last Updated:** April 9, 2024
