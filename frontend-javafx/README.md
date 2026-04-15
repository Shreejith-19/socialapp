# Social App - JavaFX Frontend

A modern JavaFX desktop client for the Social Media Content Moderation System.

## Features

- User authentication (login)
- View posts feed
- Create new posts
- Like/dislike posts
- Paginated post viewing
- User profile management (extensible)

## Architecture

### Model
- DTOs matching backend API responses
- UserDTO, PostDTO, CommentDTO, LikeDTO, AuthResponse, ErrorResponse

### View
- FXML-based UI definitions
- `login.fxml` - Login screen
- `dashboard.fxml` - Main dashboard with posts feed

### Controller
- `LoginController` - Handles authentication
- `DashboardController` - Manages posts feed and user interactions
- `BaseController` - Common functionality and navigation

### Service
- `ApiService` - High-level API operations
- `RestApiClient` - Low-level HTTP communication using HttpURLConnection

### Util
- `SessionManager` - Manages user session state (singleton pattern)

## Project Structure

```
frontend-javafx/
├── src/
│   ├── main/
│   │   ├── java/com/example/socialapp/frontend/
│   │   │   ├── model/           # DTOs
│   │   │   ├── view/            # FXML files
│   │   │   ├── controller/       # UI controllers
│   │   │   ├── service/          # API communication
│   │   │   ├── util/             # Utilities
│   │   │   └── SocialAppApplication.java
│   │   └── resources/
│   │       ├── com/example/socialapp/frontend/view/  # FXML files
│   │       └── css/              # Stylesheets
│   └── test/
└── pom.xml
```

## Build and Run

### Prerequisites
- Java 21+
- Maven 3.8+

### Build
```bash
cd frontend-javafx
mvn clean install
```

### Run with Maven
```bash
mvn javafx:run
```

### Run as JAR
```bash
mvn clean package
java -jar target/socialapp-frontend-javafx-1.0.0-shaded.jar
```

## Configuration

### Backend Server
Update `RestApiClient.java` to configure the backend server URL:
```java
private static final String BASE_URL = "http://localhost:8080/api/v1";
```

## API Integration

The frontend communicates with the backend API using:
- **Protocol**: REST over HTTP
- **Data Format**: JSON
- **Authentication**: Bearer Token (JWT)
- **HTTP Client**: HttpURLConnection

### Supported Endpoints

#### Authentication
- `POST /auth/login` - User login

#### Posts
- `GET /posts` - Get all posts (paginated)
- `GET /posts/{id}` - Get post by ID
- `POST /posts` - Create new post
- `POST /posts/{id}/like` - Like a post
- `POST /posts/{id}/dislike` - Dislike a post
- `GET /posts/{id}/like-count` - Get like count

#### Users
- `GET /users/me` - Get current user
- `GET /users/{id}` - Get user by ID

## Future Enhancements

- [ ] User registration UI
- [ ] Comment creation and viewing
- [ ] User profile management
- [ ] Search functionality
- [ ] Moderator dashboard (for MODERATOR role)
- [ ] Report content feature
- [ ] Follow/unfollow users
- [ ] Real-time notifications
- [ ] Image upload support
- [ ] Caching layer for offline mode

## Dependencies

- **JavaFX 21.0.2** - UI framework
- **Gson 2.10.1** - JSON serialization
- **SLF4J 2.0.9** - Logging API
- **Logback 1.4.11** - Logging implementation
- **Lombok 1.18.30** - Boilerplate reduction

## Notes

- No backend logic is included in this module
- All API communication goes through the ApiService
- Session management handled via SessionManager singleton
- MVC pattern with clear separation of concerns
- FXML for declarative UI definitions

## License

Apache License 2.0
