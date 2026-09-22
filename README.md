# FoodBank — Second Table

Second Table is a local food-rescue and redistribution platform. Donors publish surplus food and pickup timeslots; recipients browse available listings and request collection.

The project is split into a Vue 3 single-page application and a Spring Boot REST API backed by MySQL.

## Current features

- Donor and recipient registration, JWT login, and logout
- Public donation browsing with category, tag, food-name, and location filters
- Donor listing creation with categories, dietary tags, photos, address details, pickup timeslots, and capacity
- Donor **My listings** page backed by `GET /api/donors/{donorId}/listings`
- Pickup slots display as `YYYY-MM-DD: HH:mm - HH:mm`; the API rejects slots that cross into another date
- API catalogue endpoints for listing categories and tags
- Protected donor listing-management endpoints

Some navigation items remain placeholders while their corresponding workflows are under development.

## Tech stack

| Area | Technology |
| --- | --- |
| Frontend | Vue 3, Vue Router, Vite |
| Backend | Java 17, Spring Boot, Spring Security, Spring Data JPA |
| Database | MySQL |
| Authentication | JWT (`jjwt`) |
| API documentation | Springdoc OpenAPI / Swagger UI |

## Repository layout

```text
.
├── frontend/              # Vue application
├── backend/webapp/        # Spring Boot API
├── api/openapi.yaml       # API contract
└── CHANGELOG.md           # Recent project changes
```

## Prerequisites

- Node.js and npm
- Java 17+
- Maven 3.9+
- MySQL 8+

## Local setup

### 1. Create the database

Create a local MySQL database named `foodbankdb`:

```sql
CREATE DATABASE foodbankdb;
```

The default backend configuration is in `backend/webapp/src/main/resources/application.properties`. It currently targets `localhost:3306`, user `root`, and database `foodbankdb`. Override the datasource settings in your environment rather than committing local credentials:

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/foodbankdb'
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD='your-password'
```

> **Note:** the development configuration uses `spring.jpa.hibernate.ddl-auto=create`, which recreates the schema when the backend starts. Do not use this setting with data you need to keep.

### 2. Configure JWT signing

The backend requires a base64-encoded JWT secret of at least 32 bytes:

```bash
export JWT_SECRET='replace-with-a-base64-encoded-secret'
export JWT_EXPIRATION_MS=3600000
```

### 3. Start the backend

```bash
cd backend/webapp
mvn spring-boot:run
```

The API runs on `http://localhost:8080` by default. Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

### 4. Start the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Vite proxies `/api` requests to `http://localhost:8080`. To use another API server, set `API_PROXY_TARGET`. To bypass the proxy altogether, set `VITE_API_BASE_URL` to that server's API base URL.

## Development commands

| Location | Command | Purpose |
| --- | --- | --- |
| `frontend/` | `npm run dev` | Start the Vite development server |
| `frontend/` | `npm run build` | Create a production frontend build |
| `frontend/` | `npm run preview` | Preview the production build |
| `backend/webapp/` | `mvn compile` | Compile the API |
| `backend/webapp/` | `mvn test` | Run backend tests |
| `backend/webapp/` | `mvn spring-boot:run` | Start the API |

## API overview

| Area | Endpoint |
| --- | --- |
| Authentication | `POST /api/auth/register-donors`, `POST /api/auth/register-recipients`, `POST /api/auth/login`, `POST /api/auth/logout` |
| Browse listings | `GET /api/listings` |
| Listing metadata | `GET /api/listings/categories`, `GET /api/listings/tags` |
| Donor listings | `GET`, `POST /api/donors/{donorId}/listings` |

Authenticated requests use `Authorization: Bearer <accessToken>`. A successful donor login includes `user.donorId`; the frontend uses it to load and create that donor's listings.

For the complete request and response contract, see [`api/openapi.yaml`](api/openapi.yaml).

## Verification

From the repository root, validate the current frontend and backend changes with:

```bash
(cd frontend && npm run build)
(cd backend/webapp && mvn compile)
```

## Project status

This is an actively developed academic/industry research project. See [CHANGELOG.md](CHANGELOG.md) for the latest completed integration work.
