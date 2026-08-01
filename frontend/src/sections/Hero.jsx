import { useNavigate } from "react-router-dom";
import Button from "../components/Button";
import login from "../assets/login.png";
import register from "../assets/register.png";

function Hero() {
  const navigate = useNavigate();

  return (
    <div className="d-flex justify-content-center py-4 px-2">
      <div className="text-center w-100" style={{ maxWidth: "600px" }}>
        <h1 className="display-5 fs-2 fs-sm-1 fw-bold mb-3">Online Bidding System</h1>
        <div style={{ color: "var(--text-secondary)", fontSize: "0.95rem" }} className="mb-4">
          A real-time auction platform built using Spring Boot, React JS and
          MySQL
        </div>
        <div className="d-flex flex-column flex-sm-row gap-2 justify-content-center align-items-stretch align-items-sm-center">
          <div style={{ minWidth: "140px" }}>
            <Button
              color={"var(--blue-primary)"}
              logo={login}
              hover={"blue"}
              text={"Login"}
              onClick={() => navigate("/login")}
            />
          </div>
          <div style={{ minWidth: "140px" }}>
            <Button
              color={"var(--green-primary)"}
              logo={register}
              hover={"green"}
              text={"Register"}
              onClick={() => navigate("/register")}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

export default Hero;
