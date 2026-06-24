import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../../styles/admin.css";
import API, { clearAuth } from "../../services/api";

function getUsersFromResponse(data) {
  return data?.data?.content || [];
}

function getUserStatus(user) {
  if (user?.status) return String(user.status).toUpperCase();
  return user?.enable ? "APPROVED" : "PENDING";
}

function getStatusClass(user) {
  const status = getUserStatus(user).toLowerCase();
  if (status === "blocked") return "denied";
  return status;
}

function AdminDashboard() {
  const navigate = useNavigate();

  const [pendingUsers, setPendingUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState("");

  async function fetchPendingUsers() {
    try {
      setLoading(true);
      setActionMessage("");

      const response = await API.get("/admin/seeAllUsers", {
        params: {
          pageNumber: 0,
          pageSize: 1000,
          sortBy: "email",
          sortOrder: "asc",
        },
        authRole: "admin",
      });

      if (response.data.success) {
        const users = getUsersFromResponse(response.data);
        setPendingUsers(users.filter((user) => getUserStatus(user) === "PENDING"));
      } else {
        setPendingUsers([]);
        setActionMessage(response.data.message || "Failed to load user requests.");
      }
    } catch (error) {
      setPendingUsers([]);
      setActionMessage(error.response?.data?.message || "Failed to load user requests.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchPendingUsers();
  }, []);

  function handleLogout() {
    clearAuth("admin");
    navigate("/admin-login");
  }

  async function handleApprove(user) {
    if (!user.hotels || user.hotels.length === 0) {
      alert("Map at least one hotel to this user before approving.");
      navigate(`/admin/users/${encodeURIComponent(user.email)}`);
      return;
    }

    try {
      const response = await API.post("/admin/userAccessGrantedById", { email: user.email }, { authRole: "admin" });

      if (!response.data.success) {
        setActionMessage(response.data.message || "Approve failed.");
        return;
      }

      setActionMessage(`${user.email} approved successfully.`);
      fetchPendingUsers();
    } catch (error) {
      setActionMessage(error.response?.data?.message || "Approve failed.");
    }
  }

  async function handleDeny(user) {
    const confirmed = window.confirm(`Are you sure you want to deny ${user.email}?`);
    if (!confirmed) return;

    try {
      const response = await API.post("/admin/userAccessDeniedByEmail", { email: user.email }, { authRole: "admin" });

      if (!response.data.success) {
        setActionMessage(response.data.message || "Deny failed.");
        return;
      }

      setActionMessage(response.data.message || `${user.email} denied successfully.`);
      fetchPendingUsers();
    } catch (error) {
      setActionMessage(error.response?.data?.message || "Deny failed.");
    }
  }

  function renderActions(user) {
    return (
      <div className="action-row wrap-actions">
        <button
          type="button"
          className="secondary-btn"
          onClick={(event) => {
            event.stopPropagation();
            navigate(`/admin/users/${encodeURIComponent(user.email)}`);
          }}
        >
          Manage Hotels
        </button>

        <button
          type="button"
          className="approve-btn"
          onClick={(event) => {
            event.stopPropagation();
            handleApprove(user);
          }}
        >
          Approve
        </button>

        <button
          type="button"
          className="deny-btn"
          onClick={(event) => {
            event.stopPropagation();
            handleDeny(user);
          }}
        >
          Deny
        </button>
      </div>
    );
  }

  return (
    <div className="admin-page">
      <div className="sidebar">
        <div className="sidebar-logo">ERevMax</div>

        <nav className="sidebar-nav">
          <button className="nav-btn active" onClick={() => navigate("/admin")}>
            Dashboard
          </button>

          <button className="nav-btn" onClick={() => navigate("/admin/all-users")}>
            All Users
          </button>
        </nav>

        <button className="logout-btn" onClick={handleLogout}>
          Logout
        </button>
      </div>

      <div className="main-content">
        <div className="top-bar">
          <h1>User Requests</h1>
          <span className="admin-badge">Administrator</span>
        </div>

        {actionMessage && <div className="admin-message">{actionMessage}</div>}

        <div className="section dashboard-request-section">
          <div className="section-header-row">
            <div>
              <h2>Pending Requests</h2>
            </div>

            <button className="admin-details-btn compact-action-btn" onClick={() => navigate("/admin/all-users")}>
              Open All Users
            </button>
          </div>

          {loading ? (
            <p className="muted-text">Loading user requests...</p>
          ) : (
            <table className="user-table">
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Company</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {pendingUsers.length === 0 && (
                  <tr>
                    <td colSpan="5" className="empty-table-cell">
                      No pending user requests found.
                    </td>
                  </tr>
                )}

                {pendingUsers.map((user) => (
                  <tr
                    key={user.email}
                    className="clickable-row"
                    onClick={() => navigate(`/admin/users/${encodeURIComponent(user.email)}`)}
                  >
                    <td>{user.name || "-"}</td>
                    <td>{user.email}</td>
                    <td>{user.companyName || "-"}</td>
                    <td>
                      <span className={`status-badge ${getStatusClass(user)}`}>
                        {getUserStatus(user).toLowerCase()}
                      </span>
                    </td>
                    <td>{renderActions(user)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

export default AdminDashboard;
