const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080").replace(/\/$/, "");
let refreshPromise = null;

export class ApiError extends Error {
  constructor(message, status, responseBody) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.responseBody = responseBody;
  }
}

export function getApiUrl(path) {
  return `${API_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`;
}

export function getWebSocketUrl(path) {
  return getApiUrl(path).replace(/^http:/, "ws:").replace(/^https:/, "wss:");
}

export function getAccessToken() {
  return sessionStorage.getItem("token");
}

export function getRefreshToken() {
  return sessionStorage.getItem("refreshToken");
}

export function getAuthHeaders(headers = {}, tokenOverride = null) {
  const requestHeaders = new Headers(headers);
  const token = tokenOverride || getAccessToken();

  if (token && (tokenOverride || !requestHeaders.has("Authorization"))) {
    requestHeaders.set("Authorization", `Bearer ${token}`);
  }

  return requestHeaders;
}

async function refreshAccessToken() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    return null;
  }

  if (!refreshPromise) {
    refreshPromise = fetch(getApiUrl("/auth/refresh"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    })
      .then(async (response) => {
        if (!response.ok) {
          return null;
        }

        const data = await response.json();
        if (!data.accessToken) {
          return null;
        }

        sessionStorage.setItem("token", data.accessToken);
        return data.accessToken;
      })
      .catch(() => null)
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

export async function getValidAccessToken() {
  const token = getAccessToken();
  if (!token) {
    return null;
  }

  try {
    const payload = decodeJwtPayload(token);
    if (payload.exp && payload.exp * 1000 > Date.now() + 30_000) {
      return token;
    }
  } catch {
    // Attempt a refresh below when the stored token cannot be decoded.
  }

  return refreshAccessToken();
}

function shouldAttemptRefresh(path, response) {
  if (response.status !== 401) {
    return false;
  }

  const normalizedPath = path.split("?")[0];
  return !["/auth/login", "/auth/refresh", "/auth/logout"].includes(normalizedPath);
}

function redirectToLoginAfterAuthFailure() {
  clearAuthSession();
  if (typeof window !== "undefined" && window.location.pathname !== "/login") {
    window.location.assign("/login");
  }
}

export async function apiFetch(path, options = {}) {
  const requestHeaders = getAuthHeaders(options.headers);

  if (options.body && !requestHeaders.has("Content-Type")) {
    requestHeaders.set("Content-Type", "application/json");
  }

  const requestOptions = {
    ...options,
    headers: requestHeaders,
  };
  const response = await fetch(getApiUrl(path), requestOptions);

  if (!shouldAttemptRefresh(path, response) || !getRefreshToken()) {
    return response;
  }

  const accessToken = await refreshAccessToken();
  if (!accessToken) {
    redirectToLoginAfterAuthFailure();
    return response;
  }

  return fetch(getApiUrl(path), {
    ...requestOptions,
    headers: getAuthHeaders(options.headers, accessToken),
  });
}

export async function apiJson(path, options = {}) {
  const response = await apiFetch(path, options);
  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const message = typeof body === "object"
      ? body.message || body.error || "Request failed"
      : body || "Request failed";
    throw new ApiError(message, response.status, body);
  }

  return body;
}

export function saveAuthSession({ accessToken, refreshToken, user }) {
  sessionStorage.setItem("token", accessToken);
  if (refreshToken) {
    sessionStorage.setItem("refreshToken", refreshToken);
  }
  sessionStorage.setItem("loggedInUserName", user.name);
  sessionStorage.setItem("loggedInUserRole", user.role);
  sessionStorage.setItem("loggedInUserId", String(user.id));
}

export function clearAuthSession() {
  [
    "token",
    "jwtToken",
    "refreshToken",
    "loggedInUserName",
    "loggedInUserRole",
    "loggedInUserId",
  ].forEach((key) => sessionStorage.removeItem(key));
}

export function decodeJwtPayload(token) {
  const encodedPayload = token?.split(".")[1];
  if (!encodedPayload) {
    throw new Error("Invalid access token");
  }

  const base64 = encodedPayload.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
  return JSON.parse(atob(padded));
}
