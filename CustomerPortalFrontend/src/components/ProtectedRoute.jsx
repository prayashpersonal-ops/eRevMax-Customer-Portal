import { Navigate } from "react-router-dom";
import { clearAuth, getToken, isInactive } from "../services/api";

function ProtectedRoute({ children, tokenKey }) {
  const role = tokenKey === "adminToken" ? "admin" : "user";
  const token = getToken(role);

  if (!token || isInactive()) {
    clearAuth(role);
    return <Navigate to={role === "admin" ? "/admin-login" : "/"} replace />;
  }

  return children;
}

export default ProtectedRoute;
