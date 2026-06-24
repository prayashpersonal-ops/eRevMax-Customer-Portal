import axios from "axios";

const API_BASE_URL = "http://localhost:8080";
const INACTIVITY_LIMIT = 2 * 60 * 60 * 1000

const API = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
});

const publicPaths = [
  "/user/login",
  "/user/register",
  "/user/refresh",
  "/admin/login",
  "/admin/refresh",
];

function isPublicPath(url = "") {
  return publicPaths.some((path) => url.includes(path));
}

export function saveAuth(role, tokenResponse) {
  if (!tokenResponse?.accessToken) return;

  localStorage.setItem(`${role}Token`, tokenResponse.accessToken);

  if (tokenResponse.refreshToken) {
    localStorage.setItem(`${role}RefreshToken`, tokenResponse.refreshToken);
  }

  localStorage.setItem("activeRole", role);
  localStorage.setItem("lastActivityAt", String(Date.now()));
}

export function getToken(role) {
  return localStorage.getItem(`${role}Token`) || sessionStorage.getItem(`${role}Token`);
}

export function getRefreshToken(role) {
  return (
    localStorage.getItem(`${role}RefreshToken`) ||
    sessionStorage.getItem(`${role}RefreshToken`)
  );
}

export function clearAuth(role) {
  if (!role || role === "all") {
    ["admin", "user"].forEach(clearAuth);
    localStorage.removeItem("activeRole");
    localStorage.removeItem("lastActivityAt");
    return;
  }

  localStorage.removeItem(`${role}Token`);
  localStorage.removeItem(`${role}RefreshToken`);
  sessionStorage.removeItem(`${role}Token`);
  sessionStorage.removeItem(`${role}RefreshToken`);

  if (localStorage.getItem("activeRole") === role) {
    localStorage.removeItem("activeRole");
  }
}

export function isLoggedIn(role) {
  return Boolean(getToken(role));
}

export function isInactive() {
  const tokenExists = getToken("admin") || getToken("user");
  if (!tokenExists) return false;

  const lastActivityAt = Number(localStorage.getItem("lastActivityAt"));
  if (!lastActivityAt) return false;

  return Date.now() - lastActivityAt > INACTIVITY_LIMIT;
}

export function touchActivity() {
  if (getToken("admin") || getToken("user")) {
    localStorage.setItem("lastActivityAt", String(Date.now()));
  }
}

export function startInactivityTracker(onExpired) {
  const events = ["click", "keydown", "mousemove", "touchstart", "scroll"];

  function handleActivity() {
    if (isInactive()) {
      clearAuth("all");
      if (onExpired) onExpired();
      return;
    }

    touchActivity();
  }

  events.forEach((eventName) => {
    window.addEventListener(eventName, handleActivity, { passive: true });
  });

  const intervalId = window.setInterval(() => {
    if (isInactive()) {
      clearAuth("all");
      if (onExpired) onExpired();
    }
  }, 60 * 1000);

  return function cleanup() {
    events.forEach((eventName) => {
      window.removeEventListener(eventName, handleActivity);
    });
    window.clearInterval(intervalId);
  };
}

function getRoleForRequest(config) {
  if (config.authRole) return config.authRole;

  const url = config.url || "";
  const activeRole = localStorage.getItem("activeRole");

  if (url.startsWith("/admin")) return "admin";
  if (url.startsWith("/user")) return "user";

  if (url.startsWith("/hotels/addHotel")) return "admin";
  if (url.startsWith("/hotels/seeAll")) return "admin";
  if (url.startsWith("/hotels/roomRateMappingPlan")) return "user";

  return activeRole || (getToken("admin") ? "admin" : "user");
}

async function refreshAccessToken(role) {
  const refreshToken = getRefreshToken(role);
  if (!refreshToken) throw new Error("Refresh token missing");

  const response = await axios.post(
    `${API_BASE_URL}/${role}/refresh`,
    { refreshToken },
    { withCredentials: true }
  );

  if (!response.data?.accessToken) {
    throw new Error(response.data?.message || "Refresh failed");
  }

  saveAuth(role, response.data);
  return response.data.accessToken;
}

API.interceptors.request.use((config) => {
  if (isInactive() && !isPublicPath(config.url)) {
    clearAuth("all");
    window.location.href = "/";
    throw new axios.Cancel("Session expired due to inactivity");
  }

  if (!isPublicPath(config.url)) {
    const role = getRoleForRequest(config);
    const token = getToken(role);

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }

  return config;
});

API.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (!originalRequest || originalRequest._retry) {
      return Promise.reject(error);
    }

    const status = error.response?.status;
    const shouldTryRefresh = status === 401 || status === 403;

    if (!shouldTryRefresh || isPublicPath(originalRequest.url)) {
      return Promise.reject(error);
    }

    const role = getRoleForRequest(originalRequest);

    try {
      originalRequest._retry = true;
      const newAccessToken = await refreshAccessToken(role);
      originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
      return API(originalRequest);
    } catch (refreshError) {
      clearAuth(role);
      window.location.href = role === "admin" ? "/admin-login" : "/";
      return Promise.reject(refreshError);
    }
  }
);

export default API;
