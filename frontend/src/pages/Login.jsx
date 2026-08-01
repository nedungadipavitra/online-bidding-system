import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Button from "../components/Button";
import login from "../assets/login.png";
import "../styles/Login.css";
import { apiJson, decodeJwtPayload, saveAuthSession } from "../api/client";

function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = async () => {
    if (!email || !password) {
      alert("Email and password are required");
      return;
    }

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
      alert(error.status === 401 ? "Invalid email or password" : "Error connecting to server");
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
