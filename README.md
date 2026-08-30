# Resource Booking System

A secure RESTful API for booking resources (rooms, vehicles, equipment, etc.) built with Java 17, Spring Boot 3, MySQL, Spring Security, and JWT.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Prerequisites](#prerequisites)
- [MySQL Setup](#mysql-setup)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [Swagger / API Documentation](#swagger--api-documentation)
- [Seed Credentials](#seed-credentials)
- [API Overview](#api-overview)
- [Example Requests](#example-requests)
- [ADMIN vs USER Permissions](#admin-vs-user-permissions)

---

## Project Overview

This system provides a role-based resource booking platform with:

- **JWT authentication** — stateless, BCrypt-hashed passwords
- **Two roles** — `ADMIN` (full access) and `USER` (limited, own-data access)
- **Resources** — bookable items with name, description, and type
- **Reservations** — link a user to a resource with time range, price, and status
- **Reservation ownership** — user identity always comes from the JWT, never from the request body
- **Filtering** — by status, minimum price, maximum price
- **Pagination and sorting** — standard Spring Data pageable support
- **Centralized error handling** — consistent JSON error responses
- **OpenAPI/Swagger** — full documentation with JWT auth support

---

## Prerequisites

| Tool    | Version      |
|---------|--------------|
| Java    | 17           |
| Maven   | 3.8+         |
| MySQL   | 8.0+         |

> **Java 17**: Download from [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/#java17).  
> **Maven**: Download from [maven.apache.org](https://maven.apache.org/download.cgi) or use the included `mvnw` wrapper.  
> **MySQL**: Download from [mysql.com](https://dev.mysql.com/downloads/mysql/).

---

## MySQL Setup

1. Install and start MySQL 8.0+.
2. Create the database (the app can also create it automatically if you add `createDatabaseIfNotExist=true` to the URL):

```sql
CREATE DATABASE resource_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. Create a MySQL user (or use root for local development):

```sql
CREATE USER 'booking_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON resource_booking.* TO 'booking_user'@'localhost';
FLUSH PRIVILEGES;
```

---

## Configuration

The application reads configuration from `src/main/resources/application.properties`.

This file is **Git-ignored** so your credentials are never committed.

**Setup steps:**

1. Copy the example file:

```bash
cp src/main/resources/application-example.properties src/main/resources/application.properties
```

2. Edit `application.properties` and fill in your real values:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/resource_booking?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD

jwt.secret=YOUR_LONG_RANDOM_SECRET_AT_LEAST_32_CHARS
jwt.expiration-ms=86400000
```

**JWT secret**: Generate a strong random value — at least 32 characters. You can use:

```bash
openssl rand -base64 64
```

**Alternatively**, set environment variables instead of editing the file:

| Variable          | Description                          |
|-------------------|--------------------------------------|
| `DB_URL`          | Full JDBC URL for MySQL              |
| `DB_USERNAME`     | MySQL username                       |
| `DB_PASSWORD`     | MySQL password                       |
| `JWT_SECRET`      | JWT signing secret (min 32 chars)    |
| `JWT_EXPIRATION_MS` | Token expiry in milliseconds (default: 86400000 = 24h) |

---

## Running the Application

Using the Maven wrapper (no separate Maven installation needed):

**Linux / macOS:**
```bash
./mvnw spring-boot:run
```

**Windows:**
```cmd
mvnw.cmd spring-boot:run
```

Or build and run the JAR:

```bash
./mvnw package -DskipTests
java -jar target/resource-booking-0.0.1-SNAPSHOT.jar
```

The application starts on **http://localhost:8080**.

---

## Running Tests

Tests use an **H2 in-memory database** — no MySQL needed to run them.

```bash
./mvnw test
```

Or on Windows:

```cmd
mvnw.cmd test
```

---

## Swagger / API Documentation

Once the application is running, open:

**http://localhost:8080/swagger-ui.html**

To authenticate in Swagger:
1. Call `POST /auth/login` with admin or user credentials.
2. Copy the `token` from the response.
3. Click the **Authorize** button (top right).
4. Enter `<your-token>` in the Bearer field and click **Authorize**.

---

## Seed Credentials

The following users are created automatically on startup:

| Role  | Username | Password  |
|-------|----------|-----------|
| ADMIN | `admin`  | `admin123` |
| USER  | `user`   | `user123`  |

Passwords are stored as BCrypt hashes — never in plain text.

---

## API Overview

### Authentication

| Method | Endpoint       | Description        | Auth Required |
|--------|----------------|--------------------|---------------|
| POST   | /auth/login    | Login, get JWT     | No            |

### Resources

| Method | Endpoint              | Description           | Role Required |
|--------|-----------------------|-----------------------|---------------|
| GET    | /api/resources        | List resources        | Any           |
| GET    | /api/resources/{id}   | Get resource by ID    | Any           |
| POST   | /api/resources        | Create resource       | ADMIN         |
| PUT    | /api/resources/{id}   | Update resource       | ADMIN         |
| DELETE | /api/resources/{id}   | Delete resource       | ADMIN         |

### Reservations

| Method | Endpoint                  | Description                            | Role Required      |
|--------|---------------------------|----------------------------------------|--------------------|
| GET    | /api/reservations         | List reservations (filtered/paginated) | Any (scoped)       |
| GET    | /api/reservations/{id}    | Get reservation by ID                  | Owner or ADMIN     |
| POST   | /api/reservations         | Create reservation                     | Any authenticated  |
| PUT    | /api/reservations/{id}    | Update reservation                     | Owner or ADMIN     |
| DELETE | /api/reservations/{id}    | Delete reservation                     | Owner or ADMIN     |

#### Reservation List Query Parameters

| Parameter  | Type             | Description                          |
|------------|------------------|--------------------------------------|
| `status`   | PENDING / CONFIRMED / CANCELLED | Filter by status |
| `minPrice` | decimal          | Minimum price filter                 |
| `maxPrice` | decimal          | Maximum price filter                 |
| `page`     | integer (0-based)| Page number                          |
| `size`     | integer          | Items per page                       |
| `sort`     | field,direction  | e.g. `price,asc` or `startTime,desc` |

---

## Example Requests

### Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "role": "ROLE_ADMIN"
}
```

### Create a Resource (ADMIN)

```bash
curl -X POST http://localhost:8080/api/resources \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Conference Room A","description":"Large boardroom","type":"room"}'
```

### Create a Reservation (USER)

```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceId": 1,
    "startTime": "2027-01-15T09:00:00",
    "endTime": "2027-01-15T10:00:00",
    "price": 150.00
  }'
```

> The `userId` is taken from the JWT — **never supply it in the request body**.

### List Reservations with Filtering and Pagination

```bash
# As ADMIN – see all
curl "http://localhost:8080/api/reservations?status=PENDING&minPrice=50&page=0&size=10&sort=price,desc" \
  -H "Authorization: Bearer <admin-token>"

# As USER – only sees own reservations
curl "http://localhost:8080/api/reservations?page=0&size=5" \
  -H "Authorization: Bearer <user-token>"
```

---

## ADMIN vs USER Permissions

| Action                          | ADMIN | USER  |
|---------------------------------|-------|-------|
| Login                           | ✅    | ✅    |
| View all resources              | ✅    | ✅    |
| Create / update / delete resources | ✅ | ❌    |
| Create reservations             | ✅    | ✅    |
| View own reservations           | ✅    | ✅    |
| View all reservations           | ✅    | ❌    |
| Update own reservation          | ✅    | ✅    |
| Update any reservation          | ✅    | ❌    |
| Delete own reservation          | ✅    | ✅    |
| Delete any reservation          | ✅    | ❌    |

---

## Reservation Statuses

| Status    | Description                        |
|-----------|------------------------------------|
| PENDING   | Default on creation                |
| CONFIRMED | Confirmed reservation              |
| CANCELLED | Cancelled reservation              |
