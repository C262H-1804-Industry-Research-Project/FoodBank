# Update plan

Improvements found in a review of the codebase (October 2026), ordered by priority.
Each item says what is wrong, where it is, and the proposed fix.

## 1. Bugs and security (do first)

- [ ] **Photos between 1 MB and 5 MB fail to upload**
  - The upload forms allow up to 5 MB, but Spring Boot rejects multipart uploads over 1 MB by default, and `backend/webapp/src/main/resources/application.properties` doesn't raise the limit.
  - Fix: set `spring.servlet.multipart.max-file-size=5MB` and `spring.servlet.multipart.max-request-size=6MB`. Handle `MaxUploadSizeExceededException` in `ApiExceptionHandler` so it returns a clear 400.

- [ ] **Uploads accept any file type**
  - `ImageUploadController` keeps the extension the client sends, and `/uploads/**` is served publicly from the app's own origin. A logged-in user could upload an `.html` file containing JavaScript, and it would run in the app when someone opens the link.
  - Fix: accept only PNG, JPEG and WebP, check the file contents rather than the name, and save files with a server-chosen extension.

- [ ] **Timeslots can be overbooked**
  - `RequestService.createRequest` reads `remainingSlot` and then saves `remainingSlot - 1` without a lock. Two recipients booking the last place at the same time can both succeed.
  - Fix: lock the timeslot row when booking (`@Lock(PESSIMISTIC_WRITE)` on the lookup), or add a `@Version` field to `Timeslot`.

- [ ] **Logins live only in server memory**
  - `AuthTokenService` keeps active tokens in a `ConcurrentHashMap`, so every restart logs everyone out, and the backend can't run as more than one instance.
  - Fix: keep JWTs stateless, and store revocations (logout, bans) in the database. For example, a per-user "tokens issued before" timestamp.

- [ ] **Default admin account in non-local environments**
  - `DefaultAdminSeeder` creates `admin@test.com` / `password` unless `DEFAULT_ADMIN_EMAIL` is blank.
  - Fix: set `DEFAULT_ADMIN_EMAIL` blank in any shared or deployed environment, or change the default so the seeder is off.

## 2. Backend robustness

- [ ] **Add database migrations (Flyway)**
  - `spring.jpa.hibernate.ddl-auto=update` never removes or renames columns and can't migrate data, which is why the `PENDING` to `ACTIVE` account change needed manual SQL.
- [ ] **Use Bean Validation**
  - Fields are checked by hand with `requireText` in each service, and there are no `@Valid`, `@NotBlank` or `@Email` annotations. As a result, email formats and password lengths aren't checked.
- [ ] **Replace status strings with enums**
  - Values like `"APPROVED"`, `"BANNED"` and `"PENDING"` are repeated across services, so a typo compiles and fails silently.
- [ ] **Map relationships in entities**
  - `Report`, `Request` and `BanRecord` store plain `Integer` IDs. This forces ad-hoc joins and many small queries; for example, `RequestService.toResponse` runs several lookups for each row.
- [ ] **Move admin lists to database queries**
  - `AdminAccountService.findAccounts` and `AdminReportService` load whole tables and filter in memory. This is fine at current scale, but should become database queries if usage grows.
- [ ] **Use `SecureRandom` for pickup codes**
  - `RequestService.generatePickupCode` uses `java.util.Random`.

## 3. Frontend maintainability

- [ ] **Extract shared components**
  - The donor and recipient report lists are about 430 lines each and nearly identical.
  - Pagination, the photo upload box, the photo preview modal and the "load page, abort, retry" logic are repeated across 5–6 pages.
  - Proposed: `<Pagination>`, `<PhotoUpload>`, `<Modal>` and a `usePagedList()` composable.
- [ ] **Split the largest components**
  - `AuthIndex.vue` (~790 lines), `MisuseReports.vue`, `NewListing.vue` and `BrowseDonations.vue` (~600–670 lines each).
- [ ] **Replace `window.alert`**
  - There are 18 calls. Use inline messages or a toast component.
- [ ] **Review session storage**
  - The JWT is kept in `localStorage`, where any injected script can read it. This makes the upload issue in section 1 more serious.

## 4. Unfinished features

- [ ] **Collection history** for donors and recipients is still hard-coded: `historyData` in `DonorIndex.vue` and `myHistoryList` in `RecipientIndex.vue`.
- [ ] **Admin "Pending approvals" and "System settings"** are placeholders. Decide whether "Pending approvals" is still needed, since accounts are now approved when they are created.
- [ ] **Unused endpoints:** `GET /api/donors` and `GET /api/recipients` are no longer used by the frontend. Remove them or document them.

## 5. Docs and process

- [ ] **Update the README feature list.** It doesn't mention reports, bans or admin features.
- [ ] **Bring `api/openapi.yaml` up to date.** It documents 18 paths, but the backend has about 40 endpoints. Either generate it from Springdoc or remove it.
- [ ] **Remove or finish `api/schema/`.** It contains only `loginRequest.json`.

## Suggested order

1. Upload size limit and file-type validation (small, user-visible)
2. Timeslot overbooking fix
3. Flyway migrations
4. Shared frontend components
5. Remaining mock pages (collection history)
6. Docs clean-up
