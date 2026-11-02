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

- Node.js and npm
- Java 17+
- Maven 3.9+ (or use the included `./mvnw` wrapper)
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

The schema is created and updated automatically from the entities on startup (`spring.jpa.hibernate.ddl-auto=update`); existing data is kept.

### 2. Configure environment variables

| Variable | Used by | Required | Purpose |
| --- | --- | --- | --- |
| `JWT_SECRET` | Backend | **Yes** — the API will not start without it | Base64-encoded key (at least 32 bytes) for signing login tokens |
| `MAPBOX_ACCESS_TOKEN` | Backend | For distances | Looks up coordinates for listing pickup addresses. Without it, listings are saved without coordinates and `distanceKm` is always `null` |
| `JWT_EXPIRATION_MS` | Backend | No (default `3600000`, 1 hour) | Login token lifetime |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Backend | No | Override the MySQL connection (see step 1) |
| `DEFAULT_ADMIN_NAME`, `DEFAULT_ADMIN_EMAIL`, `DEFAULT_ADMIN_PASSWORD` | Backend | No (defaults `admin`, `admin@test.com`, `password`) | Admin account created on startup if it does not exist. **Change the password outside local development.** Set `DEFAULT_ADMIN_EMAIL` to blank to turn this off |
| `HANDOVER_GRACE_MINUTES` | Backend | No (default `15`) | Minutes after a slot ends that a donor can still mark a pickup as collected |
| `STOCK_RELEASE_CRON`, `STOCK_RELEASE_ZONE` | Backend | No (default `0 20 0 * * *`, `Australia/Adelaide`) | When the nightly job releases stock held by expired requests |
| `REQUEST_EXPIRY_CHECK_MS`, `BAN_EXPIRY_CHECK_MS` | Backend | No (default `60000`) | How often to close ended requests and lift finished temporary bans |
| `VITE_MAPBOX_TOKEN` | Frontend | For address autocomplete | Public Mapbox token (`pk.…`) for the address search boxes. Without it, the browse page hides the "Your location" box, and the suburb filter has no suggestions (type a suburb name and press Enter instead) |
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

> **Distances not showing?** Check that the backend was started with `MAPBOX_ACCESS_TOKEN`. On startup without it, the log shows `mapbox.access-token is not set; listing distances are disabled.` Restart with the token set: listings saved without coordinates are filled in automatically at startup.

### 3. Start the backend

```bash
cd backend/webapp
./mvnw spring-boot:run
```

The API runs on `http://localhost:8080` by default. Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

Uploaded listing photos are stored in an `uploads/` folder in the directory the backend is started from, and served at `/uploads/...`.

### 4. Start the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Vite proxies `/api` requests to `http://localhost:8080`. To use another API server, set `API_PROXY_TARGET`. To bypass the proxy altogether, set `VITE_API_BASE_URL` to that server's API base URL.

### 5. Sign in

- **Donors and recipients:** register from the sign-up page; you are signed in straight away.
- **Admin:** use the default admin account from step 2 (`admin@test.com` / `password` unless overridden) on the admin login.

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


## Deployment Guide

This guide documents the Docker Compose setup used for the FoodBank demonstration on DigitalOcean. Run server commands from `/opt/foodbank/FoodBank-main` unless stated otherwise. Replace every `<PLACEHOLDER>` before running a command.

### Deployment environment and supporting files

- Server OS observed during troubleshooting: Ubuntu 24.04.5 LTS, x86_64.
- Docker Engine and Docker Compose v2.
- Windows PowerShell with SSH and SCP for uploading source archives.
- MySQL 8.4 in Docker; frontend served by Nginx; Java 17 backend.
- Container builds provide Node.js/npm and Maven/Java, so these do not need separate host installations.
- Check available memory, swap and disk space before building. Small-memory servers may fail during builds.

The source archive does not include the deployment files. Add the accompanying deployment-files.zip contents at the repository root and commit them alongside this README:

```text
compose.yaml
backend/webapp/Dockerfile
backend/webapp/.dockerignore
frontend/Dockerfile
frontend/.dockerignore
frontend/deploy.nginx.conf
deploy/init-env.sh
```

Keep the repository root `.env` out of version control by adding `.env` to the root `.gitignore`. Do not overwrite existing ignore rules.

The deployment publishes port 80 only. Permit HTTP TCP 80 in the cloud firewall and restrict SSH TCP 22 to trusted client addresses. MySQL and the backend are accessed over the Compose network.

### Configuration

Create a root-level `.env`. For a FIRST deployment only, the supporting script generates database passwords and a JWT key:

```bash
sh deploy/init-env.sh
nano .env
chmod 600 .env
```

Example format only; these values are placeholders, not working credentials:

```dotenv
DB_PASSWORD=<DATABASE_PASSWORD>
DB_ROOT_PASSWORD=<MYSQL_ROOT_PASSWORD>
JWT_SECRET=<BASE64_ENCODED_KEY_AT_LEAST_32_BYTES>
DEFAULT_ADMIN_EMAIL=<ADMIN_EMAIL>
DEFAULT_ADMIN_PASSWORD='<ADMIN_PASSWORD>'
MAPBOX_ACCESS_TOKEN=<SERVER_MAPBOX_PUBLIC_TOKEN>
VITE_MAPBOX_TOKEN=<FRONTEND_MAPBOX_PUBLIC_TOKEN>
```

| Variable | Purpose |
| --- | --- |
| DB_PASSWORD | Password for the application's MySQL user |
| DB_ROOT_PASSWORD | MySQL root password; separate from the server SSH root password |
| JWT_SECRET | Backend token-signing key; keep the existing value during redeployment |
| DEFAULT_ADMIN_EMAIL / DEFAULT_ADMIN_PASSWORD | Creates an administrator only if that email does not already exist; leave email blank to disable |
| MAPBOX_ACCESS_TOKEN | Backend address-coordinate lookup |
| VITE_MAPBOX_TOKEN | Frontend address autocomplete; public pk. token, embedded at build time |

Do not put a secret sk. token in the frontend. A URL-restricted frontend token should be separate from the backend token, which must support server-originated requests.

Changing the frontend token requires rebuilding web. Changing administrator initialization variables does not reset an existing account's password.

### Database preparation and persistence

Compose creates `foodbankdb` and the `foodbank` MySQL user when its database volume is first initialized. A separate manual CREATE DATABASE step is not required for this Docker setup.

The backend connects to `jdbc:mysql://db:3306/foodbankdb`. Hibernate uses `ddl-auto=update`; schema changes may occur at startup and must be backed up before updates.

Named volumes preserve MySQL data and `/app/uploads`. Keep the same project directory and Compose project identity during updates so the existing volumes remain attached.

MySQL initialization variables do not change passwords in an already initialized volume. Never regenerate DB_PASSWORD during an update without a coordinated database password change. Do not use `docker compose down -v` or prune volumes containing application data.

### Upload and first deployment

From Windows PowerShell:

```powershell
scp "C:\Users\<WINDOWS_USER>\Downloads\FoodBank-deploy.zip" root@<SERVER_IP>:/root/FoodBank-deploy.zip
ssh root@<SERVER_IP>
```

For an Ubuntu host missing the required tools:

```bash
apt-get update
apt-get install -y docker.io docker-compose-v2 unzip openssl curl rsync
systemctl enable --now docker
```

On the server, using the prepared archive that contains source and deployment files:

```bash
mkdir -p /opt/foodbank
unzip /root/FoodBank-deploy.zip -d /opt/foodbank
cd /opt/foodbank/FoodBank-main
sh deploy/init-env.sh
nano .env
docker compose config --quiet
docker compose build backend
docker compose build web
docker compose up -d
```

Build services sequentially. The supplied Dockerfiles run backend verification and frontend tests before packaging. Stop and investigate a failed build rather than deploying unverified output.

### Redeploy an existing installation

Upload the new source archive from Windows:

```powershell
scp "C:\Users\<WINDOWS_USER>\Downloads\FoodBank-main.zip" root@<SERVER_IP>:/root/FoodBank-latest.zip
ssh root@<SERVER_IP>
```

In one server shell, first validate configuration and create a private backup directory:

```bash
cd /opt/foodbank/FoodBank-main
docker compose config --quiet
docker compose ps -a
backup_dir="/root/foodbank-backups/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$backup_dir"
chmod 700 "$backup_dir"
tar --exclude='FoodBank-main/frontend/node_modules' --exclude='FoodBank-main/frontend/dist' --exclude='FoodBank-main/backend/webapp/target' -czf "$backup_dir/project.tar.gz" -C /opt/foodbank FoodBank-main
backend_id=$(docker compose ps -aq backend)
test -n "$backend_id"
```

Stop application writes for a consistent backup; the website is unavailable during this maintenance window:

```bash
docker compose stop web backend
docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump --single-transaction --no-tablespaces -u foodbank foodbankdb' > "$backup_dir/database.sql"
docker cp "${backend_id}:/app/uploads" "$backup_dir/uploads"
test -s "$backup_dir/database.sql"
```

Check each command succeeds before continuing. Backups contain private configuration and user data; keep them outside the web root. If a backup fails, leave source unchanged and restart the existing services with `docker compose up -d`.

Extract to staging and update source while retaining deployment files:

```bash
release_dir=$(mktemp -d /root/foodbank-release-XXXXXX)
unzip /root/FoodBank-latest.zip -d "$release_dir"
ls "$release_dir/FoodBank-main"

rsync -av --delete --exclude='.env*' --exclude='node_modules/' --exclude='dist/' --exclude='Dockerfile' --exclude='.dockerignore' --exclude='deploy.nginx.conf' "$release_dir/FoodBank-main/frontend/" /opt/foodbank/FoodBank-main/frontend/

rsync -av --delete --exclude='.env*' --exclude='target/' --exclude='uploads/' --exclude='Dockerfile' --exclude='.dockerignore' "$release_dir/FoodBank-main/backend/webapp/" /opt/foodbank/FoodBank-main/backend/webapp/

docker compose config --quiet
docker compose build backend
docker compose build web
docker compose up -d --no-deps backend web
```

Run these commands individually, stopping on errors. The rsync deletion applies only to the two specified application directories, with the listed exclusions. It leaves the root .env and compose.yaml unchanged.

Rebuilding is required after code changes; restarting alone uses the existing image. Do not blindly restore a database backup if the new application has already changed the schema or accepted new data.

### Start, restart, stop and inspect

```bash
cd /opt/foodbank/FoodBank-main
docker compose up -d                         # Start configured services
docker compose restart backend web          # Restart existing application containers
docker compose stop web backend             # Maintenance: stop application, keep database
docker compose up -d --no-deps backend web   # Apply rebuilt application images/configuration
docker compose ps -a
docker compose logs --tail=100 backend web
```

A restart does not apply changed environment settings; use `up -d` to recreate affected services. These commands do not intentionally delete named volumes.

### Post-deployment checks

```bash
systemctl is-active docker
docker compose ps -a
curl -f http://localhost/
curl -f http://localhost/api/listings/categories
docker compose logs --tail=100 backend
```

Expected results: Docker active; db healthy; backend and web running; root returns HTML; categories endpoint returns JSON. A web HTTP 200 alone does not prove the backend works.

Open `http://<SERVER_IP>` and force-refresh with Ctrl+Shift+R. Verify existing-account login, donation creation, image upload, recipient requests, administrator actions and previously stored records/images.

For address autocomplete, enter an Australian address and inspect browser Network requests: /suggest should return 200 with suggestions and /retrieve should return 200 when selecting one. Configuration being non-empty does not establish token validity.

This demonstration configuration provides HTTP only. HTTPS requires additional configuration before collecting real user information.

### Troubleshooting and observed deployment issues

| Symptom | Diagnosis or action |
| --- | --- |
| PowerShell says root@... is not recognized | A destination address was entered as a command; use the complete scp command including the local file path |
| DB_PASSWORD missing during Compose interpolation | Correct the root .env variable name; a mistyped prefix was observed during setup. Preserve its existing value |
| Incorrect administrator email/password | Check initialization values and backend logs. Existing admin passwords are not overwritten by initialization |
| ERR_CONNECTION_REFUSED | Use http://, inspect Docker and container state, then check port 80/firewall if needed |
| db healthy but backend/web Exited | This state was observed after the services were stopped for maintenance; start them with up -d --no-deps backend web |
| Mapbox suggestions absent | Check VITE_MAPBOX_TOKEN, rebuild web, recreate services and force-refresh; inspect API response codes |
| Mapbox 401 / 403 / 429 | Check token validity / account access and URL restrictions / rate limits respectively |
| Build killed or exit 137 | Inspect free -h, swapon --show and available disk; build sequentially and address resource limits |
| Website returns 502 | Inspect backend logs and wait for successful backend startup |
| Database login fails after editing .env | Existing MySQL volume passwords do not change automatically; recover the matching configuration rather than deleting the volume |

### Verification record

Server screenshots confirm Ubuntu 24.04.5 LTS, Docker active, MySQL 8.4 healthy, and backend/web stopped after a maintenance stop. The application-start command above was supplied to restore those services; final startup and latest-version acceptance results were not provided in this record.

Earlier local validation recorded 150 passing frontend tests and 154 passing backend tests with successful builds. Those counts apply to the earlier source version, not automatically to the latest upload. Record the latest build results and actual post-deployment checks before marking this release verified.
