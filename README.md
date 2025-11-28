# GathorApp Monorepo

Outing and event management system with geolocation, real-time chat, and voucher system.

## Project Structure

```
gathorapp-mono/
├── backend/          # Spring Boot 3.5.6 + Java 17
├── frontend/         # Angular 20 (standalone components)
└── docs/            # UML Documentation (Mermaid)
```

## Requirements

### Backend
- Java 17+
- Gradle 8.5+
- PostgreSQL 15+ (prod) / H2 (dev/test)

### Frontend
- Node.js 20+ (see `.nvmrc`)
- npm 10+

## Quick Start

### Backend

```bash
cd backend

# Build and test
gradle clean build

# Run with H2 in-memory
gradle bootRun
```

Backend available at: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

### Frontend

```bash
cd frontend

# Install dependencies
npm install

# Dev server with proxy
npm start
```

Frontend available at: `http://localhost:4200`

## Architecture

### Backend - Packages

- `auth` - JWT Authentication + Refresh Token
- `user` - User management (USER, PREMIUM, BUSINESS)
- `event` - Business-created events (with geolocation)
- `outing` - User-organized outings
- `participation` - Participation system with concurrency control
- `chat` - Real-time chat via WebSocket (auto-deactivation)
- `notification` - Notifications with Observer Pattern
- `voucher` - Voucher system with QR code
- `report` - Report system 
- `reward` - Event rewards
- `review` - Review system
- `pattern/strategy` - Strategy Pattern for user limits
- `pattern/observer` - Observer Pattern for notifications

### Implemented Design Patterns

1. **Strategy Pattern** - Role-based user limits
   - BaseUserStrategy: 5 outings/month, max 10 participants
   - PremiumUserStrategy: 10 outings/month, unlimited event-linked
   - BusinessUserStrategy: unlimited

2. **Observer Pattern** - Multi-channel notifications
   - PersistenceObserver: saves to DB
   - WebSocketObserver: real-time push

### Multithreading

- `@Scheduled` tasks for chat deactivation (2:00 AM) and voucher expiration (3:00 AM)
- `synchronized` methods with `SERIALIZABLE` isolation for race condition prevention
- Observer pattern with parallel execution
- WebSocket with internal thread pool

## Testing

```bash
cd backend

# Run all tests
gradle test

# Coverage report (JaCoCo)
gradle jacocoTestReport
# Report: backend/build/reports/jacoco/test/html/index.html

# Coverage verification (min 60%)
gradle jacocoTestCoverageVerification
```

Currently: **585 tests** with **100% pass rate**

## Documentation

### Technical Documentation

Complete technical documentation is available in `docs/`:

```bash
cd docs

python generate-pdf.bat

# Windows wrapper
generate-pdf.bat

# Unix/Linux/macOS wrapper
./generate-pdf.sh
```

**Requirements:** Pandoc 3.8+ and XeLaTeX (MiKTeX/MacTeX/TeXLive)

**Generated files:**
- `docs/pdf/GATHORAPP.pdf` - Main technical documentation
- `docs/pdf/*.pdf` - UML diagrams (7 files)

See `docs/README.md` for compilation details.

### UML Diagrams

Source files in `docs/source/`:

- `class-diagram.mermaid` - Entities, patterns, multithreading
- `sequence-diagram.mermaid` - Participation with Observer Pattern
- `sequence-diagram-chat-websocket.mermaid` - Real-time chat via WebSocket
- `sequence-diagram-voucher-redemption.mermaid` - Voucher lifecycle
- `use-case-diagram-1-events-outings.mermaid` - Events & Outings
- `use-case-diagram-2-communication-rewards.mermaid` - Communication & Rewards
- `use-case-diagram-3-administration.mermaid` - Administration

View diagrams online: https://mermaid.live

### API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Technologies

### Backend
- Spring Boot 3.5.6
- Spring Security + JWT
- Spring Data JPA
- Spring WebSocket (STOMP)
- PostgreSQL / H2
- Lombok
- MapStruct
- JUnit 5 + Mockito
- JaCoCo (coverage)

### Frontend
- Angular 20
- TypeScript 5.7
- Standalone components
- OpenAPI Generator (auto-generated client)
- Leaflet (maps)
- STOMP.js (WebSocket)

## Database Seeding

DataSeeder automatically creates:
- Admin user
- Business users with events and rewards
- Premium users
- Regular users
- Outings with participations
- Chat messages
- Reviews

Default credentials:
- Admin: `admin@gathorapp.com` / `password123`
- Business: `business1@example.com` / `password123`
- Premium: `premium1@example.com` / `password123`
- User: `user1@example.com` / `password123`

## Key Features

1. **Event Management**
   - Creation (Business only)
   - Nearby search with radius
   - Map visualization

2. **Outing Management**
   - Creation with or without associated event
   - Join with race condition handling
   - Role-based limits (Strategy Pattern)

3. **Participation System**
   - Pending requests
   - Approval/rejection by organizer
   - Concurrency control (SERIALIZABLE + synchronized)

4. **Real-time Chat**
   - WebSocket/STOMP
   - Auto-deactivation 7 days after outing
   - Typing indicator

5. **Notifications**
   - Multi-channel (DB + WebSocket) via Observer Pattern
   - Real-time push when user online
   - Persistent history

6. **Voucher System**
   - Automatic generation for Premium organizers
   - Unique QR code
   - 60-day expiration
   - Redemption by Business

7. **Review & Rating**
   - 1-5 stars for events/outings
   - Text comments
   - Calculated average rating

## Security

- JWT authentication with refresh token
- Hashed passwords (BCrypt)
- Configured CORS
- WebSocket authentication via interceptor
- Role-based authorization

## License

Academic use - All rights reserved
