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

### 2. Configure environment variables

| Variable | Used by | Required | Purpose |
| --- | --- | --- | --- |
| `JWT_SECRET` | Backend | **Yes** — the API will not start without it | Base64-encoded key (at least 32 bytes) for signing login tokens |
| `MAPBOX_ACCESS_TOKEN` | Backend | For distances | Looks up coordinates for listing pickup addresses. Without it, listings are saved without coordinates and `distanceKm` is always `null` |
| `JWT_EXPIRATION_MS` | Backend | No (default `3600000`, 1 hour) | Login token lifetime |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Backend | No | Override the MySQL connection (see step 1) |
| `VITE_MAPBOX_TOKEN` | Frontend | For address autocomplete | Public Mapbox token (`pk.…`) for the address search boxes. Without it, the location box on the browse page only filters by suburb name |
| `API_PROXY_TARGET`, `VITE_API_BASE_URL` | Frontend | No | Point the frontend at a different API server (see step 4) |

Backend variables must be set in the environment that starts the API:

```bash
# Generate a secret once, e.g.: openssl rand -base64 48
export JWT_SECRET='replace-with-a-base64-encoded-secret'
export MAPBOX_ACCESS_TOKEN='pk.your-mapbox-token'
```

If you run the backend from IntelliJ, add these under **Run → Edit Configurations → WebappApplication → Environment variables** instead; IntelliJ does not read your shell's `export`s.

Frontend variables go in `frontend/.env.local` (git-ignored); copy `frontend/.env.example` to get started:

```bash
cp frontend/.env.example frontend/.env.local
# then set VITE_MAPBOX_TOKEN=pk.your-mapbox-token
```

The same public Mapbox token can be used for both `MAPBOX_ACCESS_TOKEN` and `VITE_MAPBOX_TOKEN`. Create one at <https://account.mapbox.com/access-tokens/>.

> **Distances not showing?** Check that the backend was started with `MAPBOX_ACCESS_TOKEN`. On startup without it, the log shows `mapbox.access-token is not set; listing distances are disabled.` Listings created while it was missing have no coordinates. Restart with the token set and recreate them (the development `ddl-auto=create` setting wipes data on restart; with a persistent schema, existing listings are filled in automatically at startup).

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
| `backend/webapp/` | `mvn test` | Run backend tests (uses an in-memory database; no MySQL or environment variables needed) |
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
