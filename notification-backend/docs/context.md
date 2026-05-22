# Project Context

## Goal

Build a scalable, production-grade realtime notification system using Spring Boot.

The project should be treated like a real backend system, not a college/demo project. Code quality, architecture, reasoning, and explainability matter more than feature count.

## Engineering Principles

- Follow SOLID principles.
- Avoid unnecessary code, abstractions, files, and features.
- Keep changes aligned with the system design.
- Prefer clear, maintainable, testable code over clever code.
- Every submitted line of code should be explainable.
- Do not copy-paste code from public repositories.
- Keep API responses consistent across the application.
- Build with production concerns in mind from the beginning.

## Product Requirements

Backend APIs should eventually support:

- Create notification.
- Get user notifications.
- Mark notification as read.

Realtime delivery should use either WebSocket or Server-Sent Events. The choice must be intentional and explained with tradeoffs.

Notifications must be stored in SQL.

Redis should be used for:

- Unread count caching.
- Rate limiting.

Failed delivery should have a retry mechanism.

## Frontend Requirement

Create a minimal dashboard that:

- Shows notifications live.
- Updates unread count instantly.

## Infrastructure Requirement

- Dockerize the application.
- Add Kubernetes deployment YAML later.
- Use environment-based configuration.

## Production Constraints

- Same user may connect from multiple devices.
- Duplicate notifications should not occur.
- Redis restart/failure should not crash the system.
- System should support horizontal scaling.

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
