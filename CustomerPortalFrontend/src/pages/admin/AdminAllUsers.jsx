import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../../styles/admin.css";
import API, { clearAuth } from "../../services/api";

const PAGE_SIZE = 10;

function getUsersFromResponse(data) {
  return data?.data?.content || [];
}

function getPageInfo(data) {
  const pageData = data?.data || {};
  return {
    pageNumber: pageData.pageNumber || 0,
    pageSize: pageData.pageSize || PAGE_SIZE,
    totalElements: pageData.totalElements || 0,
    totalPages: pageData.totalPages || 1,
    lastPage: Boolean(pageData.lastPage),
  };
}

function getStats(data) {
  const stats = data?.data?.stats;

  if (stats) {
    return {
      totalUsers: stats.totalUsers || 0,
      pendingUsers: stats.pendingUsers || 0,
      approvedUsers: stats.approvedUsers || 0,
      deniedUsers: stats.deniedUsers || 0,
      blockedUsers: stats.blockedUsers || 0,
    };
  }

  return {
    totalUsers: data?.data?.totalElements || 0,
    pendingUsers: 0,
    approvedUsers: 0,
    deniedUsers: 0,
    blockedUsers: 0,
  };
}

function getUserStatus(user) {
  if (user?.status) return String(user.status).toUpperCase();
  return user?.enable ? "APPROVED" : "PENDING";
}

function getStatusLabel(user) {
  return getUserStatus(user).toLowerCase();
}

function getStatusClass(user) {
  const status = getUserStatus(user).toLowerCase();
  return status;
}

function AdminAllUsers() {
  const navigate = useNavigate();

  const [users, setUsers] = useState([]);
  const [searchText, setSearchText] = useState("");
  const [pageNumber, setPageNumber] = useState(0);
  const [pageInfo, setPageInfo] = useState({
    pageNumber: 0,
    pageSize: PAGE_SIZE,
    totalElements: 0,
    totalPages: 1,
    lastPage: true,
  });
  const [stats, setStats] = useState({
    totalUsers: 0,
    pendingUsers: 0,
    approvedUsers: 0,
    deniedUsers: 0,
    blockedUsers: 0,
  });
  const [loading, setLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState("");

  const searchQuery = searchText.trim();

  async function fetchUsers(nextPage = pageNumber) {
    try {
      setLoading(true);
      setActionMessage("");

      const response = await API.get("/admin/seeAllUsers", {
        params: {
          pageNumber: nextPage,
          pageSize: PAGE_SIZE,
          sortBy: "email",
          sortOrder: "asc",
          search: searchQuery,
        },
        authRole: "admin",
      });

      if (response.data.success) {
        setUsers(getUsersFromResponse(response.data));
        setPageInfo(getPageInfo(response.data));
        setStats(getStats(response.data));
      } else {
        setUsers([]);
        setActionMessage(response.data.message || "Failed to load users.");
      }
    } catch (error) {
      setUsers([]);
      setActionMessage(error.response?.data?.message || "Failed to load users.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => {
      fetchUsers(pageNumber);
    }, 250);

    return () => window.clearTimeout(timer);
  }, [pageNumber, searchQuery]);

  function handleLogout() {
    clearAuth("admin");
    navigate("/admin-login");
  }

  function handleSearchChange(event) {
    setSearchText(event.target.value);
    setPageNumber(0);
  }

  function goToPage(nextPage) {
    const totalPages = Math.max(pageInfo.totalPages || 1, 1);
    const safePage = Math.max(0, Math.min(nextPage, totalPages - 1));
    setPageNumber(safePage);
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
      fetchUsers(pageNumber);
    } catch (error) {
      setActionMessage(error.response?.data?.message || "Approve failed.");
    }
  }

  async function handleDenyOrBlock(user) {
    const status = getUserStatus(user);
    const actionName = status === "APPROVED" ? "block" : "deny";
    const confirmed = window.confirm(`Are you sure you want to ${actionName} ${user.email}?`);
    if (!confirmed) return;

    try {
      const response = await API.post("/admin/userAccessDeniedByEmail", { email: user.email }, { authRole: "admin" });

      if (!response.data.success) {
        setActionMessage(response.data.message || `${actionName} failed.`);
        return;
      }

      setActionMessage(response.data.message || `${user.email} ${actionName}ed successfully.`);
      fetchUsers(pageNumber);
    } catch (error) {
      setActionMessage(error.response?.data?.message || `${actionName} failed.`);
    }
  }

  function renderUserActions(user) {
    const status = getUserStatus(user);

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

        {status === "PENDING" && (
          <>
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
                handleDenyOrBlock(user);
              }}
            >
              Deny
            </button>
          </>
        )}

        {status === "APPROVED" && (
          <button
            type="button"
            className="deny-btn"
            onClick={(event) => {
              event.stopPropagation();
              handleDenyOrBlock(user);
            }}
          >
            Block
          </button>
        )}

        {status === "BLOCKED" && <span className="muted-text small-action-note">Blocked</span>}
      </div>
    );
  }

  function renderPagination() {
    const totalPages = Math.max(pageInfo.totalPages || 1, 1);
    const currentPage = pageInfo.pageNumber || 0;
    const pages = [];
    const start = Math.max(0, currentPage - 2);
    const end = Math.min(totalPages - 1, currentPage + 2);

    for (let page = start; page <= end; page += 1) {
      pages.push(page);
    }

    return (
      <div className="pagination-row">
        <button className="secondary-btn" disabled={currentPage === 0 || loading} onClick={() => goToPage(currentPage - 1)}>
          Previous
        </button>

        {start > 0 && (
          <button className="page-btn" onClick={() => goToPage(0)} disabled={loading}>
            1
          </button>
        )}

        {start > 1 && <span className="pagination-dots">...</span>}

        {pages.map((page) => (
          <button
            key={page}
            className={page === currentPage ? "page-btn active" : "page-btn"}
            onClick={() => goToPage(page)}
            disabled={loading}
          >
            {page + 1}
          </button>
        ))}

        {end < totalPages - 2 && <span className="pagination-dots">...</span>}

        {end < totalPages - 1 && (
          <button className="page-btn" onClick={() => goToPage(totalPages - 1)} disabled={loading}>
            {totalPages}
          </button>
        )}

        <button className="secondary-btn" disabled={currentPage >= totalPages - 1 || loading} onClick={() => goToPage(currentPage + 1)}>
          Next
        </button>

        <span className="pagination-info">
          Page {currentPage + 1} of {totalPages} · {pageInfo.totalElements} result(s)
        </span>
      </div>
    );
  }

  return (
    <div className="admin-page">
      <div className="sidebar">
        <div className="sidebar-logo">ERevMax</div>

        <nav className="sidebar-nav">
          <button className="nav-btn" onClick={() => navigate("/admin")}>
            Dashboard
          </button>

          <button className="nav-btn active" onClick={() => navigate("/admin/all-users")}>
            All Users
          </button>
        </nav>

        <button className="logout-btn" onClick={handleLogout}>
          Logout
        </button>
      </div>

      <div className="main-content">
        <div className="top-bar">
          <div className="header-left">
            <button className="back-btn" onClick={() => navigate("/admin")}>
              Dashboard
            </button>
            <h1>All Users</h1>
          </div>
          <span className="admin-badge">Administrator</span>
        </div>

        {actionMessage && <div className="admin-message">{actionMessage}</div>}

        <div className="stats-row admin-stats-row-wide">
          <div className="stat-card">
            <span className="stat-number">{stats.totalUsers}</span>
            <span className="stat-label">Total Users</span>
          </div>
          <div className="stat-card pending">
            <span className="stat-number">{stats.pendingUsers}</span>
            <span className="stat-label">Pending Users</span>
          </div>
          <div className="stat-card approved">
            <span className="stat-number">{stats.approvedUsers}</span>
            <span className="stat-label">Approved Users</span>
          </div>
          <div className="stat-card blocked-stat">
            <span className="stat-number">{stats.blockedUsers}</span>
            <span className="stat-label">Blocked Users</span>
          </div>
        </div>

        <div className="section toolbar-section">
          <div>
            <h2>Users List</h2>
          </div>

          <input
            className="admin-search-input"
            type="text"
            placeholder="Search by name, email or company..."
            value={searchText}
            onChange={handleSearchChange}
          />
        </div>

        <div className="section">
          {loading ? (
            <p className="muted-text">Loading users...</p>
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
                {users.length === 0 && (
                  <tr>
                    <td colSpan="5" className="empty-table-cell">
                      No users found.
                    </td>
                  </tr>
                )}

                {users.map((user) => (
                  <tr
                    key={user.id || user.email}
                    className="clickable-row"
                    onClick={() => navigate(`/admin/users/${encodeURIComponent(user.email)}`)}
                  >
                    <td>{user.name || "—"}</td>
                    <td>{user.email}</td>
                    <td>{user.companyName || "—"}</td>
                    <td>
                      <span className={`status-badge ${getStatusClass(user)}`}>
                        {getStatusLabel(user)}
                      </span>
                    </td>
                    <td>{renderUserActions(user)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {renderPagination()}
        </div>
      </div>
    </div>
  );
}

export default AdminAllUsers;
