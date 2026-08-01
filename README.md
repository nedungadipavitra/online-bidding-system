# Online Bidding System

A cloud-ready, real-time online auction platform built using **Spring Boot Microservices**, **React.js**, **MySQL**, **Docker**, **Kubernetes**, **AWS**, and **Jenkins**. The system enables secure online auctions with live bidding, role-based access control, wallet management, delivery assignment, and cloud-native deployment.

---

## Features

- JWT Authentication & Authorization
- Role-Based Access (Admin, Seller, Buyer, Delivery Partner)
- Real-Time Live Bidding using WebSockets
- Product & Category Management
- Wallet Management
- Order Management
- Delivery Assignment Workflow
- AWS S3 Image Storage
- MySQL / AWS RDS Integration
- Dockerized Microservices
- Kubernetes Deployment
- Jenkins CI/CD Pipeline
- Atlassian Jira for Project Management
- AI-Ready Architecture (Python Microservice)

---

##  Repository Structure

```
online-bidding-system
│
├── frontend/
│
├── backend/
│   ├── api-gateway
│   ├── user-service
│   ├── product-service
│   ├── wallet-service
│   ├── bid-service
│   └── order-service
│
├── kubernetes/
├── docs/
├── docker-compose.yml
├── README.md
```

---

## Technology Stack

### Frontend

- React.js
- Vite
- Bootstrap
- Fetch API through the centralized frontend API client
- STOMP WebSocket

### Backend

- Spring Boot
- Spring Security
- Spring Data JPA
- Spring Cloud Gateway
- REST APIs
- WebSocket (STOMP)
- JWT Authentication

### Runtime configuration and secrets

Backend services read credentials and security settings from environment variables. Copy [.env.example](.env.example) to `.env` for local reference, or export the variables directly before starting the services. The copied `.env` file is ignored by Git.

Required variables:

- `DB_PASSWORD` — MySQL password used by all services.
- `JWT_SECRET` — Base64-encoded signing key containing at least 32 bytes, shared by the gateway and user service.
- `WALLET_INTERNAL_TOKEN` — private token used only for trusted wallet settlement and wallet provisioning calls.

Service URLs and `GATEWAY_ALLOWED_ORIGINS` can be overridden for deployment environments; local localhost defaults remain available for non-secret settings. Do not use the example placeholder values in production, and restrict direct access to internal service ports.

### Local backend startup

After creating and filling in the root `.env`, start all backend services from PowerShell with:

```powershell
.\start-backend.ps1
```

The script loads `.env`, opens each backend service in a separate PowerShell window, and starts the API gateway after a short delay. If PowerShell blocks local scripts, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\start-backend.ps1
```

The script uses Maven from `PATH`, `MAVEN_HOME`, or the local Maven distribution previously used for this repository.

### Database

- MySQL
- AWS RDS

### Cloud

- AWS EC2
- AWS S3
- AWS RDS

### DevOps

- Docker
- Kubernetes
- Jenkins
- GitHub Actions (optional)

### Project Management

- Atlassian Jira

### AI Integration

- Python FastAPI
- Google Gemini / OpenAI

---

##  Team

- Ayush Sarbariya
- Tejas Bayaskar
- Pavitra Nedungadi

---

##  Documentation

- Backend Documentation → `backend/README.md`
- Frontend Documentation -> `frontend/README.md`
- Implementation notes -> `IMPLEMENTATION_NOTES.md`

---

##  Future Enhancements
- AI Price Recommendation
- AI Chatbot
- Fraud Detection
- Smart Product Search
- Analytics Dashboard
- Payment Gateway Integration
---

##  Highlights

- Microservices Architecture
- Cloud Native Design
- Event Driven Communication
- Real-Time WebSocket Updates
- Secure JWT Authentication
- CI/CD Automation
- Production Ready Deployment

---

Developed as part of CDAC PG-DAC Major Project.
