# Design Principles in Social App Backend

This document explains the design principles (SOLID and GRASP) used throughout the Social App backend architecture.

## Quick Navigation

- **[SOLID Principles](./SOLID_PRINCIPLES.md)** - Five core design principles for maintainable code
- **[GRASP Principles](./GRASP_PRINCIPLES.md)** - General Responsibility Assignment Software Patterns

## Architecture Overview

```
Controller Layer (HTTP)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Database
```

### Layer Responsibilities

| Layer | Type | Examples |
|---|---|---|
| **Controller** | Request/Response handling | PostController, AuthController, AppealController |
| **Service** | Business logic & rules | PostService, UserService, ModerationService |
| **Repository** | Data persistence | UserRepository, PostRepository, AppealRepository |
| **Entity** | Domain objects | User, Post, Appeal, Ban |
| **DTO** | Data transfer | PostDTO, UserDTO, AppealDTO |
| **Config** | System setup | SecurityConfig, GlobalExceptionHandler |
| **Exception** | Error handling | BannedUserException, ResourceNotFoundException |
| **Util** | Helper methods | SecurityUtil, mappers |

## Key Design Decisions

### 1. Security Architecture
- JWT token-based authentication
- Role-based access control (USER, MODERATOR, ADMIN)
- Method-level security with `@PreAuthorize`
- Centralized security configuration

### 2. Error Handling
- Global exception handler for all endpoints
- Custom exception hierarchy
- Standardized error response format
- Specific error messages for different scenarios

### 3. Data Flow
- DTOs decouple API from internal entities
- Service interfaces separate business logic from implementation
- Repository pattern abstracts database details
- Dependency injection reduces coupling

### 4. Content Moderation
- Automatic content review on post creation
- Moderation queue for flagged content
- Appeal workflow for users
- Admin decision-making process

### 5. Ban Management
- Temporary vs. Permanent bans
- Ban appeals with moderator review
- Automatic status updates
- Clear user messaging

## Design Principles Summary

### SOLID Principles (5 core principles)
1. **S**ingle Responsibility - Each class has one reason to change
2. **O**pen/Closed - Open for extension, closed for modification
3. **L**iskov Substitution - Subtypes are substitutable for base types
4. **I**nterface Segregation - Don't depend on interfaces you don't use
5. **D**ependency Inversion - Depend on abstractions, not concrete classes

### GRASP Principles (9 patterns)
1. **Information Expert** - Assign responsibility to the class with the information
2. **Creator** - Assign object creation to the class that knows how to create it
3. **Controller** - Mediate between system and external actors
4. **Low Coupling** - Minimize dependencies between classes
5. **High Cohesion** - Group related responsibilities together
6. **Polymorphism** - Use polymorphism instead of type checks
7. **Pure Fabrication** - Create classes that support better design
8. **Indirection** - Use intermediaries to reduce coupling
9. **Protected Variations** - Shield against external variations

## File Structure

```
socialapp/
├── src/main/java/com/example/socialapp/
│   ├── controller/          # HTTP request handlers
│   ├── service/             # Business logic interfaces & implementations
│   ├── repository/          # Database access (JPA)
│   ├── entity/              # JPA entities
│   ├── dto/                 # Data Transfer Objects
│   ├── exception/           # Custom exceptions
│   ├── config/              # Configuration classes
│   ├── security/            # Security & authentication
│   ├── enums/               # Enum types
│   └── util/                # Utility classes
├── SOLID_PRINCIPLES.md      # Detailed SOLID explanation
├── GRASP_PRINCIPLES.md      # Detailed GRASP explanation
└── DESIGN_PRINCIPLES.md     # This file
```

## See Also

- [SOLID Principles Details](./SOLID_PRINCIPLES.md)
- [GRASP Principles Details](./GRASP_PRINCIPLES.md)

---

**Last Updated:** April 15, 2026
