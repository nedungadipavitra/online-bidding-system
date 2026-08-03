import { useCallback, useState } from "react";
import AddProductForm from "../sections/AddProductForm";
import MyProducts from "../sections/MyProducts";
import ReloadButton from "../components/ReloadButton";

function SellerDashboard() {
  const userName = sessionStorage.getItem("loggedInUserName") || "Seller";
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [isRefreshingProducts, setIsRefreshingProducts] = useState(false);

  const handleProductAdded = () => {
    setRefreshTrigger((prev) => prev + 1);
  };

  const reloadProducts = () => {
    setRefreshTrigger((prev) => prev + 1);
  };

  const handleProductsLoadingChange = useCallback((loading) => {
    setIsRefreshingProducts(loading);
  }, []);

  return (
    <div className="main-content">
      <div>
        <div className="py-4 container d-flex justify-content-between align-items-center gap-3 flex-wrap">
          <div>
            <h1 className="fw-bold mb-1">Seller Dashboard</h1>
            <div
              style={{ color: "var(--text-secondary)" }}
            >
              Welcome, <strong>{userName}</strong>
            </div>
          </div>
          <ReloadButton onClick={reloadProducts} loading={isRefreshingProducts} label="Reload products" />
        </div>
        <AddProductForm onProductAdded={handleProductAdded} />
        <div className="mt-3">
          <MyProducts refreshTrigger={refreshTrigger} onLoadingChange={handleProductsLoadingChange} />
        </div>
      </div>
    </div>
  );
}

export default SellerDashboard;
