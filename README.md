# Online Bidding System

A cloud-ready, real-time online auction platform built with **Spring Boot microservices**, **React (Vite)**, **MySQL / AWS RDS**, **Docker**, **AWS (EC2, ECR, S3, Parameter Store)**, and **Jenkins CI/CD**.

The system supports secure auctions with live bidding, role-based access, wallet management, delivery assignment, and production deployment on AWS.

---

## Features

- JWT authentication and authorization (access + refresh tokens)
- Role-based access: Admin, Seller, Buyer, Delivery Partner
- Real-time live bidding over WebSockets (STOMP via API gateway `/ws`)
- Product and category management
- Wallet management and trusted internal settlement
- Order management and delivery assignment
- AWS S3 product images
- MySQL locally / AWS RDS in production
- Dockerized services with per-service Jenkins pipelines
- AWS Parameter Store for production secrets and service URLs

---

## Repository structure

```
online-bidding-system-monorepo/
├── frontend/                 # React + Vite SPA
├── backend/
│   ├── api-gateway/          # Spring Cloud Gateway (:8080)
│   ├── user-service/         # Auth + users (:8081)
│   ├── product-service/      # Products + categories (:8082)
│   ├── wallet-service/       # Wallets + transactions (:8083)
│   ├── order-service/        # Orders (:8084)
│   └── bid-service/          # Bids + WebSocket (:8085)
├── ci/jenkins/               # Optional Jenkins Docker Compose on EC2
├── jenkins/                  # Shared pipeline groovy helpers
├── scripts/                  # deploy.sh / rollback.sh used on EC2
├── start-backend.ps1         # Local: start all backend services
├── Jenkinsfile               # Root pipeline reference (services use their own)
├── .env.example              # Local env template (copy to .env — never commit .env)
├── README.md                 # This file — run guide + structure
├── architecture.md           # Who / what / where / how / why + diagrams
└── IMPLEMENTATION_NOTES.md   # Feature / architecture change notes
```

Browser traffic in production:

- SPA → `http://<edge-eip>/` (port **80**)
- API / WebSocket → `http://<edge-eip>:8080` (API gateway)

---

## Technology stack

### Frontend

- React 19 + Vite
- React Router, Bootstrap / React-Bootstrap
- Centralized API client (`frontend/src/api/client.js`) via `VITE_API_BASE_URL`
- STOMP WebSocket (`@stomp/stompjs`, SockJS) through the gateway
- `react-toastify` for success/error feedback

### Backend

- Java / Spring Boot
- Spring Security + JWT
- Spring Data JPA
- Spring Cloud Gateway
- REST APIs
- WebSocket (STOMP) on bid-service, exposed through the gateway

### Data and cloud

- MySQL (local) / AWS RDS (prod)
- AWS EC2, ECR, S3, Systems Manager Parameter Store
- Docker on EC2 hosts

### DevOps

- Jenkins (per-service `Jenkinsfile` under `backend/<service>/` and `frontend/`)
- Optional Jenkins host setup under `ci/jenkins/`

---

## Prerequisites (local)

- JDK 17+ and Maven on `PATH` (or `MAVEN_HOME`)
- Node.js 20+ and npm
- MySQL 8 with databases/schemas expected by each service
- Git

---

## How to run locally

### 1. Environment

```powershell
copy .env.example .env
```

Edit `.env` and set at least:

| Variable | Purpose |
|----------|---------|
| `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD` | MySQL |
| `JWT_SECRET` | Base64 key, ≥ 32 bytes, shared by gateway + user-service |
| `WALLET_INTERNAL_TOKEN` | Trusted service-to-service wallet calls |
| `GATEWAY_ALLOWED_ORIGINS` | Browser origins (default Vite: `http://localhost:5173`) |

Do **not** commit `.env`.

### 2. Start backend (all services)

From the repo root:

```powershell
.\start-backend.ps1
```

If PowerShell blocks scripts:

```powershell
powershell -ExecutionPolicy Bypass -File .\start-backend.ps1
```

The script loads `.env`, opens each backend service in its own window, and starts the API gateway after a short delay.

Default ports:

| Service | Port |
|---------|------|
| api-gateway | 8080 |
| user-service | 8081 |
| product-service | 8082 |
| wallet-service | 8083 |
| order-service | 8084 |
| bid-service | 8085 |

All browser/API calls in local and prod should go through the **gateway** (`http://localhost:8080`), not directly to service ports.

### 3. Start frontend

```powershell
cd frontend
npm install
npm run dev
```

Open the URL Vite prints (usually `http://localhost:5173`).

Optional: set `VITE_API_BASE_URL` if the gateway is not at `http://localhost:8080`.

Vite is configured to listen on the LAN (`host: true`) so other devices on the same Wi‑Fi can open the app.

### 4. Stop backend

```powershell
8080..8085 | % { Get-NetTCPConnection -LocalPort $_ -EA SilentlyContinue | Select -Expand OwningProcess -Unique | % { Stop-Process -Id $_ -Force } }
```

Then close the leftover PowerShell windows if needed.

---

## Recent tech / project changes (high level)

- All browser traffic goes through the **API gateway** (REST + WebSocket `/ws`)
- Central frontend API client + JWT refresh (`VITE_API_BASE_URL`)
- Real-time highest bid updates over STOMP
- Admin category/product/user CRUD persisted via backend APIs
- Local backend startup via `start-backend.ps1` + root `.env`
- AWS deploy: Jenkins → ECR → EC2, prod profile + Parameter Store

Deploy architecture (hosts, CI/CD, CORS/IST fixes): [`architecture.md`](architecture.md).

---

## Production (AWS) overview

Demo edge host (subject to change): Elastic IP **`3.6.164.251`**

| Surface | URL |
|---------|-----|
| Frontend (nginx) | `http://3.6.164.251/` |
| API gateway | `http://3.6.164.251:8080` |

| Host role | Typical containers | Ports |
|-----------|--------------------|-------|
| obs-edge | api-gateway + frontend | 8080, 80 |
| obs-identity | user-service + wallet-service | 8081, 8083 |
| obs-catalog | product-service | 8082 |
| obs-fulfillment | order-service + bid-service | 8084, 8085 |

CI/CD pattern per service:

1. Jenkins builds from the service `Jenkinsfile`
2. Image pushed to ECR (`obs/<service>`)
3. SSH deploy to the target EC2 (`EC2_HOST`)

Production uses `SPRING_PROFILES_ACTIVE=prod` and Parameter Store (`/obs/prod/`, `/obs/gateway/`, `/obs/user/`, …).

More: [`architecture.md`](architecture.md), [`frontend/README.md`](frontend/README.md).

---

## Team documentation

| Doc | What it covers |
|-----|----------------|
| [`README.md`](README.md) (this file) | Folder structure, stack, how to run, short tech/AWS overview |
| [`architecture.md`](architecture.md) | Who / what / where / how / why + Mermaid diagrams (`aws-deploy-changes`) |
| [`IMPLEMENTATION_NOTES.md`](IMPLEMENTATION_NOTES.md) | Feature/architecture changes (API client, realtime bids, admin CRUD) |
| [`frontend/README.md`](frontend/README.md) | Frontend env, local Vite, prod edge / Jenkins notes |

---

## Team

- Ayush Sarbariya
- Tejas Bayaskar
- Pavitra Nedungadi

---

## Future enhancements

- AI price recommendation / chatbot
- Fraud detection and smarter search
- Analytics dashboard
- Payment gateway integration

---

Developed as part of CDAC PG-DAC Major Project.
