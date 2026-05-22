# API Reference

Base backend URL in local Docker:

```text
http://localhost:8080
```

All responses use `ApiResponse`.

## Signup

```http
POST /api/auth/signup
```

Creates a user, assigns the `USER` role, sets access/refresh JWT cookies, and returns user data.

Request:

```json
{
  "email": "user@example.com",
  "password": "Password@123",
  "fullName": "Test User"
}
```

Success response:

```json
{
  "status": true,
  "status_code": 201,
  "data": {
    "userId": "uuid",
    "email": "user@example.com",
    "fullName": "Test User",
    "roles": ["USER"]
  },
  "message": "User created successfully"
}
```

Notes:

- Password is stored as a BCrypt hash.
- Email is currently marked verified immediately because signup OTP sending is not implemented yet.
- JWT tokens are set in HttpOnly cookies, not returned in the response body.

## Login With Password

```http
POST /api/auth/login
```

Request:

```json
{
  "email": "user@example.com",
  "loginType": "EMAIL_PASSWORD",
  "password": "Password@123"
}
```

Success response:

```json
{
  "status": true,
  "status_code": 200,
  "data": {
    "userId": "uuid",
    "email": "user@example.com",
    "fullName": "Test User",
    "roles": ["USER"]
  },
  "message": "Login successful"
}
```

Behavior:

- Checks user exists.
- Checks password.
- Checks user is active.
- Requires email to be verified.
- Sets access/refresh JWT cookies.

## Create Login OTP

```http
POST /api/auth/login/otp
```

Creates a login OTP for an existing active user.

Request:

```json
{
  "email": "user@example.com"
}
```

Success response:

```json
{
  "status": true,
  "status_code": 200,
  "data": {
    "otp": "123456"
  },
  "message": "Login OTP created successfully"
}
```

Important:

This currently returns the OTP in the response because mail delivery is not implemented yet. Before production, this endpoint should send the OTP by email and stop returning the raw OTP.

## Login With OTP

```http
POST /api/auth/login
```

Request:

```json
{
  "email": "user@example.com",
  "loginType": "EMAIL_OTP",
  "otp": "123456"
}
```

Behavior:

- Checks user exists.
- Checks user is active.
- Verifies OTP with purpose `LOGIN`.
- Consumes OTP after successful verification.
- Marks email verified if it was not already verified.
- Sets access/refresh JWT cookies.

Frontend flow:

1. Call `POST /api/auth/login/otp`.
2. Ask user to enter the OTP.
3. Call `POST /api/auth/login` with `loginType: "EMAIL_OTP"`.

## Refresh Token

```http
POST /api/auth/refresh
```

Reads the refresh token from the `refresh_token` HttpOnly cookie, validates it, and issues fresh access/refresh cookies.

Request body:

```text
No body required.
```

Frontend call:

```js
await fetch("http://localhost:8080/api/auth/refresh", {
  method: "POST",
  credentials: "include"
});
```

Success response:

```json
{
  "status": true,
  "status_code": 200,
  "data": {
    "userId": "uuid",
    "email": "user@example.com",
    "fullName": "Test User",
    "roles": ["USER"]
  },
  "message": "Token refreshed successfully"
}
```

## Logout

```http
POST /api/auth/logout
```

Clears access and refresh cookies.

Request body:

```text
No body required.
```

Frontend call:

```js
await fetch("http://localhost:8080/api/auth/logout", {
  method: "POST",
  credentials: "include"
});
```

Success response:

```json
{
  "status": true,
  "status_code": 200,
  "message": "Logout successful"
}
```

## Forgot Password OTP

```http
POST /api/auth/forgot-password/otp
```

Creates a password reset OTP for an existing active user.

Request:

```json
{
  "email": "user@example.com"
}
```

Success response:

```json
{
  "status": true,
  "status_code": 200,
  "data": {
    "otp": "123456"
  },
  "message": "Password reset OTP created successfully"
}
```

Important:

This currently returns the OTP in the response because mail delivery is not implemented yet. Before production, this endpoint should send the OTP by email and stop returning the raw OTP.

## Reset Password

```http
POST /api/auth/reset-password
```

Verifies a password-reset OTP, hashes the new password, saves it, and clears auth cookies.

Request:

```json
{
  "email": "user@example.com",
  "otp": "123456",
  "newPassword": "NewPassword@123"
}
```

Success response:

```json
{
  "status": true,
  "status_code": 200,
  "message": "Password reset successful"
}
```

Notes:

- OTP purpose is `PASSWORD_RESET`.
- OTP is consumed after successful reset.
- Existing auth cookies are cleared after password reset.

## Current User Profile

```http
GET /api/auth/me
```

Requires a valid access token cookie.

Frontend call:

```js
await fetch("http://localhost:8080/api/auth/me", {
  method: "GET",
  credentials: "include"
});
```

Success response:

```json
{
  "status": true,
  "status_code": 200,
  "data": {
    "userId": "uuid",
    "email": "user@example.com",
    "fullName": "Test User",
    "roles": ["USER"]
  },
  "message": "Profile fetched successfully"
}
```

## User Profile

```http
GET /api/users/me
```

Requires a valid access token cookie.

This is the user-module profile endpoint and should be preferred for frontend profile screens.

Frontend call:

```js
await fetch("http://localhost:8080/api/users/me", {
  method: "GET",
  credentials: "include"
});
```

Success response:

```json
{
  "status": true,
  "status_code": 200,
  "data": {
    "userId": "uuid",
    "email": "user@example.com",
    "fullName": "Test User",
    "emailVerified": true,
    "roles": ["USER"]
  },
  "message": "Profile fetched successfully"
}
```

## Admin Profile Check

```http
GET /api/auth/admin/me
```

Requires a valid access token cookie and `ADMIN` role.

This endpoint exists to verify role-based access control is wired correctly.

## Frontend Usage

Because auth tokens are HttpOnly cookies, frontend code must include credentials.

Example:

```js
await fetch("http://localhost:8080/api/auth/login", {
  method: "POST",
  credentials: "include",
  headers: {
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    email: "user@example.com",
    loginType: "EMAIL_PASSWORD",
    password: "Password@123"
  })
});
```

Protected API calls must also use:

```js
credentials: "include"
```

## Send Notification To All Users

```http
POST /api/notifications/send-all
```

Requires:

- Valid access token cookie.
- `ADMIN` role.

Request:

```json
{
  "title": "System maintenance",
  "message": "Service will be down at 11 PM.",
  "type": "SYSTEM",
  "priority": "HIGH"
}
```

Success response:

```json
{
  "status": true,
  "status_code": 201,
  "data": {
    "notificationId": "uuid",
    "recipientCount": 10
  },
  "message": "Notification sent successfully"
}
```

## Send Notification To Selected Users

```http
POST /api/notifications/send-selected
```

Requires:

- Valid access token cookie.
- `ADMIN` role.

Request:

```json
{
  "title": "Account alert",
  "message": "Please verify your account details.",
  "type": "ACCOUNT",
  "priority": "NORMAL",
  "recipientUserIds": [
    "user-id-1",
    "user-id-2"
  ]
}
```

Success response:

```json
{
  "status": true,
  "status_code": 201,
  "data": {
    "notificationId": "uuid",
    "recipientCount": 2
  },
  "message": "Notification sent successfully"
}
```

Notes:

- One `notifications` row is created.
- One `notification_recipients` row is created per recipient.
- Selected recipients must be active users.

## Notification Stream

```http
GET /api/notifications/stream
```

Requires a valid access token cookie.

This is an SSE endpoint. It pushes new notification events to the connected user.

Frontend example:

```js
const events = new EventSource("http://localhost:8080/api/notifications/stream", {
  withCredentials: true
});

events.addEventListener("notification", (event) => {
  const notification = JSON.parse(event.data);
  console.log(notification);
});
```

Notification event shape:

```json
{
  "recipientId": "recipient-uuid",
  "notificationId": "notification-uuid",
  "title": "System maintenance",
  "message": "Service will be down at 11 PM.",
  "type": "SYSTEM",
  "priority": "HIGH",
  "createdAt": "2026-05-22T20:00:00"
}
```

Notes:

- Admin still sends notifications through REST.
- Users receive realtime updates through SSE.
- User actions such as viewed/read should be updated through REST APIs.

## Get My Notifications

```http
GET /api/notifications/me
```

Requires a valid access token cookie.

Success response:

```json
{
  "status": true,
  "status_code": 200,
  "data": [
    {
      "recipientId": "recipient-uuid",
      "notificationId": "notification-uuid",
      "title": "System maintenance",
      "message": "Service will be down at 11 PM.",
      "type": "SYSTEM",
      "priority": "HIGH",
      "deliveryStatus": "PENDING",
      "deliveredAt": null,
      "viewedAt": null,
      "readAt": null,
      "createdAt": "2026-05-22T20:00:00"
    }
  ],
  "message": "Notifications fetched successfully"
}
```

## Mark Notification Viewed

```http
PATCH /api/notifications/{recipientId}/viewed
```

Requires a valid access token cookie.

Only the owner of the recipient row can mark it viewed.

## Mark Notification Read

```http
PATCH /api/notifications/{recipientId}/read
```

Requires a valid access token cookie.

Only the owner of the recipient row can mark it read. If `viewedAt` is empty, this endpoint also sets `viewedAt`.

## Get Unread Count

```http
GET /api/notifications/me/unread-count
```

Requires a valid access token cookie.

Success response:

```json
{
  "status": true,
  "status_code": 200,
  "data": {
    "unreadCount": 5
  },
  "message": "Unread count fetched successfully"
}
```
