import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch, apiJson } from "../api/client";
import { createRequestCache } from "../api/resources";

function DeliveryDashboard() {
  const navigate = useNavigate();
  const [deliveries, setDeliveries] = useState([]);
  const loggedInUserId = sessionStorage.getItem("loggedInUserId");

  const loadDeliveries = useCallback(async () => {
    if (!loggedInUserId) return;
    try {
      const dbOrders = await apiJson(`/orders/delivery/${loggedInUserId}`);
      const requestCache = createRequestCache();
      const mapped = await Promise.all(dbOrders.map(async (order) => {
        const [product, buyer, seller] = await Promise.all([
          requestCache.getProduct(order.productId).catch(() => null),
          requestCache.getUser(order.buyerId).catch(() => null),
          requestCache.getUser(order.sellerId).catch(() => null)
        ]);

        return {
          id: order.id,
          productName: product?.name || "Product",
          buyerName: buyer?.name || "Buyer",
          sellerName: seller?.name || "Seller",
          price: order.finalPrice,
          status: order.status || order.deliveryStatus || "ASSIGNED"
        };
      }));
      setDeliveries(mapped);
    } catch (error) {
      console.error("Error loading deliveries:", error);
    }
  }, [loggedInUserId]);

  useEffect(() => {
    // The callback performs external data loading and updates state after its async responses.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadDeliveries();
  }, [loadDeliveries]);

  const updateStatus = async (orderId, newStatus) => {
    try {
      const token = sessionStorage.getItem("token");
      const res = await apiFetch(`/orders/${orderId}/status`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          "Authorization": token ? `Bearer ${token}` : ""
        },
        body: JSON.stringify({ status: newStatus })
      });
      if (res.ok) {
        alert(`Order status updated to ${newStatus}`);
        loadDeliveries();
      } else {
        const errorData = await res.json();
        alert(errorData.message || "Failed to update status");
      }
    } catch (e) {
      console.error(e);
      alert("Error updating order status");
    }
  };

  const renderActionButtons = (delivery) => {
    const status = delivery.status.toUpperCase();
    if (status === "ASSIGNED") {
      return (
        <button className="btn btn-outline-primary btn-sm" onClick={() => updateStatus(delivery.id, "DISPATCHED")}>
          Dispatch Order
        </button>
      );
    } else if (status === "DISPATCHED" || status === "OUT_FOR_DELIVERY") {
      return (
        <button className="btn btn-outline-success btn-sm" onClick={() => updateStatus(delivery.id, "DELIVERED")}>
          Mark Delivered
        </button>
      );
    } else if (status === "DELIVERED") {
      return (
        <span className="text-secondary small">
          Delivered
        </span>
      );
    }
    return null;
  };

  return (
    <div className="main-content container py-4 text-start" style={{ maxWidth: "1000px" }}>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="fw-bold mb-1" style={{ fontSize: "2rem" }}>Delivery Dashboard</h1>
          <p className="text-muted mb-0">Track and update won auctions shipment progress.</p>
        </div>
        <button onClick={() => navigate("/")} className="btn btn-outline-primary">&larr; Auctions</button>
      </div>

      {/* Deliveries Table Card */}
      <div className="card border shadow-sm">
        <div className="card-body p-0">
          <div className="table-responsive">
            <table className="table align-middle mb-0 table-hover">
              <thead className="table-light">
                <tr className="border-bottom text-muted" style={{ fontSize: "0.85rem" }}>
                  <th className="px-4 py-3">Order ID</th>
                  <th className="py-3">Product Name</th>
                  <th className="py-3">Buyer</th>
                  <th className="py-3">Seller</th>
                  <th className="py-3">Final Price</th>
                  <th className="py-3">Status</th>
                  <th className="px-4 py-3 text-center">Action</th>
                </tr>
              </thead>
              <tbody>
                {deliveries.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="text-center py-5 text-muted">No assigned deliveries found.</td>
                  </tr>
                ) : (
                  deliveries.map((delivery) => (
                    <tr key={delivery.id}>
                      <td className="px-4 py-3 fw-bold text-secondary">{delivery.id}</td>
                      <td className="py-3 fw-semibold">{delivery.productName}</td>
                      <td className="py-3">{delivery.buyerName}</td>
                      <td className="py-3">{delivery.sellerName}</td>
                      <td className="py-3 fw-bold text-success">₹{delivery.price.toLocaleString("en-IN")}</td>
                      <td className="py-3">
                        <span className={`badge rounded-pill px-3 py-1.5 ${
                          delivery.status.toLowerCase() === "delivered" ? "bg-success" : 
                          delivery.status.toLowerCase() === "dispatched" || delivery.status.toLowerCase() === "out for delivery" ? "bg-primary" : "bg-warning text-dark"
                        }`}>
                          {delivery.status}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-center">
                        {renderActionButtons(delivery)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

export default DeliveryDashboard;
