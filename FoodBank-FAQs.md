## For users

### 1. How do I create a new account?

Open the login page and select **Create Account**. Choose **Donor**, **Recipient** or **Both**, complete the required details, and click **Create account**.

### 2. Can I be both a donor and a recipient?

Yes. Select **Both** during registration. You can also add the other role to an existing account through the role-switching option.

### 3. How do I switch between donor and recipient identities?

Click **Switch to Recipient** or **Switch to Donor**. If you have two separate accounts, choose the option to link an existing account and enter its email and password. Once linked, you can switch within the website.

### 4. How can a donor publish a listing?

Log in as a donor and open **New listing**. Enter the food details, quantity, category, pickup address and timeslots, then click **Publish listing**. **Save as draft** keeps the listing unpublished.

### 5. How can a recipient find available food?

Open **Browse donations**. Search by food name or filter by category, tags and suburb. Listings display availability and pickup information.

### 6. How do I request food?

Click **Request** on an available listing, select a pickup timeslot, enter the quantity and submit. The request remains pending unless the donor has enabled automatic approval.

### 7. Why can’t I submit a pickup request?

The timeslot may be full or already started, the requested quantity may exceed available stock or daily limits, or you may already have a request for that timeslot. Check the displayed error message.

### 8. How does a donor manage pickup requests?

Open **Requests to approve** to approve or decline pending requests. After the food is collected, confirm completion to create a collection history record.

### 9. Can I cancel my request?

Yes. Open **My requests** and click **Cancel**. Cancellation is available for pending or approved requests before the pickup timeslot starts.

### 10. Where can I view previous donations or collections?

Donors can open **Donation history**, while recipients can open **Collection history**. These pages display recorded collection details.

### 11. How do I report a problem?

Open **File a report**, select an eligible pickup request, and provide the reason and relevant details. Recipients can report food-quality issues, while donors can report recipient behaviour. Updates appear in **Report list**.

### 12. Why am I asked to log in again?

Login sessions expire. Restarting the backend also clears active sessions in the current implementation. Log in again to continue.

## For developers

### 13. What software do developers need?

Install Node.js and npm, a full Java 17 or later JDK, and MySQL 8 or later. Node.js 24 is recommended. The repository includes a Maven Wrapper, so a separate Maven installation is optional.

### 14. How do I configure the database?

Create a MySQL database:

```sql
CREATE DATABASE foodbankdb;
```

Set the connection details in the environment used to start the backend. For a Bash terminal:

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/foodbankdb'
export SPRING_DATASOURCE_USERNAME='your-database-user'
export SPRING_DATASOURCE_PASSWORD='your-database-password'
```

Replace the username and password placeholders with your local database credentials.

### 15. How do I start the backend?

Set `JWT_SECRET` to a Base64-encoded signing key containing at least 32 bytes. For example, in Bash, starting from the repository root:

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
cd backend/webapp
./mvnw spring-boot:run
```

Keep the generated key for subsequent starts of the same local installation. The backend normally runs on port 8080. On Windows, use `mvnw.cmd` and configure the environment variables in your terminal or IDE.

### 16. How do I start the frontend?

Open another terminal at the repository root and run:

```bash
cd frontend
npm ci
npm run dev
```

Open the address printed by Vite, normally `http://localhost:5173`.

### 17. Why does the frontend show “Cannot connect to the donation API”?

This message indicates a failed network request. First check that the backend started successfully and is listening on port 8080. Vite proxies API requests to that port by default. If the backend uses another address, set `API_PROXY_TARGET` and restart Vite. If you set `VITE_API_BASE_URL`, check that address too. Network or CORS problems can also cause the request to fail.

### 18. Are Mapbox tokens required?

They are optional for the core donation workflow. Set `MAPBOX_ACCESS_TOKEN` for backend geocoding and `VITE_MAPBOX_TOKEN` for frontend address autocomplete. Restart the relevant services after changing them. Without tokens, address suggestions and distance features are limited or unavailable.

### 19. How do I run tests and check the frontend build?

In `frontend`, run:

```bash
npm test
npm run build
```

In `backend/webapp`, run:

```bash
./mvnw test
```

On Windows, use `mvnw.cmd test`. Backend tests use an isolated H2 database and test configuration, so they do not require a running MySQL server or real Mapbox credentials.

### 20. Will restarting the app delete existing data?

The current main configuration uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

With this configuration, existing MySQL records normally remain across restarts. Check any environment or profile overrides, which can change this behaviour.

Backend tests use a separate temporary H2 database with:

```properties
spring.jpa.hibernate.ddl-auto=create-drop
```