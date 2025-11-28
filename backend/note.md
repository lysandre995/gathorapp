# DB Access

Go to `http://localhost:8080/h2-console`

JDBC URL: `jdbc:h2:file:./data/gathorapp`

---

# 📘 Backend API Specification

This section describes all RESTful endpoints exposed by the backend application.
Each route includes its HTTP method, purpose, and the required user role.

---

## 1. Authentication & Profile Management (`/api/auth`, `/api/profile`)

| Method | Route                           | Description                                          | User Role     |
| :----- | :------------------------------ | :--------------------------------------------------- | :------------ |
| `POST` | `/api/auth/register`            | Register a new user (Base, Premium, or Business).    | Public        |
| `POST` | `/api/auth/login`               | Log in and receive JWT and Refresh Token.            | Public        |
| `POST` | `/api/auth/refresh`             | Renew the access token using the refresh token.      | Authenticated |
| `GET`  | `/api/profile`                  | Get the authenticated user's full profile details.   | Authenticated |
| `PUT`  | `/api/profile`                  | Update the authenticated user's profile information. | Authenticated |
| `POST` | `/api/profile/upgrade/premium`  | Upgrade from Base to Premium account.                | Base          |
| `POST` | `/api/profile/upgrade/business` | Upgrade from Base to Business account.               | Base          |

---

## 2. Outings (`/api/outings`)

| Method   | Route                            | Description                                                         | User Role     |
| :------- | :------------------------------- | :------------------------------------------------------------------ | :------------ |
| `GET`    | `/api/outings`                   | Retrieve a list of outings with filters (location, date, category). | Authenticated |
| `POST`   | `/api/outings`                   | Create a new outing.                                                | Base, Premium |
| `GET`    | `/api/outings/{id}`              | Get details of a specific outing.                                   | Authenticated |
| `PUT`    | `/api/outings/{id}`              | Update an existing outing (title, date, participants).              | Organizer     |
| `DELETE` | `/api/outings/{id}`              | Delete an existing outing.                                          | Organizer     |
| `GET`    | `/api/outings/{id}/participants` | Get the list of participants in the outing.                         | Organizer     |
| `POST`   | `/api/outings/event/{eventId}`   | Create an outing linked to a specific event.                        | Premium       |

---

## 3. Events (`/api/events`)

| Method   | Route                                        | Description                                         | User Role     |
| :------- | :------------------------------------------- | :-------------------------------------------------- | :------------ |
| `GET`    | `/api/events`                                | Retrieve a list of events (searchable by filters).  | Authenticated |
| `POST`   | `/api/events`                                | Create a new event.                                 | Business      |
| `GET`    | `/api/events/{id}`                           | Get details of a specific event.                    | Authenticated |
| `PUT`    | `/api/events/{id}`                           | Update an existing event.                           | Business      |
| `DELETE` | `/api/events/{id}`                           | Delete an existing event.                           | Business      |
| `POST`   | `/api/events/{id}/authorize-premium`         | Authorize a Premium user to create linked outings.  | Business      |
| `GET`    | `/api/events/{id}/linked-outings`            | Retrieve outings linked to this event.              | Authenticated |
| `GET`    | `/api/events/{id}/rewards`                   | Retrieve rewards or promotions linked to the event. | Authenticated |
| `POST`   | `/api/events/{id}/rewards/{rewardId}/assign` | Assign a reward to a Premium user.                  | Business      |

---

## 4. Participation (`/api/outings/{id}/participation`)

| Method   | Route                                         | Description                                           | User Role     |
| :------- | :-------------------------------------------- | :---------------------------------------------------- | :------------ |
| `POST`   | `/api/outings/{id}/participate`               | Request to join an outing.                            | Authenticated |
| `DELETE` | `/api/outings/{id}/withdraw`                  | Withdraw from an outing.                              | Authenticated |
| `GET`    | `/api/outings/{id}/requests`                  | List pending participation requests.                  | Organizer     |
| `POST`   | `/api/outings/{id}/requests/{userId}/approve` | Approve a participation request.                      | Organizer     |
| `POST`   | `/api/outings/{id}/requests/{userId}/reject`  | Reject a participation request.                       | Organizer     |
| `GET`    | `/api/profile/my-participations`              | List all outings and events the user participates in. | Authenticated |

---

## 5. Chat & Real-Time Communication (`/api/chats`, `/ws`)

| Method | Route                                   | Description                                                                         | User Role              |
| :----- | :-------------------------------------- | :---------------------------------------------------------------------------------- | :--------------------- |
| `GET`  | `/api/chats/outing/{outingId}/messages` | Retrieve message history for an outing’s chat.                                      | Participant, Organizer |
| `POST` | `/api/chats/outing/{outingId}/messages` | Send a new chat message.                                                            | Participant, Organizer |
| **WS** | `/ws/chat/{outingId}`                   | WebSocket endpoint for real-time chat messages.                                     | Participant, Organizer |
| **WS** | `/ws/notifications`                     | WebSocket endpoint for real-time notifications (e.g., new participation, approval). | Authenticated          |

---

## 6. Reviews & Ratings (`/api/reviews`)

| Method | Route                       | Description                                 | User Role   |
| :----- | :-------------------------- | :------------------------------------------ | :---------- |
| `POST` | `/api/outings/{id}/reviews` | Submit a review for a completed outing.     | Participant |
| `GET`  | `/api/outings/{id}/reviews` | Retrieve all reviews for a specific outing. | Public      |
| `POST` | `/api/events/{id}/reviews`  | Submit a review for a completed event.      | Participant |
| `GET`  | `/api/events/{id}/reviews`  | Retrieve all reviews for a specific event.  | Public      |

---

## 7. Rewards & Vouchers (`/api/rewards`, `/api/vouchers`)

| Method | Route                              | Description                                                    | User Role     |
| :----- | :--------------------------------- | :------------------------------------------------------------- | :------------ |
| `POST` | `/api/rewards`                     | Create a new reward or incentive linked to an event.           | Business      |
| `GET`  | `/api/rewards/my-vouchers`         | Retrieve all vouchers owned by the authenticated Premium user. | Premium       |
| `POST` | `/api/vouchers/{voucherId}/redeem` | Redeem a voucher (e.g., via QR code scan).                     | Business      |
| `GET`  | `/api/vouchers/{voucherId}`        | Retrieve voucher details.                                      | Authenticated |

---

## 8. Notifications (`/api/notifications`)

| Method   | Route                           | Description                                        | User Role     |
| :------- | :------------------------------ | :------------------------------------------------- | :------------ |
| `GET`    | `/api/notifications`            | Retrieve notifications for the authenticated user. | Authenticated |
| `POST`   | `/api/notifications/read/{id}`  | Mark a notification as read.                       | Authenticated |
| `DELETE` | `/api/notifications/{id}`       | Delete a specific notification.                    | Authenticated |
| **WS**   | `/topic/notifications/{userId}` | WebSocket topic for live notifications.            | Authenticated |

---

## 9. Administration & Configuration (`/api/admin`)

| Method  | Route                    | Description                                   | User Role  |
| :------ | :----------------------- | :-------------------------------------------- | :--------- |
| `GET`   | `/api/admin/logs`        | Retrieve application logs.                    | Maintainer |
| `GET`   | `/api/admin/health`      | Check application and database status.        | Maintainer |
| `POST`  | `/api/admin/seed`        | Populate the database with sample data.       | Admin      |
| `GET`   | `/api/admin/config`      | Get current configuration from ConfigService. | Maintainer |
| `PATCH` | `/api/admin/config`      | Update configuration values at runtime.       | Maintainer |
| `GET`   | `/api/admin/users/roles` | Retrieve all available user roles.            | Admin      |

---

## 10. Maps & Geolocation (`/api/map`)

| Method | Route                                                | Description                                    | User Role     |
| :----- | :--------------------------------------------------- | :--------------------------------------------- | :------------ |
| `GET`  | `/api/map/events/nearby?lat=...&lon=...&radius=...`  | Retrieve nearby events within a given radius.  | Authenticated |
| `GET`  | `/api/map/outings/nearby?lat=...&lon=...&radius=...` | Retrieve nearby outings within a given radius. | Authenticated |
| `GET`  | `/api/map/autocomplete?q=...`                        | Get autocomplete suggestions for locations.    | Authenticated |

---

# 🔐 Roles & Permissions

| Role              | Permissions                                                                  |
| :---------------- | :--------------------------------------------------------------------------- |
| **Base User**     | Create and join outings.                                                     |
| **Premium User**  | All Base privileges + create unlimited outings from events + access rewards. |
| **Business User** | Create and manage events, define reward programs.                            |
| **Admin**         | Manage users, roles, and system content.                                     |
| **Maintainer**    | Access logs, configuration, and system health checks.                        |

---

Would you like me to also generate a **short “endpoint index” section** (just the list of paths and methods, no descriptions) for quick reference at the top of your documentation or OpenAPI summary?
