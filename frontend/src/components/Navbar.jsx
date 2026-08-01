import { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { apiFetch, clearAuthSession, getRefreshToken } from "../api/client";

function Navbar() {
  const navigate = useNavigate();
  useLocation();
  const [isOpen, setIsOpen] = useState(false);

  const user = (() => {
    const name = sessionStorage.getItem("loggedInUserName");
    const role = sessionStorage.getItem("loggedInUserRole");
    return name && role ? { name, role } : null;
  })();

  const handleLogout = async () => {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      try {
        await apiFetch("/auth/logout", {
          method: "POST",
          body: JSON.stringify({ refreshToken }),
        });
      } catch (error) {
        console.error("Logout request failed:", error);
      }
    }
    clearAuthSession();
    navigate("/");
  };

  const getDashboardPath = () => {
    if (!user) return "/";
    if (user.role === "SELLER") return "/seller/dashboard";
    if (user.role === "DELIVERY") return "/delivery";
    if (user.role === "ADMIN") return "/admin/dashboard";
    return "/buyer/dashboard";
  };

  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-black fixed-top py-3 px-4">
      <div className="container-fluid p-0">
        {user ? (
          <span className="navbar-brand fw-bold fs-5 text-white m-0" style={{ cursor: "default" }}>
            Online Bidding
          </span>
        ) : (
          <Link to="/" className="navbar-brand fw-bold fs-5 text-white m-0">
            Online Bidding
          </Link>
        )}
        <button
          className="navbar-toggler border-secondary"
          type="button"
          onClick={() => setIsOpen(!isOpen)}
          aria-expanded={isOpen}
          aria-label="Toggle navigation"
        >
          <span className="navbar-toggler-icon"></span>
        </button>

        <div className={`collapse navbar-collapse justify-content-end ${isOpen ? "show" : ""}`} id="navbarNav">
          <div className="d-flex flex-column flex-lg-row align-items-stretch align-items-lg-center gap-3 mt-3 mt-lg-0 w-100 justify-content-end">
            {user ? (
              <>
                <span className="text-white-50 small my-1 my-lg-0 text-start text-lg-end">
                  Welcome, <strong className="text-white">{user.name}</strong> ({user.role})
                </span>

                {/* Conditional Wallet link for Buyer and Seller */}
                {(user.role === "BUYER" || user.role === "SELLER") && (
                  <button
                    onClick={() => navigate(user.role === "BUYER" ? "/buyer-wallet" : "/seller-wallet")}
                    style={{ backgroundColor: "#6f42c1" }}
                    className="text-white rounded-1 border-0 px-3 py-2 text-center"
                  >
                    My Wallet
                  </button>
                )}

                {/* Conditional Delivery Dashboard link */}
                {user.role === "DELIVERY" && (
                  <button
                    onClick={() => navigate("/delivery")}
                    style={{ backgroundColor: "var(--green-primary)" }}
                    className="text-white rounded-1 border-0 px-3 py-2 text-center"
                  >
                    Deliveries
                  </button>
                )}

                <button
                  onClick={() => navigate(getDashboardPath())}
                  style={{ backgroundColor: "var(--blue-primary)" }}
                  className="text-white rounded-1 border-0 px-3 py-2 btn-hover-blue text-center"
                >
                  {user.role === "ADMIN" ? "Dashboard (Admin)" : "Dashboard"}
                </button>

                <button
                  onClick={handleLogout}
                  style={{ backgroundColor: "#dc3545" }}
                  className="text-white rounded-1 border-0 px-3 py-2 text-center"
                >
                  Logout
                </button>
              </>
            ) : (
              <div className="d-flex gap-2 justify-content-start justify-content-lg-end mt-2 mt-lg-0">
                <button
                  onClick={() => navigate("/login")}
                  style={{
                    backgroundColor: "var(--blue-primary)",
                  }}
                  className="text-white rounded-1 border-0 px-3 py-2 btn-hover-blue flex-grow-1 flex-lg-grow-0 text-center"
                >
                  Login
                </button>
                <button
                  onClick={() => navigate("/register")}
                  style={{ backgroundColor: "var(--green-primary)" }}
                  className="text-white rounded-1 border-0 px-3 py-2 btn-hover-green flex-grow-1 flex-lg-grow-0 text-center"
                >
                  Register
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}

export default Navbar;
