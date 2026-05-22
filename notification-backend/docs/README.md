# Backend Documentation

This folder documents the backend implementation module by module.

## Documents

- [Project Context](./context.md)
- [Implemented Modules](./modules.md)
- [API Reference](./api.md)
- [Database Schema](./database-schema.md)
- [Infrastructure](./infrastructure.md)
- [Pending Work](./pending-work.md)

## Current Backend Status

The backend currently has:

- Common API response wrapper.
- Global exception handling.
- User, role, user-role, and OTP verification entities.
- Super admin seed.
- Password hashing helpers.
- OTP create/verify helpers.
- JWT access/refresh token generation.
- HttpOnly cookie auth.
- Signup API.
- Login API with password and OTP modes.
- Login OTP creation API for development/testing.
- Refresh-token API.
- Logout API.
- Forgot-password OTP API.
- Reset-password API.
- Current-user profile API.
- User-module profile API.
- Role-based access support with method security.

Notification delivery, realtime transport, Redis integration, and notification APIs are not implemented yet.

Notification database entities are now in place.
Admin notification send APIs are now in place.
