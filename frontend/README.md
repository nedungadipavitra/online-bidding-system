# Frontend (React + Vite)

SPA for the Online Bidding System. All HTTP and WebSocket traffic goes through the **API gateway**.

## Local development

```powershell
cd frontend
npm install
npm run dev
```

- App: usually `http://localhost:5173`
- API default: `http://localhost:8080` (override with `VITE_API_BASE_URL`)
- Dev server listens on the LAN (`host: true` in `vite.config.js`) so phones/other PCs on the same Wi‑Fi can open the app

```powershell
# optional
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run dev
```

## Application configuration

| Variable | When | Meaning |
|----------|------|---------|
| `VITE_API_BASE_URL` | Build / runtime (Vite) | Gateway base URL; baked in at **build** time for Docker/prod |

Central client: `src/api/client.js`.

## Production (AWS)

| Item | Value |
|------|--------|
| Image | `frontend/Dockerfile` — Vite build → nginx |
| Health | `GET /health` |
| Host | obs-edge Elastic IP (demo: `3.6.164.251`) |
| Port | **80** (SPA); API remains gateway **:8080** |
| CI | Jenkins job `obs-frontend`, Script Path `frontend/Jenkinsfile` |
| ECR | `obs/frontend` |
| Build arg | `VITE_API_BASE_URL` (demo: `http://3.6.164.251:8080`) |

Prod URL: `http://3.6.164.251/`.

## Scripts

| Command | Purpose |
|---------|---------|
| `npm run dev` | Vite dev server |
| `npm run build` | Production bundle → `dist/` |
| `npm run preview` | Preview the production build locally |
| `npm run lint` | ESLint |

## Related docs

- Root [`README.md`](../README.md) — full monorepo run guide and folder structure
- [`IMPLEMENTATION_NOTES.md`](../IMPLEMENTATION_NOTES.md) — realtime bidding, admin CRUD, API client notes
