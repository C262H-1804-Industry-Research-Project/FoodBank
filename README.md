# FoodBank — Second Table

Second Table is a local food-rescue and redistribution platform. Donors publish surplus food and pickup timeslots; recipients browse available listings and request collection; admins moderate reports, accounts and listings.

The project is split into a Vue 3 single-page application and a Spring Boot REST API backed by MySQL.

## Features

### User management
- Donor and recipient registration; new accounts are signed in straight away
- JWT login and logout, with role-based access for donors, recipients and admins
- One account can hold both roles: register for both at sign-up, or add the second role later, then switch between them

### Food donation (donors)
- Create, edit and delete listings with a photo, category, dietary tags, storage notes, quantity, expiry and collect-by dates, and pickup address
- Pickup timeslots with a recipient limit per slot; slots display as `YYYY-MM-DD: HH:mm - HH:mm` and cannot cross into another date
- Publish a listing or save it as a draft; publishing requires at least one pickup timeslot
- **My listings** shows stock left, collection rate, and highlights listings past their collect-by date

### Food browsing (recipients)
- Search by food name, category, tags and suburb, with paginated results
- Remaining places shown for each pickup timeslot
- Estimated distance from the recipient's saved location (requires Mapbox, see below)

### Pickup requests
- Recipients request a listing for a chosen timeslot; a slot can be booked until 30 minutes before it ends
- Recipients can cancel a request before its pickup starts
- Donors see a count of pending requests, approve or reject them, and mark them as collected to update stock
- Optional auto-approval and pickup codes per listing
- Pending requests not reviewed by the end of their slot are rejected automatically; approved requests not collected within the handover grace period expire, and their stock is released nightly
- Collection history for donors and recipients

### Reporting
- Donors and recipients can report approved, completed or expired (no-show) pickups, with an optional evidence image
- Users track the status of their own reports
- Admins review reports and dismiss, warn, ban for 1–365 days, or deactivate the reported account; temporary bans lift automatically

### Administration monitoring
- Account statistics: requests made vs. successful collections, reports against each user, and ban history
- Ban, deactivate and reactivate accounts; banned users are logged out and cannot log in
- Review all listings with their collection rate, and remove inappropriate ones

## Tech stack

| Area | Technology |
| --- | --- |
| Frontend | Vue 3, Vue Router, Vite |
| Backend | Java 17, Spring Boot, Spring Security, Spring Data JPA, Spring Scheduling |
| Database | MySQL (H2 in-memory for tests) |
| Authentication | JWT (`jjwt`), BCrypt password hashing |
| Location services | Mapbox Search Box API (address autocomplete and geocoding) |
| API documentation | Springdoc OpenAPI / Swagger UI |
| Testing | JUnit 5, Spring Boot Test, MockMvc (backend); Vitest, Vue Test Utils, jsdom (frontend) |
| CI | GitHub Actions (separate backend and frontend workflows) |

## Repository layout

```text
.
├── frontend/              # Vue application
├── backend/webapp/        # Spring Boot API
├── api/openapi.yaml       # API contract
├── .github/workflows/     # CI: backend and frontend tests
└── CHANGELOG.md           # Recent project changes
```

## Prerequisites

- Node.js 22.12+ (or 24+) and npm — pinned in `frontend/.nvmrc` and the `engines` field of `frontend/package.json`; with nvm, run `nvm use` in `frontend/`
- Java 17+ — set as `java.version` in `backend/webapp/pom.xml`
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- MySQL 8+

Library dependencies are listed in `frontend/package.json` (installed with `npm install`) and `backend/webapp/pom.xml` (downloaded by Maven on the first build).

## Local setup

### 1. Clone the repository

```bash
git clone https://github.com/C262H-1804-Industry-Research-Project/FoodBank
cd FoodBank
```

All later commands are run from this `FoodBank` folder unless a step says otherwise.

### 2. Create the database

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

The schema is created and updated automatically from the entities on startup (`spring.jpa.hibernate.ddl-auto=update`); existing data is kept.

### 3. Configure environment variables

| Variable | Used by | Required | Purpose |
| --- | --- | --- | --- |
| `JWT_SECRET` | Backend | **Yes** — the API will not start without it | Base64-encoded key (at least 32 bytes) for signing login tokens |
| `MAPBOX_ACCESS_TOKEN` | Backend | For distances | Looks up coordinates for listing pickup addresses. Without it, listings are saved without coordinates and `distanceKm` is always `null` |
| `JWT_EXPIRATION_MS` | Backend | No (default `3600000`, 1 hour) | Login token lifetime |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Backend | No | Override the MySQL connection (see step 2) |
| `DEFAULT_ADMIN_NAME`, `DEFAULT_ADMIN_EMAIL`, `DEFAULT_ADMIN_PASSWORD` | Backend | No (defaults `admin`, `admin@test.com`, `password`) | Admin account created on startup if it does not exist. **Change the password outside local development.** Set `DEFAULT_ADMIN_EMAIL` to blank to turn this off |
| `HANDOVER_GRACE_MINUTES` | Backend | No (default `15`) | Minutes after a slot ends that a donor can still mark a pickup as collected |
| `STOCK_RELEASE_CRON`, `STOCK_RELEASE_ZONE` | Backend | No (default `0 20 0 * * *`, `Australia/Adelaide`) | When the nightly job releases stock held by expired requests |
| `REQUEST_EXPIRY_CHECK_MS`, `BAN_EXPIRY_CHECK_MS` | Backend | No (default `60000`) | How often to close ended requests and lift finished temporary bans |
| `VITE_MAPBOX_TOKEN` | Frontend | For address autocomplete | Public Mapbox token (`pk.…`) for the address search boxes. Without it, the browse page hides the "Your location" box, and the suburb filter has no suggestions (type a suburb name and press Enter instead) |
| `API_PROXY_TARGET`, `VITE_API_BASE_URL` | Frontend | No | Point the frontend at a different API server (see step 5) |

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

> **Distances not showing?** Check that the backend was started with `MAPBOX_ACCESS_TOKEN`. On startup without it, the log shows `mapbox.access-token is not set; listing distances are disabled.` Restart with the token set: listings saved without coordinates are filled in automatically at startup.

### 4. Start the backend

```bash
cd backend/webapp
./mvnw spring-boot:run
```

The API runs on `http://localhost:8080` by default. Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

Uploaded listing photos are stored in an `uploads/` folder in the directory the backend is started from, and served at `/uploads/...`.

### 5. Start the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Vite proxies `/api` requests to `http://localhost:8080`. To use another API server, set `API_PROXY_TARGET`. To bypass the proxy altogether, set `VITE_API_BASE_URL` to that server's API base URL.

### 6. Sign in

- **Donors and recipients:** register from the sign-up page; you are signed in straight away.
- **Admin:** use the default admin account from step 3 (`admin@test.com` / `password` unless overridden) on the admin login.

## Development commands

| Location | Command | Purpose |
| --- | --- | --- |
| `frontend/` | `npm run dev` | Start the Vite development server |
| `frontend/` | `npm run build` | Create a production frontend build |
| `frontend/` | `npm run preview` | Preview the production build |
| `frontend/` | `npm test` | Run frontend tests once (Vitest; API and Mapbox are mocked) |
| `frontend/` | `npm run test:watch` | Re-run frontend tests on every change |
| `backend/webapp/` | `./mvnw compile` | Compile the API |
| `backend/webapp/` | `./mvnw test` | Run backend tests (uses an in-memory database; no MySQL or environment variables needed) |
| `backend/webapp/` | `./mvnw spring-boot:run` | Start the API |

## API overview

| Area | Endpoints |
| --- | --- |
| Authentication | `POST /api/auth/register-donors`, `/register-recipients`, `/register-both`, `/login`, `/logout` |
| Account roles | `POST /api/account/add-role`, `/switch-role`, `/link` |
| Browse listings | `GET /api/listings`, `/api/listings/categories`, `/api/listings/tags`, `GET /api/suggestions/foodnames` |
| Recipient location | `GET`, `PUT`, `DELETE /api/recipients/me/location` |
| Donor listings | `GET`, `POST /api/donors/{donorId}/listings`; `PUT`, `DELETE /api/donors/{donorId}/listings/{listingId}` |
| Photo upload | `POST /api/uploads` |
| Pickup requests (recipient) | `GET`, `POST /api/requests`; `DELETE /api/requests/{requestId}` |
| Pickup requests (donor) | `GET /api/donors/{donorId}/requests`; `PATCH .../{requestId}/approve`, `/reject`, `/complete` |
| Collection history | `GET /api/donors/{donorId}/collection-history`, `GET /api/recipients/me/collection-history` |
| Reports | `POST /api/reports`, `GET /api/reports/statuses`, `GET /api/requests/reportable`, `GET /api/donors/{donorId}/requests/reportable`, `GET /api/donors/{donorId}/reports`, `GET /api/recipients/me/reports` |
| Admin | `GET /api/admin/accounts`; `POST /api/admin/users/{userId}/ban`, `/unban`; `POST /api/admin/listings/{listingId}/ban`; `GET`, `PATCH /api/admin/reports/{reportId}`; `GET`, `PUT /api/settings/ban-days`; `POST /api/admin/admins` |

Authenticated requests use `Authorization: Bearer <accessToken>`. A successful donor login includes `user.donorId`; the frontend uses it to load and create that donor's listings.

For the complete request and response contract, see [`api/openapi.yaml`](api/openapi.yaml) or Swagger UI.

## Testing and CI

From the repository root, validate frontend and backend changes with:

```bash
(cd frontend && npm test && npm run build)
(cd backend/webapp && ./mvnw test)
```

GitHub Actions runs the same checks on every push and pull request: the backend workflow when `backend/**` changes, and the frontend workflow (tests and production build) when `frontend/**` changes.
