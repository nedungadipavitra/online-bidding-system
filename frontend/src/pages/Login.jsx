import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Button from "../components/Button";
import login from "../assets/login.png";
import "../styles/Login.css";
import { toast } from "react-toastify";
import { apiJson, decodeJwtPayload, saveAuthSession } from "../api/client";

function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleLogin = async () => {
    if (!email || !password) {
      toast.error("Email and password are required.");
      return;
    }

    setIsSubmitting(true);
    try {
      const data = await apiJson("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password })
      });

      const tokenPayload = decodeJwtPayload(data.accessToken);
      const userData = await apiJson(`/users/${tokenPayload.userId}`, {
        headers: {
          Authorization: `Bearer ${data.accessToken}`,
        },
      });
      const user = {
        id: tokenPayload.userId,
        name: userData.name,
        role: tokenPayload.role,
      };
      saveAuthSession({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        user,
      });

      if (user.role === "ADMIN") {
        navigate("/admin/dashboard");
      } else if (user.role === "SELLER") {
        navigate("/seller/dashboard");
      } else if (user.role === "BUYER") {
        navigate("/buyer/dashboard");
      } else {
        navigate("/delivery");
      }
    } catch (error) {
      console.error("Login error:", error);
      toast.error(error.status === 401 ? "Invalid email or password." : error.message || "Unable to connect to the server.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <div className="login-container">
        <div className="login-card">
          <h1>Login</h1>

          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <div onClick={handleLogin}>
            <Button
              color={"var(--blue-primary)"}
              logo={login}
              hover={"blue"}
              text={"Login"}
              loading={isSubmitting}
              loadingText="Signing in..."
            />
          </div>

          <hr />

          <p>
            New user? <Link to="/register">Register here</Link>
          </p>
        </div>
      </div>
    </>
  );
}

export default Login;
