import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "../../styles/login.css";
import API, { saveAuth } from "../../services/api";

function AdminLogin() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleLogin(e) {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const response = await API.post("/admin/login", {
        email: email.trim(),
        password,
      });

      const data = response.data;

      if (!data.success || !data.accessToken) {
        setError(data.message || "Login failed");
        return;
      }

      saveAuth("admin", data);
      navigate("/admin");
    } catch (error) {
      setError(error.response?.data?.message || "Server error");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-page">
      <div className="left-section">
        <div className="logo-box"></div>

        <div className="hero-content">
          <h1>
            Welcome Back, <br />
            <span>Admin</span>
          </h1>

          <div className="underline"></div>

          <p>Access your dashboard, map hotels to users, approve users and block access.</p>
        </div>

        <div className="feature-list">
          <div className="feature-card">
            <div className="feature-icon">U</div>
            <div>
              <h3>User Management</h3>
              <p>Approve, deny and block users</p>
            </div>
          </div>

          <div className="feature-card">
            <div className="feature-icon">H</div>
            <div>
              <h3>Hotel Mapping</h3>
              <p>Assign hotels before approval</p>
            </div>
          </div>

          <div className="auth-card">
            <button className="back-btn" type="button" onClick={() => navigate("/")}>Back</button>
          </div>
        </div>
      </div>

      <div className="right-section">
        <div className="login-card">
          <div className="lock-circle">A</div>

          <h1>Admin Login</h1>
          <p className="subtitle">Enter your credentials to continue</p>

          <form onSubmit={handleLogin}>
            <input
              type="email"
              placeholder="Enter your email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />

            <input
              type="password"
              placeholder="Enter your password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />

            <div className="login-options">
              <label>
                <input type="checkbox" defaultChecked /> Keep me logged in
              </label>
              <span>You will be automatically logged out after 2 hours of inactivity.</span>
            </div>

            {error && <p className="error-message">{error}</p>}

            <button type="submit" disabled={loading}>
              {loading ? "Logging in..." : "Login"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

export default AdminLogin;
