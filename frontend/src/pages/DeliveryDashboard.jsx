import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import LoadingState from "../components/LoadingState";
import { apiFetch, apiJson, getResponseMessage } from "../api/client";
import { createRequestCache } from "../api/resources";

function DeliveryDashboard() {
  const navigate = useNavigate();
  const [deliveries, setDeliveries] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [updatingOrderId, setUpdatingOrderId] = useState(null);
  const loggedInUserId = sessionStorage.getItem("loggedInUserId");

  const loadDeliveries = useCallback(async () => {
    if (!loggedInUserId) {
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
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
      toast.error("Unable to load assigned deliveries.");
    } finally {
      setIsLoading(false);
    }
  }, [loggedInUserId]);

  useEffect(() => {
    // The callback performs external data loading and updates state after its async responses.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadDeliveries();
  }, [loadDeliveries]);

  const updateStatus = async (orderId, newStatus) => {
    setUpdatingOrderId(orderId);
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
        toast.success(`Order status updated to ${newStatus}.`);
        await loadDeliveries();
      } else {
        toast.error(await getResponseMessage(res, "Failed to update status."));
      }
    } catch (e) {
      console.error(e);
      toast.error("Error updating order status.");
    } finally {
      setUpdatingOrderId(null);
    }
  };

  const renderActionButtons = (delivery) => {
    const status = delivery.status.toUpperCase();
    if (status === "ASSIGNED") {
      return (
        <button className="btn btn-outline-primary btn-sm" onClick={() => updateStatus(delivery.id, "DISPATCHED")} disabled={updatingOrderId === delivery.id}>
          {updatingOrderId === delivery.id ? "Updating..." : "Dispatch Order"}
        </button>
      );
    } else if (status === "DISPATCHED" || status === "OUT_FOR_DELIVERY") {
      return (
        <button className="btn btn-outline-success btn-sm" onClick={() => updateStatus(delivery.id, "DELIVERED")} disabled={updatingOrderId === delivery.id}>
          {updatingOrderId === delivery.id ? "Updating..." : "Mark Delivered"}
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
                {isLoading ? (
                  <tr>
                    <td colSpan="7">
                      <LoadingState message="Loading deliveries..." compact />
                    </td>
                  </tr>
                ) : deliveries.length === 0 ? (
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
