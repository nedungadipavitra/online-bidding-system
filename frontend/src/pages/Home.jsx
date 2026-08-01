import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Hero from "../sections/Hero";

function Home() {
  const navigate = useNavigate();

  useEffect(() => {
    const name = sessionStorage.getItem("loggedInUserName");
    const role = sessionStorage.getItem("loggedInUserRole");
    if (name && role) {
      if (role === "SELLER") {
        navigate("/seller/dashboard", { replace: true });
      } else if (role === "DELIVERY") {
        navigate("/delivery", { replace: true });
      } else if (role === "ADMIN") {
        navigate("/admin/dashboard", { replace: true });
      } else {
        navigate("/buyer/dashboard", { replace: true });
      }
    }
  }, [navigate]);

  return (
    <div className="home-container">
      <div className="home-content">
        <Hero />
      </div>
    </div>
  );
}

export default Home;
