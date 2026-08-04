import { Navigate } from "react-router-dom";

function ProtectedRoute({ children, allowedRoles }) {
  const userName = sessionStorage.getItem("loggedInUserName");
  const userRole = sessionStorage.getItem("loggedInUserRole");

  if (!userName) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(userRole)) {
    return <Navigate to="/" replace />;
  }

  return children;
}

export default ProtectedRoute;
