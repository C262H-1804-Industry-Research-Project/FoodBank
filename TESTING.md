# Second Table Testing Documentation

## 1. Overview

Automated testing was recorded on 2 November 2026. Shuming Li conducted manual functional testing on 21 and 22 October 2026. The source was supplied as FoodBank-main.zip; a commit identifier was not recorded. Results apply to the locally tested source, not necessarily the deployed website.

## 2. Environment

- Manual environment: Windows 11 Home, build 26200; Chrome 155.0.8059.85 (64-bit).
- Backend: JUnit, Spring Boot Test, MockMvc and an in-memory H2 database.
- Frontend: Vitest 5.0.3, Vue Test Utils and jsdom; mocked responses where applicable.

## 3. Automated Results

| Category | Total | Passed | Failures / errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Backend service unit tests | 6 | 6 | 0 | 0 |
| Backend API and integration tests | 177 | 177 | 0 | 0 |
| Backend application-context test | 1 | 1 | 0 | 0 |
| **Backend total** | **184** | **184** | **0** | **0** |
| Frontend automated tests | 168 | 168 | 0 | 0 |
| **Combined automated total** | **352** | **352** | **0** | **0** |

All 18 frontend test files passed. The Vite production build also succeeded. The build is not counted as a test case. The separate six-test backend rerun and verbose 168-test frontend rerun are already included above and are not counted twice.

Backend API tests exercise multiple application layers and are classified as API/integration tests. Frontend tests include unit, component and simulated interaction checks, rather than exclusively unit tests.

### 3.1 Backend Feature Coverage

The expected result is that the assertions for the named feature pass. Suite success does not establish complete feature coverage.

| Test suite / feature | Expected result | Cases | Actual result |
| --- | --- | ---: | --- |
| AccountLinkApiTest | Tested rules and responses match suite assertions. | 11 | 11 passed; 0 failures, 0 errors, 0 skipped |
| AccountRoleApiTest | Tested rules and responses match suite assertions. | 2 | 2 passed; 0 failures, 0 errors, 0 skipped |
| AdminApiTest | Tested rules and responses match suite assertions. | 8 | 8 passed; 0 failures, 0 errors, 0 skipped |
| AdminCollectionRateApiTest | Tested rules and responses match suite assertions. | 1 | 1 passed; 0 failures, 0 errors, 0 skipped |
| AdminModerationApiTest | Tested rules and responses match suite assertions. | 6 | 6 passed; 0 failures, 0 errors, 0 skipped |
| AuthApiTest | Tested rules and responses match suite assertions. | 6 | 6 passed; 0 failures, 0 errors, 0 skipped |
| CollectionRateApiTest | Tested rules and responses match suite assertions. | 5 | 5 passed; 0 failures, 0 errors, 0 skipped |
| DistanceApiTest | Tested rules and responses match suite assertions. | 17 | 17 passed; 0 failures, 0 errors, 0 skipped |
| DonorHistoryApiTest | Tested rules and responses match suite assertions. | 4 | 4 passed; 0 failures, 0 errors, 0 skipped |
| ListingApiTest | Tested rules and responses match suite assertions. | 14 | 14 passed; 0 failures, 0 errors, 0 skipped |
| ListingDailyCapacityApiTest | Tested rules and responses match suite assertions. | 7 | 7 passed; 0 failures, 0 errors, 0 skipped |
| ListingDatesApiTest | Tested rules and responses match suite assertions. | 12 | 12 passed; 0 failures, 0 errors, 0 skipped |
| ListingDescriptionApiTest | Tested rules and responses match suite assertions. | 4 | 4 passed; 0 failures, 0 errors, 0 skipped |
| RecipientLocationApiTest | Tested rules and responses match suite assertions. | 10 | 10 passed; 0 failures, 0 errors, 0 skipped |
| ReportApiTest | Tested rules and responses match suite assertions. | 18 | 18 passed; 0 failures, 0 errors, 0 skipped |
| RequestApiTest | Tested rules and responses match suite assertions. | 18 | 18 passed; 0 failures, 0 errors, 0 skipped |
| ShumingFlowsApiTest | Tested rules and responses match suite assertions. | 25 | 25 passed; 0 failures, 0 errors, 0 skipped |
| SuburbFilterApiTest | Tested rules and responses match suite assertions. | 2 | 2 passed; 0 failures, 0 errors, 0 skipped |
| DistanceIntegrationTest | Tested rules and responses match suite assertions. | 7 | 7 passed; 0 failures, 0 errors, 0 skipped |
| GeocodingServiceTest | Tested rules and responses match suite assertions. | 6 | 6 passed; 0 failures, 0 errors, 0 skipped |
| WebappApplicationTests | Tested rules and responses match suite assertions. | 1 | 1 passed; 0 failures, 0 errors, 0 skipped |

### 3.2 Frontend Feature Coverage

Mocked API responses do not establish a live connection to the production backend.

| Test suite | Feature / expected behaviour | Cases | Actual result |
| --- | --- | ---: | --- |
| api/client.test.js | Request parameters, credentials, response and error handling behave as asserted. | 13 | 13 passed |
| api/donorListingAdapter.test.js | Collection rates, deadlines and expiry mapping behave as asserted. | 7 | 7 passed |
| api/donorReportAdapter.test.js | Report statuses and optional fields behave as asserted. | 5 | 5 passed |
| api/listingAdapter.test.js | Distance formatting and stock mapping behave as asserted. | 10 | 10 passed |
| api/mapbox.test.js | Address parsing, lookup configuration and error handling behave as asserted. | 7 | 7 passed |
| components/SuburbPicker.test.js | Suggestions, keyboard selection and fallback input behave as asserted. | 12 | 12 passed |
| views/admin/AccountManagement.test.js | Account collection-rate rendering behave as asserted. | 1 | 1 passed |
| views/admin/ManageListings.test.js | Listing rates across loading and filtering behave as asserted. | 1 | 1 passed |
| views/donor/DonationHistory.test.js | History outcomes, pagination and loading/error states behave as asserted. | 3 | 3 passed |
| views/donor/DonorHistoryIntegration.test.js | History API contract, filtering and retry behave as asserted. | 2 | 2 passed |
| views/donor/ListingDescription.test.js | Description limits during creation and editing behave as asserted. | 12 | 12 passed |
| views/donor/MyListings.test.js | Pagination, retry and editing validation behave as asserted. | 5 | 5 passed |
| views/donor/NewListing.test.js | Required timeslots for publishing and draft behaviour behave as asserted. | 4 | 4 passed |
| views/recipient/BrowseDonations.test.js | Locations, filters, distances and stock availability behave as asserted. | 11 | 11 passed |
| views/recipient/ReportIssue.test.js | Report submission, evidence validation and errors behave as asserted. | 6 | 6 passed |
| views/recipient/ReportList.test.js | Report loading, filters, pagination and retry behave as asserted. | 4 | 4 passed |
| AdminModeration.test.js | Moderation, account actions and failure handling behave as asserted. | 17 | 17 passed |
| ShumingFlows.test.js | Admin authentication, donor review, recipient requests and time boundaries behave as asserted. | 48 | 48 passed |

## 4. Manual Functional Testing

There are 33 recorded scenario entries: 12 in session one and 21 in session two. Listing publication was repeated, so these are not 33 distinct features. Approval and rejection remain one entry as in the original list.

Individual pass/fail outcomes were not consistently recorded. Outcome not recorded means neither Passed nor Failed. Automated results do not retrospectively establish the outcome of these manual sessions.

### 21 October 2026

| ID | Test case | Expected result / check | Actual result |
| --- | --- | --- | --- |
| M01 | Donor registration | A valid donor account receives the selected role. | Passed |
| M02 | Recipient registration | A valid recipient account receives the selected role. | Passed |
| M03 | Donor login | Valid credentials provide donor access. | Passed |
| M04 | Recipient login | Valid credentials provide recipient access. | Passed |
| M05 | Donor listing publication | Valid listings publish and required inputs are checked. | Missing-timeslot issue recorded. |
| M06 | Recipient listing browsing | Available listings and details can be viewed. | Passed |
| M07 | Recipient pickup request | Requests follow timeslot and stock rules. | Timeslot and over-allocation issues recorded. |
| M08 | Donor approval and rejection | Request status matches the donor action. | Passed |
| M09 | Donor history browsing | Relevant donation records are displayed. | Passed |
| M10 | Recipient history browsing | Relevant collection records are displayed. | Passed |
| M11 | Dual-role registration | Both selected roles are assigned. | Passed |
| M12 | Dual-role login | Authentication supports permitted roles. | Passed |

### 22 October 2026

| ID | Test case | Expected result / check | Actual result |
| --- | --- | --- | --- |
| M13 | Donor listing publication | Quantity, dates, limits and descriptions follow agreed rules. | Input-validation and error-message issues recorded. |
| M14 | Administrator login | Valid credentials provide administrator access. | Passed |
| M15 | Administrator bans/removes a listing | Listing availability reflects the moderation action. | Passed |
| M16 | Recipient reports a donor | An eligible report is recorded. | Passed |
| M17 | Donor reports a recipient | An eligible report is recorded. | Passed |
| M18 | Administrator bans a donor | The intended restriction applies. | Passed |
| M19 | Administrator bans a recipient | The intended restriction applies. | Passed |
| M20 | Administrator reactivates a donor | Access is restored under reactivation rules. | Passed |
| M21 | Administrator reactivates a recipient | Access is restored under reactivation rules. | Passed |
| M22 | Dual-role account publishes as donor | Authorised donor publication is supported. | Passed |
| M23 | Dual-role account requests as recipient | An authorised recipient request is supported. | Passed |
| M24 | Dual-role account requests its own listing | Follow the agreed self-request rule; rule requires confirmation. | Passed |
| M25 | Dual-role account confirms its own collection | Follow agreed ownership and completion rules. | Passed |
| M26 | Dual-role account reports itself as donor | Follow the agreed self-reporting rule; rule requires confirmation. | Passed |
| M27 | Dual-role account reports itself as recipient | Follow the agreed self-reporting rule; rule requires confirmation. | Passed |
| M28 | Administrator bans dual-role donor account | Apply the intended account or role restriction. | Passed |
| M29 | Recipient login after donor deactivation | Follow agreed account-wide or role-specific restrictions. | Passed |
| M30 | Administrator bans dual-role recipient account | Apply the intended account or role restriction. | Passed |
| M31 | Donor login following deactivation | Original wording says after donor deactivation; whether recipient was intended requires clarification. | Passed |
| M32 | Administrator deactivates account through user management | Apply intended deactivation rules. | Passed |
| M33 | Administrator issues account warning | Record warning under the moderation workflow. | Passed |

## 5. Reported Issues and Follow-up

Seven issues were recorded: missing pickup timeslots, same-day timeslot availability, over-allocation, decimal quantities, invalid dates, daily limits exceeding listing quantity, and long descriptions displaying an Unauthorised error.
The development team subsequently confirmed that the reported bugs had been fixed. The passing automated suite includes related regression checks, supporting evidence of the fixes is documented in the Bug Reports.


## 6. Execution Commands

```powershell
# Run from backend/webapp
.\mvnw.cmd -B clean test
.\mvnw.cmd -B "-Dtest=GeocodingServiceTest" test

# Run from frontend
npm.cmd ci
npm.cmd test
npm.cmd test -- --reporter=verbose
npm.cmd run build
```

## 7. Evidence

Results were verified against these supplied records:

- backend-test-log.txt
- backend-test-summary.csv
- backend-unit-test-log.txt
- frontend-test-log.txt
- frontend-test-details.txt
- frontend-build-log.txt

Evidence filenames identify the reviewed records; they do not imply that logs have been committed. Dependency-installation output was not supplied.

## 8. Limitations

H2-backed tests do not establish production MySQL behaviour under all workloads. jsdom and mocked responses do not replace live-browser verification. Results do not establish production load performance, live Mapbox availability, broad browser compatibility, accessibility conformance or formal client acceptance.
