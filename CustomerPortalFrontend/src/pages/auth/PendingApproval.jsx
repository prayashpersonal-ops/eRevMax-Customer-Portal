import { useNavigate } from "react-router-dom";
import "../../styles/login.css";

function PendingApproval() {
  const navigate = useNavigate();

  function handleBackToLogin() {
    navigate("/");
  }

  return (
    <div className="login-page">

      <div className="left-section">
        <div className="logo-box"></div>

        <div className="hero-content">
          <h1>
            Almost <br />
            <span>There!</span>
          </h1>
          <div className="underline"></div>
          <p>
            Your account is being reviewed by our admin team. You'll be notified once approved.
          </p>
        </div>

        <div className="feature-list">
          <div className="feature-card">
            <div className="feature-icon">R</div>
            <div>
              <h3>Under Review</h3>
              <p>Admin is verifying your details</p>
            </div>
          </div>

          <div className="feature-card">
            <div className="feature-icon">E</div>
            <div>
              <h3>Check Your Email</h3>
              <p>We'll notify you once approved</p>
            </div>
          </div>

          <div className="feature-card">
            <div className="feature-icon">A</div>
            <div>
              <h3>Then You're In!</h3>
              <p>Access hotels, analytics and more</p>
            </div>
          </div>
        </div>
      </div>

      <div className="right-section">
        <div className="login-card">

          <div className="lock-circle">P</div>

          <h1>Pending</h1>

          <p className="subtitle">
            Your account is waiting for admin approval
          </p>

          <div className="pending-info">
            <p>Your details are being reviewed</p>
            <p>You'll receive an email once approved</p>
            <p>Then you can log in and get started</p>
          </div>

          <button type="button" onClick={handleBackToLogin}>
            Back to Login
          </button>

        </div>
      </div>

    </div>
  );
}

export default PendingApproval;