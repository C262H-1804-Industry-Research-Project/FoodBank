# Changelog

## Donor listings integration

- Connected the donor **My listings** page to `GET /api/donors/{donorId}/listings`, replacing the client-side sample listings.
- Added a donor-listings API client method and mapped the response into the existing table fields without changing the page's frontend components or layout.
- Used the authenticated donor ID for both loading and creating listings; removed the hard-coded donor ID (`1`) from the new-listing flow.
- Added `donorId` to the donor login response so the frontend can address the correct donor resource.
- Extended the donor-listing response with suburb, region, and timeslot maximum capacity. The existing booking display now derives booked places from maximum capacity minus remaining capacity.
- Documented the GET listings endpoint and related response schemas in `api/openapi.yaml`.

## Validation

- `npm run build` completed successfully for the frontend.
- `mvn clean compile` completed successfully for the backend.
- Maven tests were not run because the sandbox cannot write required Surefire dependencies to the local Maven cache.
