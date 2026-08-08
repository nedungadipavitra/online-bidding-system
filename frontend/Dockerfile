# -----------------------------------------------------------------------------
# frontend — multi-stage production image (React + Vite → nginx static)
# Port: 80. Health: GET /health
# Build arg VITE_API_BASE_URL is baked into the SPA at build time.
# -----------------------------------------------------------------------------

# ---- Build stage: Vite production bundle ----
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Default points at local gateway; Jenkins overrides for prod (edge EIP :8080).
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# ---- Runtime stage: nginx serves dist/ ----
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/health >/dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]
