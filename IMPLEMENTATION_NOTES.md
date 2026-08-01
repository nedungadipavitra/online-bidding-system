# Online Bidding Platform — Implementation Notes

This document summarizes the changes made during the architecture, reliability, security, realtime bidding, and Admin Dashboard review.

## Main outcomes

- Frontend and backend communication is routed through the API gateway.
- Authentication uses shared JWT configuration and refresh-token support.
- Buyer dashboards and product pages receive the highest bid through STOMP WebSocket updates without refreshing.
- Admin category management is persisted in MySQL instead of browser `localStorage`.
- Product, user, category, wallet, bid, and order workflows have clearer backend ownership and authorization checks.
- Local backend startup is documented and automated through `start-backend.ps1`.

## Frontend changes

### Centralized API communication

- Added `frontend/src/api/client.js`.
- Added centralized API URL handling through `VITE_API_BASE_URL`.
- Added automatic `Authorization: Bearer ...` headers.
- Added access-token refresh and session cleanup when authentication fails.
- Added common JSON/API error handling.
- Added request caching helpers in `frontend/src/api/resources.js` to avoid repeated product, user, and highest-bid requests.

### Authentication and routing

- Login now uses the gateway API and stores access-token, refresh-token, user ID, name, and role consistently.
- Protected routes enforce the expected user role.
- Application routes use lazy-loaded page components.

### Realtime bidding

- Reworked `frontend/src/utils/websocket.js` to use STOMP over the gateway WebSocket endpoint `/ws`.
- Refreshes the JWT before the initial connection and every reconnect.
- Subscribes to `/topic/auction/{auctionId}`.
- Product descriptions update the current bid immediately when another buyer places a higher bid.
- Buyer dashboard cards update live as well.
- Lower or stale bid messages are ignored.
- The successful bid response updates the submitting buyer's page immediately.

### Auction state and product UI

- Added `frontend/src/utils/auctionStatus.js` to calculate UPCOMING, ACTIVE, ENDED, and terminal auction states consistently.
- Buyers only see auctions that are currently open for bidding.
- Admin product editing uses a category select input.
- Admin product editing does not expose or submit the current bid price.
- Seller product views load current highest bids from the bid service.
- Seller product creation now loads categories from `/categories`, ensuring it sees categories created by an administrator.

### Admin Dashboard

- The Admin Dashboard was split into reusable stats, tabs, forms, and data-table components.
- Category CRUD now calls the backend:
  - `POST /categories`
  - `PUT /categories/{id}`
  - `DELETE /categories/{id}`
- Category success messages are shown only after the server confirms the operation.
- User CRUD calls the user service through the gateway.
- Product CRUD calls the product service through the gateway.
- Bids and orders are displayed as read-only administrative views.

## Backend changes

### API gateway

- Gateway routes support environment-based service URLs.
- Gateway JWT validation extracts user ID, role, and email from the verified token.
- Client-supplied identity headers are removed before trusted identity headers are added.
- Client-supplied internal wallet credentials are removed.
- Public endpoints include authentication, product/category reads, and the initial WebSocket/SockJS handshake.
- Native WebSocket `/ws` and SockJS HTTP requests such as `/ws/info` use separate routes.
- Gateway CORS settings are configurable through `GATEWAY_ALLOWED_ORIGINS`.

### Bid service

- Added product snapshot validation before accepting bids.
- Bid placement validates auction status, start/end times, seller ownership, and strictly increasing bid amount.
- Wallet settlement is handled by the server through an internal wallet token instead of multiple browser-side wallet requests.
- Added highest-bid lookup for multiple auctions: `GET /bids/highest?auctionIds=...`.
- HTTP bid placement broadcasts the accepted bid to all subscribers.
- WebSocket bid placement also broadcasts accepted bids.
- WebSocket authentication validates the JWT on STOMP `CONNECT`.
- Authenticated principals are retained by WebSocket session and restored for later `SUBSCRIBE`, `SEND`, and other frames.
- WebSocket session principals are cleaned up on disconnect.

### Product and category services

- Product updates preserve the server-owned current highest bid.
- Product update/delete authorization supports administrators and the owning seller.
- Product creation uses the authenticated seller ID from the gateway instead of trusting a browser-supplied seller ID.
- Category create/update/delete endpoints require the ADMIN role.
- Category persistence uses the JPA repository and MySQL database.

### User, wallet, and order services

- User and order controllers validate role and ownership for administrative and workflow operations.
- Wallet operations include trusted internal settlement/provisioning paths.
- Order status transitions and delivery assignment are validated on the server.
- Service configuration now supports environment variables for database credentials, JWT secrets, service URLs, and internal tokens.

## Configuration and startup

- Added root `.env.example`.
- Root `.env` remains ignored by Git and must never be committed.
- Services use environment variables such as:
  - `DB_HOST`
  - `DB_PORT`
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `JWT_SECRET`
  - `WALLET_INTERNAL_TOKEN`
  - service URL overrides
- Added `start-backend.ps1` to load the root `.env` and launch all backend services plus the gateway.
- Updated project documentation with local startup instructions.

## Testing and verification

The following frontend checks pass:

```powershell
cd frontend
npm.cmd run lint
npm.cmd run build
```

Backend tests should be run from each Maven service after Maven is correctly installed or configured:

```powershell
backend\user-service\mvnw.cmd test
backend\product-service\mvnw.cmd test
backend\wallet-service\mvnw.cmd test
backend\bid-service\mvnw.cmd test
backend\order-service\mvnw.cmd test
backend\api-gateway\mvnw.cmd test
```

The repository Maven wrapper was not usable in the review environment, so backend Maven tests must be confirmed by the team on a working Maven installation.

## Important operational notes

1. Start MySQL before starting the backend services.
2. Confirm the root `.env` contains the correct database password and shared JWT secret.
3. Restart both `bid-service` and `api-gateway` after WebSocket or routing changes.
4. Restart Vite or perform a hard browser refresh after frontend changes.
5. A successful realtime connection should show `CONNECTED` followed by successful `SUBSCRIBE` frames, without an authentication error.
6. Never commit `.env`, passwords, JWT secrets, wallet tokens, `node_modules`, `target`, or `.pnpm-store`.

## Suggested follow-up work

- Run the full backend Maven test suite in CI.
- Add integration tests covering category persistence through the gateway.
- Add end-to-end tests for two simultaneous buyers receiving the same bid update.
- Replace remaining legacy local-storage helpers after confirming their unused screens can be removed safely.
- Add pagination and server-side filtering for large Admin Dashboard tables.
