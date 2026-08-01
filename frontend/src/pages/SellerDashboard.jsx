import { useState } from "react";
import AddProductForm from "../sections/AddProductForm";
import MyProducts from "../sections/MyProducts";

function SellerDashboard() {
  const userName = sessionStorage.getItem("loggedInUserName") || "Seller";
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const handleProductAdded = () => {
    setRefreshTrigger((prev) => prev + 1);
  };

  return (
    <div className="main-content">
      <div>
        <div className="py-4">
          <h1 className="fw-bold text-center">Seller Dashboard</h1>
          <div
            style={{ color: "var(--text-secondary)" }}
            className="text-center"
          >
            Welcome, <strong>{userName}</strong>
          </div>
        </div>
        <AddProductForm onProductAdded={handleProductAdded} />
        <div className="mt-3">
          <MyProducts refreshTrigger={refreshTrigger} />
        </div>
      </div>
    </div>
  );
}

export default SellerDashboard;
