import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../../styles/user.css";
import API, { clearAuth, getToken } from "../../services/api";

function decodeToken(token) {
  try {
    const base64Payload = token.split(".")[1];
    const payload = base64Payload.replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(payload));
  } catch {
    return {};
  }
}

function money(value) {
  if (value === null || value === undefined) return "—";
  return `₹${Number(value).toFixed(2)}`;
}

function getHotelName(hotel) {
  return hotel?.name || hotel?.hotelName || "Unnamed Hotel";
}

function UserDashboard() {
  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [hotels, setHotels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("profile");
  const [searchText, setSearchText] = useState("");

  const [selectedHotel, setSelectedHotel] = useState(null);
  const [hotelDetails, setHotelDetails] = useState(null);
  const [roomRatePlans, setRoomRatePlans] = useState({});
  const [detailsLoading, setDetailsLoading] = useState(false);

  const [selectedRoomCode, setSelectedRoomCode] = useState("");
  const [selectedRatePlanCode, setSelectedRatePlanCode] = useState("");
  const [priceLoading, setPriceLoading] = useState(false);
  const [priceDetails, setPriceDetails] = useState(null);
  const [priceError, setPriceError] = useState("");
  const [message, setMessage] = useState("");

  async function fetchHotels() {
    try {
      setLoading(true);
      const response = await API.get("/user/listOfHotelsOfUser", { authRole: "user" });

      if (response.data.success && response.data.data) {
        setHotels(response.data.data);
      } else {
        setHotels([]);
        setMessage(response.data.message || "No hotels found for your account.");
      }
    } catch (error) {
      setHotels([]);
      setMessage(error.response?.data?.message || "Failed to load hotels.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const token = getToken("user");
    const decoded = decodeToken(token || "");

    setUser({
      name: decoded.name || decoded.sub || "User",
      email: decoded.sub || decoded.email || "—",
      company: decoded.companyName || "—",
      address: decoded.address || "—",
    });

    fetchHotels();
  }, []);

  const filteredHotels = useMemo(() => {
    const query = searchText.trim().toLowerCase();
    if (!query) return hotels;

    return hotels.filter((hotel) => getHotelName(hotel).toLowerCase().includes(query));
  }, [hotels, searchText]);

  function isHotelSellable(hotel) {
    return Boolean(hotel?.sellable);
  }

  async function fetchRatePlansForRooms(details) {
    const rooms = details?.roomTypes || [];
    const fallbackPlans = details?.ratePlans || [];
    const nextRoomRatePlans = {};

    await Promise.all(
      rooms.map(async (room) => {
        if (!room.roomTypeCode) {
          nextRoomRatePlans[room.roomTypeCode || "unknown"] = fallbackPlans;
          return;
        }

        try {
          const response = await API.post(
            "/hotels/getRatePlan",
            { code: room.roomTypeCode },
            { authRole: "user" }
          );

          nextRoomRatePlans[room.roomTypeCode] = response.data.success
            ? response.data.data || []
            : fallbackPlans;
        } catch {
          nextRoomRatePlans[room.roomTypeCode] = fallbackPlans;
        }
      })
    );

    return nextRoomRatePlans;
  }

  async function handleHotelClick(hotel) {
    try {
      setSelectedHotel(hotel);
      setHotelDetails(null);
      setRoomRatePlans({});
      setSelectedRoomCode("");
      setSelectedRatePlanCode("");
      setPriceDetails(null);
      setPriceError("");
      setDetailsLoading(true);

      const response = await API.post(
        "/hotels/getHotelDetailsWithRoomTypeAndPatePlan",
        { code: getHotelName(hotel) },
        { authRole: "user" }
      );

      const details = response.data.success && response.data.data?.length > 0
        ? response.data.data[0]
        : { ...hotel, roomTypes: [], ratePlans: [] };

      setHotelDetails(details);
      const mappedPlans = await fetchRatePlansForRooms(details);
      setRoomRatePlans(mappedPlans);
    } catch (error) {
      setHotelDetails({ ...hotel, roomTypes: [], ratePlans: [] });
      setPriceError(error.response?.data?.message || "Hotel details failed.");
    } finally {
      setDetailsLoading(false);
    }
  }

  async function fetchPriceFor(roomCode, ratePlanCode) {
    if (!selectedHotel || !roomCode || !ratePlanCode) {
      setPriceError("Select one room type and one rate plan first.");
      return;
    }

    try {
      setPriceLoading(true);
      setPriceError("");
      setPriceDetails(null);
      setSelectedRoomCode(roomCode);
      setSelectedRatePlanCode(ratePlanCode);

      const response = await API.post(
        "/hotels/roomRateMappingPlan",
        {
          hotelName: getHotelName(selectedHotel),
          roomTypeCode: roomCode,
          ratePlanCode,
        },
        { authRole: "user" }
      );

      if (response.data.success && response.data.data?.length > 0) {
        setPriceDetails(response.data.data[0]);
      } else {
        setPriceError(response.data.message || "No price mapping found.");
      }
    } catch (error) {
      setPriceError(error.response?.data?.message || "Price mapping failed.");
    } finally {
      setPriceLoading(false);
    }
  }

  async function handleToggleSellable(hotel) {
    const hotelName = getHotelName(hotel);
    const current = isHotelSellable(hotel);
    const next = !current;

    try {
      const response = await API.post(
        "/hotels/updateUserHotelSellable",
        {
          userHotelId: hotel.userHotelId,
          hotelId: hotel.hotelId || hotel.id,
          hotelName,
          sellable: next,
        },
        { authRole: "user" }
      );

      setHotels((currentHotels) =>
        currentHotels.map((item) => {
          const sameHotel =
            (hotel.userHotelId && item.userHotelId === hotel.userHotelId) ||
            getHotelName(item) === hotelName;

          return sameHotel ? { ...item, sellable: next } : item;
        })
      );

      setSelectedHotel((currentSelected) => {
        if (!currentSelected || getHotelName(currentSelected) !== hotelName) return currentSelected;
        return { ...currentSelected, sellable: next };
      });

      setMessage(response.data.message || (next ? "Hotel marked sellable." : "Hotel removed from sellable."));
    } catch (error) {
      setMessage(error.response?.data?.message || "Sellable toggle failed.");
    }
  }

  function closeDetailsModal() {
    setSelectedHotel(null);
    setHotelDetails(null);
    setRoomRatePlans({});
    setSelectedRoomCode("");
    setSelectedRatePlanCode("");
    setPriceDetails(null);
    setPriceError("");
  }

  function handleLogout() {
    clearAuth("user");
    navigate("/");
  }

  function renderHotelCard(hotel) {
    const isSellable = isHotelSellable(hotel);

    return (
      <div className={isSellable ? "hotel-card sellable" : "hotel-card"} key={hotel.userHotelId || hotel.hotelId || hotel.id || getHotelName(hotel)}>
        <div className="hotel-emoji">H</div>

        <div className="hotel-info">
          <div className="hotel-title-row">
            <h3>{getHotelName(hotel)}</h3>
            {isSellable && <span className="sellable-chip">Sellable</span>}
          </div>
          <p> Room Types: {hotel.roomTypes?.length || 0}</p>
          <p> Rate Plans: {hotel.ratePlans?.length || 0}</p>

          <div className="hotel-card-actions">
            <button type="button" className="details-btn" onClick={() => handleHotelClick(hotel)}>
              View Details
            </button>
            <button type="button" className={isSellable ? "sellable-btn active" : "sellable-btn"} onClick={() => handleToggleSellable(hotel)}>
              {isSellable ? "Sellable" : "Mark Sellable"}
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (loading) {
    return <div style={{ padding: "2rem" }}>Loading...</div>;
  }

  return (
    <div className="user-page">
      <div className="sidebar">
        <div className="sidebar-logo">ERevMax</div>

        <nav className="sidebar-nav">
          <button
            className={activeTab === "profile" ? "nav-btn active" : "nav-btn"}
            onClick={() => setActiveTab("profile")}
          >
            My Profile
          </button>

          <button
            className={activeTab === "hotels" ? "nav-btn active" : "nav-btn"}
            onClick={() => setActiveTab("hotels")}
          >
            Hotels
          </button>
        </nav>

        <button className="logout-btn" onClick={handleLogout}>Logout</button>
      </div>

      <div className="main-content">
        {message && <div className="user-message">{message}</div>}

        {activeTab === "profile" && (
          <div>
            <div className="top-bar">
              <h1>My Profile</h1>
              <span className="user-badge">Travel Agent</span>
            </div>

            <div className="stats-row">
              <div className="stat-card">
                <span className="stat-number">{hotels.length}</span>
                <span className="stat-label">Hotels Available</span>
              </div>

              <div className="stat-card approved">
                <span className="stat-number">
                  {hotels.reduce((sum, hotel) => sum + (hotel.ratePlans?.length || 0), 0)}
                </span>
                <span className="stat-label">Rate Plans</span>
              </div>

              <div className="stat-card pending">
                <span className="stat-number">
                  {hotels.reduce((sum, hotel) => sum + (hotel.roomTypes?.length || 0), 0)}
                </span>
                <span className="stat-label">Room Types</span>
              </div>
            </div>

            <div className="profile-card">
              <div className="profile-avatar">U</div>

              <div className="profile-info">
                <div className="info-row"><span className="info-label">Full Name</span><span className="info-value">{user?.name}</span></div>
                <div className="info-row"><span className="info-label">Email</span><span className="info-value">{user?.email}</span></div>
                <div className="info-row"><span className="info-label">Company</span><span className="info-value">{user?.company}</span></div>
                <div className="info-row"><span className="info-label">Address</span><span className="info-value">{user?.address}</span></div>
              </div>
            </div>

            <div className="section-header">
              <h2>Assigned Hotels</h2>
              <button type="button" className="view-all-btn" onClick={() => setActiveTab("hotels")}>
                View All
              </button>
            </div>

            <div className="hotel-grid">
              {hotels.length === 0 ? <p className="no-results">No hotels assigned yet.</p> : hotels.slice(0, 3).map(renderHotelCard)}
            </div>
          </div>
        )}

        {activeTab === "hotels" && (
          <div>
            <div className="top-bar">
              <h1>Hotels</h1>
              <span className="user-badge">Travel Agent</span>
            </div>

            <div className="search-row">
              <input
                type="text"
                className="search-input"
                placeholder="Search assigned hotels..."
                value={searchText}
                onChange={(event) => setSearchText(event.target.value)}
              />
            </div>

            <div className="hotel-grid">
              {filteredHotels.length === 0 ? <p className="no-results">No hotels found.</p> : filteredHotels.map(renderHotelCard)}
            </div>
          </div>
        )}
      </div>

      {selectedHotel && (
        <div className="details-modal-overlay" onClick={closeDetailsModal}>
          <div className="details-modal" onClick={(event) => event.stopPropagation()}>
            <div className="modal-header">
              <div>
                <p className="details-label">Hotel Details</p>
                <h2>{getHotelName(selectedHotel)}</h2>
                <div className="hotel-summary-line">
                  <span>{hotelDetails?.roomTypes?.length || 0} Room Types</span>
                  <span>{hotelDetails?.ratePlans?.length || 0} Rate Plans</span>
                  {isHotelSellable(selectedHotel) && <span className="green-pill">Sellable</span>}
                </div>
              </div>

              <button type="button" className="close-details-btn" onClick={closeDetailsModal}>×</button>
            </div>

            {detailsLoading ? (
              <p className="details-loading">Loading details...</p>
            ) : (
              <>
                {priceError && <p className="error-message inline-error">{priceError}</p>}

                {priceDetails && (
                  <div className="rate-map-box selected-price-box">
                    <h3>Selected Price Mapping</h3>
                    <p>{selectedRoomCode} + {selectedRatePlanCode}</p>
                    <div className="price-grid">
                      <div><span>Base Rate</span><b>{money(priceDetails.baseRate)}</b></div>
                      <div><span>Taxes & Fee</span><b>{money(priceDetails.taxesAndFee)}</b></div>
                      <div><span>Total Trip Cost</span><b>{money(priceDetails.totalTripCost)}</b></div>
                      <div><span>Hotel Receives</span><b>{money(priceDetails.hotelReceives)}</b></div>
                      <div><span>Agent Earnings %</span><b>{priceDetails.agentEarningsPercent ?? "—"}</b></div>
                      <div><span>Occupancy</span><b>{priceDetails.occupancy ?? "—"}</b></div>
                      <div><span>Cancellation Charge</span><b>{money(priceDetails.cancellationCharge)}</b></div>
                      <div><span>Breakfast</span><b>{priceDetails.breakfastIncluded ? "Included" : "Not Included"}</b></div>
                    </div>
                  </div>
                )}

                <div className="details-section">
                  <h3>Rooms with Rate Plans</h3>

                  {hotelDetails?.roomTypes?.length === 0 ? (
                    <p className="empty-message">No room types found</p>
                  ) : (
                    <div className="details-grid combined-grid">
                      {hotelDetails?.roomTypes?.map((room) => {
                        const plans = roomRatePlans[room.roomTypeCode] || hotelDetails?.ratePlans || [];

                        return (
                          <div className="room-detail-card combined" key={room.roomTypeCode}>
                            <div className="room-card-header">
                              <h4>{room.roomTypeName || "Unnamed Room"}</h4>
                              <span>{room.roomTypeCode || "—"}</span>
                            </div>

                            <div className="room-badges">
                              <span>{room.roomView || "No View"}</span>
                              <span>{room.bedType || "Bed N/A"}</span>
                              <span>Max {room.maxRoomCapacity || "—"}</span>
                            </div>

                            <p className="room-description">
                              {room.shortDescription || "No short description available."}
                            </p>

                            <div className="detail-row"><span>Base Occupancy</span><b>{room.baseOccupancy || "—"}</b></div>
                            <div className="detail-row"><span>Adults / Children</span><b>{room.maxAdults || "—"} / {room.maxChildren || "—"}</b></div>
                            <div className="detail-row"><span>Smoking</span><b>{room.smokingAllowed ? "Allowed" : "Not Allowed"}</b></div>

                            <div className="nested-rate-plan-list">
                              <h5>Rate Plans</h5>
                              {plans.length === 0 ? (
                                <p className="empty-message small-empty">No rate plans found for this room.</p>
                              ) : (
                                plans.map((plan) => (
                                  <button
                                    type="button"
                                    className="nested-rate-plan-btn"
                                    key={`${room.roomTypeCode}-${plan.ratePlanCode}`}
                                    onClick={() => fetchPriceFor(room.roomTypeCode, plan.ratePlanCode)}
                                    disabled={priceLoading}
                                  >
                                    <strong>{plan.ratePlanName || plan.ratePlanCode}</strong>
                                    <span>{plan.ratePlanCode || "—"}</span>
                                  </button>
                                ))
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default UserDashboard;
