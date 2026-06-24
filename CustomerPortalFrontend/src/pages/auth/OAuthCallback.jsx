import { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { getToken, saveAuth } from "../../services/api";

function decodeJwtPayload(token) {
  const payloadBase64Url = token.split(".")[1];
  if (!payloadBase64Url) return null;

  const payloadBase64 = payloadBase64Url.replace(/-/g, "+").replace(/_/g, "/");
  return JSON.parse(atob(payloadBase64));
}

function OAuthCallback() {
  const navigate = useNavigate();
  const hasHandledOAuth = useRef(false);

  useEffect(() => {
    if (hasHandledOAuth.current) return;
    hasHandledOAuth.current = true;

    const queryParams = new URLSearchParams(window.location.search);
    const token = queryParams.get("token") || queryParams.get("accessToken");
    const refreshToken = queryParams.get("refreshToken");

    if (!token) {
      if (getToken("user")) {
        navigate("/user", { replace: true });
        return;
      }

      navigate("/", { replace: true });
      return;
    }

    saveAuth("user", {
      accessToken: token,
      refreshToken,
      success: true,
    });

    try {
      const decodedPayload = decodeJwtPayload(token);
      if (decodedPayload?.enabled === false) {
        navigate("/pending", { replace: true });
        return;
      }
    } catch {
      navigate("/user", { replace: true });
      return;
    }

    navigate("/user", { replace: true });
  }, [navigate]);

  return <div style={{ padding: "2rem" }}>Logging you in...</div>;
}

export default OAuthCallback;
