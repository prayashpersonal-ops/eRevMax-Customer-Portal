import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "../../styles/login.css";
import API, { saveAuth } from "../../services/api";

const GOOGLE_LOGIN_URL = "http://localhost:8080/oauth2/authorization/google";

function validatePassword(password) {
  if (password.length < 8) return "Password must be at least 8 characters long.";
  if (!/[A-Z]/.test(password)) return "Password must contain one uppercase letter.";
  if (!/[a-z]/.test(password)) return "Password must contain one lowercase letter.";
  if (!/[0-9]/.test(password)) return "Password must contain one number.";
  if (!/[^A-Za-z0-9]/.test(password)) return "Password must contain one special character.";
  return "";
}

function UserAuthPage() {
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState("login");

  const [loginEmail, setLoginEmail] = useState("");
  const [loginPassword, setLoginPassword] = useState("");
  const [loginError, setLoginError] = useState("");
  const [loginLoading, setLoginLoading] = useState(false);

  const [signupName, setSignupName] = useState("");
  const [signupEmail, setSignupEmail] = useState("");
  const [signupPassword, setSignupPassword] = useState("");
  const [signupCompany, setSignupCompany] = useState("");
  const [signupAddress, setSignupAddress] = useState("");
  const [signupError, setSignupError] = useState("");
  const [signupLoading, setSignupLoading] = useState(false);

  function handleGoogleLogin() {
    window.location.href = GOOGLE_LOGIN_URL;
  }

  async function handleLogin(e) {
    e.preventDefault();
    setLoginLoading(true);
    setLoginError("");

    try {
      const response = await API.post("/user/login", {
        email: loginEmail.trim(),
        password: loginPassword,
      });

      const data = response.data;

      if (!data.success) {
        if (data.message === "User account is disabled") {
          navigate("/pending");
          return;
        }

        setLoginError(data.message || "Login failed");
        return;
      }

      saveAuth("user", data);
      navigate("/user");
    } catch (error) {
      setLoginError(error.response?.data?.message || "Server error");
    } finally {
      setLoginLoading(false);
    }
  }

  async function handleSignup(e) {
    e.preventDefault();
    setSignupLoading(true);
    setSignupError("");

    const passwordError = validatePassword(signupPassword);
    if (passwordError) {
      setSignupError(passwordError);
      setSignupLoading(false);
      return;
    }

    try {
      const response = await API.post("/user/register", {
        name: signupName.trim(),
        email: signupEmail.trim(),
        password: signupPassword,
        companyName: signupCompany.trim(),
        address: signupAddress.trim(),
      });

      const data = response.data;

      if (!data.success) {
        setSignupError(data.message || "Registration failed");
        return;
      }

      alert("Registration successful. Wait for admin approval.");
      setActiveTab("login");
      setLoginEmail(signupEmail.trim());
      setSignupPassword("");
    } catch (error) {
      setSignupError(error.response?.data?.message || "Server error");
    } finally {
      setSignupLoading(false);
    }
  }

  return (
    <div className="login-page">
      <div className="left-section">
        <div className="logo-box"></div>

        <div className="hero-content">
          <h1>
            Your Travel <br />
            <span>Portal</span>
          </h1>
          <div className="underline"></div>
          <p>
            Find top rated hotels, manage bookings and grow your travel business with ease.
          </p>
        </div>

        <div className="feature-list">
          <div className="feature-card">
            <div className="feature-icon">H</div>
            <div>
              <h3>Hotel Discovery</h3>
              <p>Browse hotels assigned by admin</p>
            </div>
          </div>

          <div className="feature-card">
            <div className="feature-icon">R</div>
            <div>
              <h3>Rate Mapping</h3>
              <p>View room and rate plan pricing</p>
            </div>
          </div>

          <div className="feature-card">
            <div className="feature-icon">S</div>
            <div>
              <h3>Secure Login</h3>
              <p>Stay logged in until 2 hours of inactivity</p>
            </div>
          </div>
        </div>

        <div className="admin-corner">
          <span>Are you an admin?</span>
          <button
            type="button"
            className="admin-corner-btn"
            onClick={() => navigate("/admin-login")}
          >
            Login here
          </button>
        </div>
      </div>

      <div className="right-section">
        <div className="login-card">
          <div className="lock-circle">{activeTab === "login" ? "U" : "S"}</div>

          <div className="tab-row">
            <button
              type="button"
              className={activeTab === "login" ? "tab-btn active" : "tab-btn"}
              onClick={() => {
                setActiveTab("login");
                setLoginError("");
                setSignupError("");
              }}
            >
              Login
            </button>
            <button
              type="button"
              className={activeTab === "signup" ? "tab-btn active" : "tab-btn"}
              onClick={() => {
                setActiveTab("signup");
                setLoginError("");
                setSignupError("");
              }}
            >
              Sign Up
            </button>
          </div>

          {activeTab === "login" && (
            <form onSubmit={handleLogin}>
              <p className="subtitle">Welcome back! Login to continue</p>

              <input
                type="email"
                placeholder="Enter your email"
                value={loginEmail}
                onChange={(e) => setLoginEmail(e.target.value)}
                required
              />

              <input
                type="password"
                placeholder="Enter your password"
                value={loginPassword}
                onChange={(e) => setLoginPassword(e.target.value)}
                required
              />

              <div className="login-options">
                <label>
                  <input type="checkbox" defaultChecked /> Keep me logged in
                </label>
                <span>You will be automatically logged out after 2 hours of inactivity.</span>
              </div>

              {loginError && <p className="error-message">{loginError}</p>}

              <button type="submit" disabled={loginLoading}>
                {loginLoading ? "Logging in..." : "Login"}
              </button>

              <div className="auth-divider">
                <span></span>
                <p>or</p>
                <span></span>
              </div>

              <button
                type="button"
                className="google-login-btn"
                onClick={handleGoogleLogin}
              >
                Continue with Google
              </button>
            </form>
          )}

          {activeTab === "signup" && (
            <form onSubmit={handleSignup}>
              <p className="subtitle">Create your account to get started</p>

              <input
                type="text"
                placeholder="Full name"
                value={signupName}
                onChange={(e) => setSignupName(e.target.value)}
                required
              />

              <input
                type="email"
                placeholder="Email address"
                value={signupEmail}
                onChange={(e) => setSignupEmail(e.target.value)}
                required
              />

              <input
                type="password"
                placeholder="Create a password"
                value={signupPassword}
                onChange={(e) => setSignupPassword(e.target.value)}
                required
              />

              <p className="password-hint">
                Min 8 chars, uppercase, lowercase, number and special character.
              </p>

              <input
                type="text"
                placeholder="Company name"
                value={signupCompany}
                onChange={(e) => setSignupCompany(e.target.value)}
                required
              />

              <input
                type="text"
                placeholder="Address"
                value={signupAddress}
                onChange={(e) => setSignupAddress(e.target.value)}
                required
              />

              {signupError && <p className="error-message">{signupError}</p>}

              <button type="submit" disabled={signupLoading}>
                {signupLoading ? "Signing up..." : "Create Account"}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}

export default UserAuthPage;
