# Realtime Notification System Context

## Project Goal

Build a scalable, production-grade realtime notification system using Spring Boot.

This project should be treated like a real backend system, not a college/demo project. Code quality, architecture, reasoning, and explainability matter more than feature count.

## Engineering Principles

- Follow SOLID principles.
- Avoid unnecessary code, abstractions, files, and features.
- Keep changes aligned with the system design.
- Prefer clear, maintainable, testable code over clever code.
- Every submitted line of code should be explainable.
- Do not copy-paste code from public repositories.
- Keep API responses consistent across the application.
- Build with production concerns in mind from the beginning.

## Backend Requirements

Create REST APIs to:

- Create notifications.
- Get user notifications.
- Mark notifications as read.

Implement realtime notification delivery using one of:

- WebSocket
- Server-Sent Events

The choice should be intentional and explained with tradeoffs.

Notifications must be stored in a SQL database.

Redis should be used for:

- Caching unread notification counts.
- Rate limiting.

Failed notification delivery should have a retry mechanism.

## Frontend Requirements

Create a minimal dashboard that:

- Shows notifications live.
- Updates unread count instantly.

## Infrastructure Requirements

- Dockerize the application.
- Add Kubernetes deployment YAML.
- Add environment-based configuration.

## Production Constraints

- The same user may connect from multiple devices.
- Duplicate notifications should not occur.
- Redis restart/failure should not crash the system.
- The system should support horizontal scaling.

## Submission Requirements

- Git repository.
- README with architecture explanation.
- API documentation.
- AI usage declaration.

## Evaluation Criteria

- Architecture quality.
- Error handling.
- Code consistency.
- Scalability thinking.
- Ability to explain tradeoffs.

## Current Design Decisions

### API Response Format

All API responses should use the same structure.

Success:

```json
{
  "status": true,
  "status_code": 200,
  "data": {},
  "message": "success message"
}
```

Error:

```json
{
  "status": false,
  "status_code": 400,
  "error_message": "error msg"
}
```

Use static factory methods for API responses instead of public constructors. This keeps controller code clear and prevents inconsistent response objects.

### Exception Handling

Use a global exception handler for consistent error responses.

Application code should throw specific custom exceptions such as:

- `BadRequestException`
- `NotFoundException`
- `UnauthorizedException`
- `ForbiddenException`
- `ConflictException`

Example:

```java
throw new BadRequestException("Invalid request data");
```

The global exception handler should convert it into the common API error response format.

## Collaboration Notes

- Do not change code unless explicitly asked.
- Before implementation, understand the existing codebase and project direction.
- Avoid writing extra code just because it might be useful later.
- Prefer small, focused, production-minded changes.
