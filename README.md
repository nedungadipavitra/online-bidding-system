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
- Axios
- STOMP WebSocket

### Backend

- Spring Boot
- Spring Security
- Spring Data JPA
- Spring Cloud Gateway
- REST APIs
- WebSocket (STOMP)
- JWT Authentication

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
