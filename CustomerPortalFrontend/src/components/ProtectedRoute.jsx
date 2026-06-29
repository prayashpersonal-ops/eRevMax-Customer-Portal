import { Navigate, useLocation } from "react-router-dom";
import { clearAuth, getToken, isInactive, saveAuth } from "../services/api";

function decodeJwtPayload(token) {
  const payloadBase64Url = token.split(".")[1];
  if (!payloadBase64Url) return null;

  const payloadBase64 = payloadBase64Url.replace(/-/g, "+").replace(/_/g, "/");
  return JSON.parse(atob(payloadBase64));
}

function getTokenFromUrl(search) {
  const params = new URLSearchParams(search);
  return params.get("token") || params.get("accessToken");
}

function cleanTokenFromUrl(search, pathname) {
  const params = new URLSearchParams(search);
  params.delete("token");
  params.delete("accessToken");
  params.delete("refreshToken");

  const queryString = params.toString();
  const cleanUrl = queryString ? `${pathname}?${queryString}` : pathname;
  window.history.replaceState({}, document.title, cleanUrl);
} 

function ProtectedRoute({ children, tokenKey }) {
  const location = useLocation();
  const role = tokenKey === "adminToken" ? "admin" : "user";
  let token = getToken(role);

  if (role === "user") {
    const urlToken = getTokenFromUrl(location.search);

    if (urlToken) {
      saveAuth("user", {
        accessToken: urlToken,
        success: true,
      });

      token = urlToken;
      cleanTokenFromUrl(location.search, location.pathname);

      try {
        const decodedPayload = decodeJwtPayload(urlToken);
        if (decodedPayload?.enabled === false) {
          return <Navigate to="/pending" replace />;
        }
      } catch {
        return <Navigate to="/" replace />;
      }
    }
  }

  if (!token || isInactive()) {
    clearAuth(role);
    return <Navigate to={role === "admin" ? "/admin-login" : "/"} replace />;
  }

  return children;
}

export default ProtectedRoute;
