import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import "../../styles/admin.css";
import API, { clearAuth } from "../../services/api";

function getHotelName(hotel) {
  return hotel?.hotelName || hotel?.name || "Unnamed Hotel";
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

function AdminUserDetail() {
  const { email } = useParams();
  const navigate = useNavigate();
  const decodedEmail = decodeURIComponent(email);

  const [user, setUser] = useState(null);
  const [allHotels, setAllHotels] = useState([]);
  const [selectedHotelNames, setSelectedHotelNames] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [notFound, setNotFound] = useState(false);
  const [selectedHotel, setSelectedHotel] = useState(null);

  const assignedHotelNames = useMemo(() => {
    return new Set((user?.hotels || []).map((hotel) => getHotelName(hotel)));
  }, [user]);

  const availableHotels = useMemo(() => {
    return allHotels.filter((hotel) => !assignedHotelNames.has(hotel.name));
  }, [allHotels, assignedHotelNames]);

  async function fetchUserAndHotels() {
    try {
      setLoading(true);
      setMessage("");
      setNotFound(false);

      const [usersResponse, hotelsResponse] = await Promise.all([
        API.get("/admin/seeAllUsers", {
          params: { pageNumber: 0, pageSize: 10, sortBy: "email", sortOrder: "asc", search: decodedEmail },
          authRole: "admin",
        }),
        API.get("/hotels/seeAllHotels", { authRole: "admin" }),
      ]);

      if (usersResponse.data.success) {
        const users = usersResponse.data?.data?.content || [];
        const found = users.find((item) => item.email === decodedEmail);

        if (found) {
          setUser(found);
        } else {
          setNotFound(true);
        }
      }

      if (hotelsResponse.data.success) {
        setAllHotels(hotelsResponse.data.data || []);
      }
    } catch (error) {
      setMessage(error.response?.data?.message || "Failed to load user details.");
      setNotFound(true);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchUserAndHotels();
  }, [decodedEmail]);


  function toggleHotelSelection(hotelName) {
    setSelectedHotelNames((current) => {
      if (current.includes(hotelName)) {
        return current.filter((name) => name !== hotelName);
      }

      return [...current, hotelName];
    });
  }

  async function handleAssignHotels() {
    if (selectedHotelNames.length === 0) {
      setMessage("Select at least one hotel to assign.");
      return;
    }

    try {
      setSaving(true);
      setMessage("");

      await API.post(
        "/hotels/addHotel",
        {
          email: decodedEmail,
          hotels: selectedHotelNames.map((name) => ({ name })),
        },
        { authRole: "admin" }
      );

      setSelectedHotelNames([]);
      setMessage("Hotels mapped successfully.");
      fetchUserAndHotels();
    } catch (error) {
      setMessage(error.response?.data?.message || "Hotel mapping failed.");
    } finally {
      setSaving(false);
    }
  }

  async function handleApprove() {
    if (!user.hotels || user.hotels.length === 0) {
      setMessage("Map at least one hotel before approving this user.");
      return;
    }

    try {
      setSaving(true);
      await API.post("/admin/userAccessGrantedById", { email: user.email }, { authRole: "admin" });
      setMessage("User approved successfully.");
      fetchUserAndHotels();
    } catch (error) {
      setMessage(error.response?.data?.message || "Approve failed.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDenyOrBlock() {
    const status = getUserStatus(user);
    const actionName = status === "APPROVED" ? "block" : "deny";
    const confirmed = window.confirm(`Are you sure you want to ${actionName} this user?`);
    if (!confirmed) return;

    try {
      setSaving(true);
      const response = await API.post("/admin/userAccessDeniedByEmail", { email: user.email }, { authRole: "admin" });
      if (!response.data.success) {
        setMessage(response.data.message || `${actionName} failed.`);
        return;
      }
      setMessage(response.data.message || (actionName === "block" ? "User blocked successfully." : "User denied successfully."));
      fetchUserAndHotels();
    } catch (error) {
      setMessage(error.response?.data?.message || `${actionName} failed.`);
    } finally {
      setSaving(false);
    }
  }

  async function handleUnmapHotel(hotel) {
    const hotelName = getHotelName(hotel);
    const confirmed = window.confirm(`Unmap ${hotelName} from ${user.email}?`);
    if (!confirmed) return;

    try {
      setSaving(true);
      setMessage("");

      const payload = hotel.userHotelId
        ? { userHotelId: hotel.userHotelId }
        : { email: user.email, hotelId: hotel.hotelId || hotel.id, hotelName };

      await API.post("/hotels/removeHotel", payload, { authRole: "admin" });

      setMessage(`${hotelName} unmapped successfully.`);
      if (selectedHotel && getHotelName(selectedHotel) === hotelName) {
        setSelectedHotel(null);
      }
      fetchUserAndHotels();
    } catch (error) {
      setMessage(error.response?.data?.message || "Unmap failed. Check backend removeHotel endpoint.");
    } finally {
      setSaving(false);
    }
  }

  function handleLogout() {
    clearAuth("admin");
    navigate("/admin-login");
  }

  if (loading) {
    return <div style={{ padding: "2rem" }}>Loading...</div>;
  }

  if (notFound || !user) {
    return (
      <div style={{ padding: "2rem" }}>
        <p>User not found.</p>
        <button className="back-btn" onClick={() => navigate("/admin")}>Back to Dashboard</button>
      </div>
    );
  }

  return (
    <div className="admin-page">
      <div className="sidebar">
        <div className="sidebar-logo">ERevMax</div>

        <nav className="sidebar-nav">
          <button className="nav-btn" onClick={() => navigate("/admin")}>Dashboard</button>
        </nav>

        <button className="logout-btn" onClick={handleLogout}>Logout</button>
      </div>

      <div className="main-content">
        <div className="top-bar">
          <div className="header-left">
            <button className="back-btn" onClick={() => navigate("/admin")}>Back</button>
            <h1>User Detail</h1>
          </div>

          <span className={`status-badge ${getStatusClass(user)}`}>
            {getUserStatus(user).toLowerCase()}
          </span>
        </div>

        {message && <div className="admin-message">{message}</div>}

        <div className="section">
          <div className="section-header-row">
            <h2>Profile Info</h2>
            <div className="action-row">
              {getUserStatus(user) !== "APPROVED" && (
                <button className="approve-btn" disabled={saving} onClick={handleApprove}>
                  Approve
                </button>
              )}

              {(getUserStatus(user) === "PENDING" || getUserStatus(user) === "APPROVED") && (
                <button className="deny-btn" disabled={saving} onClick={handleDenyOrBlock}>
                  {getUserStatus(user) === "APPROVED" ? "Block" : "Deny"}
                </button>
              )}
            </div>
          </div>

          <div className="detail-grid">
            <div className="detail-row"><span className="detail-label">Full Name</span><span className="detail-value">{user.name || "—"}</span></div>
            <div className="detail-row"><span className="detail-label">Email</span><span className="detail-value">{user.email}</span></div>
            <div className="detail-row"><span className="detail-label">Company</span><span className="detail-value">{user.companyName || "—"}</span></div>
            <div className="detail-row"><span className="detail-label">Address</span><span className="detail-value">{user.address || "—"}</span></div>
            <div className="detail-row"><span className="detail-label">LoginProvider</span><span className="detail-value">{user.provider || "LOCAL"}</span></div>
          </div>
        </div>

        <div className="section">
          <h2>Map Hotels Before Approval</h2>

          {availableHotels.length === 0 ? (
            <p className="empty-msg">All available hotels are already mapped to this user.</p>
          ) : (
            <div className="hotel-picker-grid">
              {availableHotels.map((hotel) => (
                <label className="hotel-picker-card" key={hotel.id || hotel.name}>
                  <input
                    type="checkbox"
                    checked={selectedHotelNames.includes(hotel.name)}
                    onChange={() => toggleHotelSelection(hotel.name)}
                  />
                  <div>
                    <strong>{hotel.name}</strong>
                    <p>{hotel.roomTypes?.length || 0} room types · {hotel.ratePlans?.length || 0} rate plans</p>
                  </div>
                </label>
              ))}
            </div>
          )}

          <button
            type="button"
            className="approve-btn map-hotels-btn"
            disabled={saving || selectedHotelNames.length === 0}
            onClick={handleAssignHotels}
          >
            {saving ? "Saving..." : `Assign Selected Hotels (${selectedHotelNames.length})`}
          </button>
        </div>

        <div className="section">
          <h2>Linked Hotels ({user.hotels?.length || 0})</h2>

          {(!user.hotels || user.hotels.length === 0) && (
            <p style={{ color: "#64748b" }}>No hotels linked to this user.</p>
          )}

          <div className="admin-hotel-grid">
            {(user.hotels || []).map((hotel, index) => {
              const hotelName = getHotelName(hotel);
              const isSellable = Boolean(hotel.sellable);

              return (
                <div
                  className={isSellable ? "admin-hotel-card sellable" : "admin-hotel-card"}
                  key={hotel.userHotelId || hotel.hotelId || hotel.id || `${hotelName}-${index}`}
                >
                  <div className="admin-hotel-card-top">
                    <div>
                      <h3>{hotelName}</h3>
                      {isSellable && <span className="sellable-chip">Sellable</span>}
                    </div>
                  </div>

                  <div className="admin-hotel-stats">
                    <p> Room Types: {hotel.roomTypes?.length || 0}</p>
                    <p> Rate Plans: {hotel.ratePlans?.length || 0}</p>
                  </div>

                  <div className="action-row wrap-actions">
                    <button type="button" className="admin-details-btn" onClick={() => setSelectedHotel(hotel)}>
                      View Details
                    </button>
                    <button type="button" className="danger-outline-btn" disabled={saving} onClick={() => handleUnmapHotel(hotel)}>
                      Unmap
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {selectedHotel && (
        <div className="admin-details-modal-overlay" onClick={() => setSelectedHotel(null)}>
          <div className="admin-details-modal" onClick={(event) => event.stopPropagation()}>
            <div className="admin-modal-header">
              <div>
                <p className="admin-details-label">Hotel Details</p>
                <h2>{getHotelName(selectedHotel)}</h2>
                <div className="admin-hotel-summary-line">
                  <span>{selectedHotel.roomTypes?.length || 0} Room Types</span>
                  <span>{selectedHotel.ratePlans?.length || 0} Rate Plans</span>
                </div>
              </div>

              <button className="admin-close-details-btn" onClick={() => setSelectedHotel(null)}>×</button>
            </div>

            <div className="admin-details-section">
              <h3>Rooms with Rate Plans</h3>
              {selectedHotel.roomTypes?.length === 0 ? (
                <p className="admin-empty-message">No room types found.</p>
              ) : (
                <div className="admin-details-grid">
                  {selectedHotel.roomTypes?.map((room) => (
                    <div className="admin-room-detail-card combined" key={room.roomTypeCode}>
                      <div className="admin-room-card-header">
                        <h4>{room.roomTypeName || "Unnamed Room"}</h4>
                        <span>{room.roomTypeCode || "—"}</span>
                      </div>
                      <div className="admin-room-badges">
                        <span>{room.roomView || "No View"}</span>
                        <span>{room.bedType || "Bed N/A"}</span>
                        <span>{room.baseOccupancy || "—"} Base Occupancy</span>
                      </div>
                      <p className="admin-room-description">
                        {room.shortDescription || "No short description available."}
                      </p>

                      <div className="nested-rate-plan-list">
                        <h5>Rate Plans</h5>
                        {(selectedHotel.ratePlans || []).length === 0 ? (
                          <p className="muted-text">No rate plans found.</p>
                        ) : (
                          (selectedHotel.ratePlans || []).map((plan) => (
                            <div className="nested-rate-plan" key={`${room.roomTypeCode}-${plan.ratePlanCode}`}>
                              <strong>{plan.ratePlanName || plan.ratePlanCode}</strong>
                              <span>{plan.ratePlanCode || "—"}</span>
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdminUserDetail;
