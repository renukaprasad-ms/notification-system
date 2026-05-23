# Notification Frontend

This frontend is a React and Vite dashboard for the realtime notification system.

## Features

- Cookie-based authentication flows.
- Login with password or OTP.
- Live notification updates over SSE.
- Unread count updates in the UI.
- Notification list with pagination and search.
- Admin screens for sending notifications and viewing user/admin summaries.

## Runtime Behavior

The frontend does not deliver notifications directly.

Its role is:

1. Call backend APIs to create or fetch data.
2. Show success to the admin when the backend confirms notification persistence.
3. Listen on SSE for live notification events.
4. Refresh local unread count and notification list state as events arrive.

That means admin success in the UI should represent "notification accepted and stored", not "every client already received it live".

## Local Development

The app is usually run through the root infrastructure scripts and Docker Compose files.

Backend default URL:

```text
http://localhost:8080
```

Frontend default URL:

```text
http://localhost:5173
```
