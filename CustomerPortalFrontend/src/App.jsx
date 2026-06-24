import { useEffect } from "react";
import { BrowserRouter, Routes, Route, useNavigate } from "react-router-dom";
import ProtectedRoute from "./components/ProtectedRoute";
import UserAuthPage from "./pages/auth/UserAuthPage";
import AdminLogin from "./pages/auth/AdminLogin";
import AdminDashboard from "./pages/admin/AdminDashboard";
import AdminAllUsers from "./pages/admin/AdminAllUsers";
import UserDashboard from "./pages/user/UserDashboard";
import PendingApproval from "./pages/auth/PendingApproval";
import AdminUserDetail from "./pages/admin/AdminUserDetail";
import OAuthCallback from "./pages/auth/OAuthCallback";
import { startInactivityTracker } from "./services/api";

function AppRoutes() {
  const navigate = useNavigate();

  useEffect(() => {
    return startInactivityTracker(() => {
      navigate("/");
    });
  }, [navigate]);

  return (
    <Routes>
      <Route path="/" element={<UserAuthPage />} />
      <Route path="/auth/callback" element={<OAuthCallback />} />
      <Route path="/admin-login" element={<AdminLogin />} />
      <Route path="/pending" element={<PendingApproval />} />

      <Route
        path="/admin"
        element={
          <ProtectedRoute tokenKey="adminToken">
            <AdminDashboard />
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/all-users"
        element={
          <ProtectedRoute tokenKey="adminToken">
            <AdminAllUsers />
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/users/:email"
        element={
          <ProtectedRoute tokenKey="adminToken">
            <AdminUserDetail />
          </ProtectedRoute>
        }
      />

      <Route
        path="/user"
        element={
          <ProtectedRoute tokenKey="userToken">
            <UserDashboard />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  );
}

export default App;
