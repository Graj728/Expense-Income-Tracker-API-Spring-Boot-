# Expense & Income Tracker API (Spring Boot)

A backend API for tracking personal income/expenses with a group expense splitter
(equal / percentage / exact splits + debt simplification).

## Stack
- Java 17, Spring Boot 3.3
- Spring Web, Spring Data JPA, Spring Security (JWT)
- PostgreSQL + Flyway migrations
- Lombok, MapStruct-ready DTOs

## Setup

### Option A — Docker
See the top-level `docker-compose.yml` (ships alongside this project) — it
builds this backend, the frontend, and a Postgres container together:
```bash
docker compose up --build
```
Backend will be on `http://localhost:8080`.

### Option B — Local Maven + Postgres

1. Create a Postgres database:
   ```sql
   CREATE DATABASE expense_tracker;
   ```
2. Set environment variables (or edit `application.yml` directly):
   ```
   DB_USERNAME=postgres
   DB_PASSWORD=postgres
   JWT_SECRET=<a long random string, 256+ bits>
   ```
3. Run:
   ```
   mvn spring-boot:run
   ```
   Flyway will auto-create the schema and seed default categories on first startup.

## Core design decisions

- **Income is always personal.** `Transaction.group` must be `null` for `type=INCOME`
  (enforced by a DB check constraint and in the service layer).
- **Expenses can be personal or group-based.** Personal expenses go through
  `/api/transactions`; group expenses (which trigger the splitter) go through
  `/api/groups/{groupId}/expenses`.
- **Splitting strategies:** `EQUAL`, `PERCENTAGE`, `EXACT`. The splitter always
  guarantees the sum of shares equals the transaction amount exactly (remainder
  cents are distributed deterministically to avoid rounding drift).
- **Balances** are computed on the fly (not stored) from all `ExpenseSplit` rows
  plus recorded `Settlement`s, then reduced to the minimum number of suggested
  transfers via a greedy debt-simplification algorithm.

## API overview

### Auth
```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
```

### Categories
```
GET /api/categories?type=INCOME|EXPENSE
```

### Personal transactions (income + personal expenses)
```
POST   /api/transactions
GET    /api/transactions?type=&from=&to=&categoryId=&personalOnly=
GET    /api/transactions/{id}
PUT    /api/transactions/{id}
DELETE /api/transactions/{id}
```

### Groups
```
POST   /api/groups
GET    /api/groups
GET    /api/groups/{id}
POST   /api/groups/{id}/members
DELETE /api/groups/{id}/members/{userId}
```

### Group expenses (the splitter)
```
POST   /api/groups/{groupId}/expenses
GET    /api/groups/{groupId}/expenses
DELETE /api/groups/{groupId}/expenses/{transactionId}
```

Example request body (equal split):
```json
{
  "amount": 90.00,
  "date": "2026-07-01",
  "description": "Dinner",
  "categoryId": 6,
  "paidByUserId": 1,
  "splitType": "EQUAL",
  "participantUserIds": [1, 2, 3]
}
```

Example request body (exact split):
```json
{
  "amount": 100.00,
  "date": "2026-07-01",
  "description": "Groceries",
  "categoryId": 6,
  "paidByUserId": 1,
  "splitType": "EXACT",
  "shares": [
    { "userId": 1, "value": 40.00 },
    { "userId": 2, "value": 60.00 }
  ]
}
```

### Balances & settlements
```
GET  /api/groups/{groupId}/balances       // net balances + suggested transfers
POST /api/groups/{groupId}/settlements    // record a payment that clears debt
GET  /api/groups/{groupId}/settlements
```

### Reports
```
GET /api/reports/summary?from=2026-07-01&to=2026-07-31
```

## Authentication

All endpoints except `/api/auth/**` require a Bearer token:
```
Authorization: Bearer <accessToken>
```
Access tokens expire in 15 minutes; use `/api/auth/refresh` with the refresh
token to get a new pair.

## Notes / next steps
- Email OTP verification on registration is not yet implemented.
- Add pagination to list endpoints for large datasets.
- Add integration tests (Testcontainers + Postgres) for the splitter and
  balance-simplification logic — the rounding and greedy-matching paths are
  the highest-value places to test.
- Rate limiting on `/api/auth/**` is in-memory and per-instance; won't behave
  correctly if the app is ever scaled horizontally behind a load balancer.