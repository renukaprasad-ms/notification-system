# Implemented Modules

## Common Module

Package:

```text
com.renuka.notification_backend.common
```

### API Response

File:

```text
common/response/ApiResponse.java
```

All API responses use one common format.

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

`ApiResponse` uses static factory methods and a private constructor to prevent inconsistent response objects.

### Exception Handling

Files:

```text
common/exception/ApiException.java
common/exception/BadRequestException.java
common/exception/ConflictException.java
common/exception/ForbiddenException.java
common/exception/NotFoundException.java
common/exception/UnauthorizedException.java
common/exception/GlobalExceptionHandler.java
```

`GlobalExceptionHandler` converts application exceptions, validation failures, security failures, and unexpected exceptions into the common error response format.

## User Module

Package:

```text
com.renuka.notification_backend.user
```

### Entities

Files:

```text
user/entity/User.java
user/entity/Role.java
user/entity/RoleName.java
user/entity/UserRole.java
user/entity/UserRoleId.java
```

Tables:

- `users`
- `roles`
- `user_roles`

Admin users are represented through roles, not a separate admin table.

Current roles:

- `USER`
- `ADMIN`

The user table supports email/password authentication only. There is no phone-number field.

### Repositories

Files:

```text
user/repository/UserRepository.java
user/repository/RoleRepository.java
user/repository/UserRoleRepository.java
```

These repositories support user lookup, role lookup, duplicate email checks, and role lookup by user id.

### Profile API

Files:

```text
user/controller/UserController.java
user/service/UserService.java
user/dto/UserProfileResponse.java
```

`GET /api/users/me` returns the authenticated user's profile using the JWT access token cookie.

## Auth Module

Package:

```text
com.renuka.notification_backend.auth
```

### DTOs

Files:

```text
auth/dto/CreateUserRequest.java
auth/dto/LoginRequest.java
auth/dto/LoginResponse.java
auth/dto/LoginType.java
```

`LoginType` supports:

- `EMAIL_PASSWORD`
- `EMAIL_OTP`

### Services

Files:

```text
auth/service/AuthService.java
auth/service/PasswordHashService.java
```

`AuthService` currently supports:

- Creating a user.
- Creating a login OTP.
- Logging in with email/password.
- Logging in with email/OTP.
- Refreshing auth cookies from a valid refresh-token cookie.
- Creating password-reset OTPs.
- Resetting passwords through OTP verification.

`PasswordHashService` wraps `PasswordEncoder` so password hashing/checking stays centralized.

### OTP

Files:

```text
auth/otp/OtpVerification.java
auth/otp/OtpVerificationRepository.java
auth/otp/OtpCodeGenerator.java
auth/otp/OtpService.java
auth/otp/OtpChannel.java
auth/otp/OtpPurpose.java
```

OTP is stored as a hash, not plain text.

`OtpService` supports:

- Creating an OTP.
- Verifying an OTP.
- Expiry checks.
- Max-attempt checks.
- Consuming OTP after successful verification.

Current OTP channel:

- `EMAIL`

Current OTP purposes:

- `SIGNUP`
- `LOGIN`
- `PASSWORD_RESET`

There is no mail sender integration yet. The current login OTP API returns the OTP in the response for development/testing.

## Security Module

Package:

```text
com.renuka.notification_backend.security
```

### Password Encoder

File:

```text
security/config/SecurityBeanConfig.java
```

Defines BCrypt `PasswordEncoder`.

### Security Chain

File:

```text
security/config/SecurityConfig.java
```

Current behavior:

- Stateless sessions.
- Public auth endpoints are explicitly allow-listed.
- All other endpoints require authentication.
- JWT filter is registered before username/password authentication filter.
- Method security is enabled with `@EnableMethodSecurity`.
- Role-based checks can use `@PreAuthorize("hasRole('ADMIN')")`.

### JWT

Files:

```text
security/jwt/JwtService.java
security/jwt/JwtCookieService.java
security/jwt/JwtClaims.java
security/jwt/JwtTokenType.java
security/filter/JwtAuthenticationFilter.java
```

JWT design:

- Access token and refresh token are created.
- Tokens are stored in HttpOnly cookies.
- Access token cookie path is `/`.
- Refresh token cookie path is `/api/auth/refresh`.
- JWT validation checks signature, token type, and expiration.
- Auth filter reads the access token from cookies and sets Spring Security authentication.
- Logout clears auth cookies.

## Config Module

Package:

```text
com.renuka.notification_backend.config
```

### Database Seeder

File:

```text
config/DatabaseSeeder.java
```

On startup, if seed is enabled, it creates:

- `USER` role.
- `ADMIN` role.
- Super admin user.
- User-role mappings for the super admin.

The seed is idempotent, so restarts do not create duplicates.

## Notification Module

Package:

```text
com.renuka.notification_backend.notification
```

Notification database entities, repositories, and admin send APIs are implemented. Realtime delivery and user notification APIs are not implemented yet.

Implemented entities:

- `Notification`
- `NotificationRecipient`
- `NotificationDeliveryAttempt`

Implemented enums:

- `NotificationType`
- `NotificationPriority`
- `DeliveryStatus`
- `NotificationChannel`
- `DeliveryAttemptStatus`

Implemented repositories:

- `NotificationRepository`
- `NotificationRecipientRepository`
- `NotificationDeliveryAttemptRepository`

Implemented APIs:

- `POST /api/notifications/send-all`
- `POST /api/notifications/send-selected`
- `GET /api/notifications/stream`
- `GET /api/notifications/me`
- `PATCH /api/notifications/{recipientId}/viewed`
- `PATCH /api/notifications/{recipientId}/read`
- `GET /api/notifications/me/unread-count`

Implemented realtime service:

- `NotificationStreamService`
- `NotificationDeliveryTrackingService`

The stream service currently keeps SSE connections in memory. This is enough for a single backend instance. For horizontal scaling, Redis Pub/Sub or Redis Streams should be added later.

Delivery status behavior:

- If at least one active SSE connection receives the event, recipient status becomes `DELIVERED`.
- If no SSE connection exists or delivery fails, recipient status becomes `FAILED`.
- Every in-app delivery result writes a `notification_delivery_attempts` row.

Pending areas:

- Redis unread-count cache.
- Rate limiting.
- Retry mechanism for failed delivery attempts.
