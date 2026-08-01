import { useState } from "react";
import iphone from "../assets/iphone.jpeg";
import ActiveStatus from "./ActiveStatus";
import SoldStatus from "./SoldStatus";
import { apiFetch } from "../api/client";

const fallbackProduct = {
  name: "iPhone 17 Pro",
  category: "Electronics",
  basePrice: 70000,
  currentBid: 85250,
  status: "ACTIVE",
  endTime: "2026-08-30T22:00"
};

function ProductRow({ product = fallbackProduct, deliveryPartners = [], onAssignSuccess }) {
  const [selectedPartnerId, setSelectedPartnerId] = useState("");
  const [isAssigning, setIsAssigning] = useState(false);

  const handleAssign = async () => {
    if (!selectedPartnerId) {
      alert("Please select a delivery partner first!");
      return;
    }
    setIsAssigning(true);
    try {
      const token = sessionStorage.getItem("token");
      let oId = product.orderId;
      if (!oId) {
        const createRes = await apiFetch("/orders", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": token ? `Bearer ${token}` : ""
          },
          body: JSON.stringify({
            productId: product.id,
            buyerId: product.winnerId,
            sellerId: product.sellerId,
            finalPrice: product.currentBid,
            status: "PENDING"
          })
        });
        if (createRes.ok) {
          const newOrder = await createRes.json();
          oId = newOrder.id;
        } else {
          throw new Error("Failed to create order");
        }
      }

      const assignRes = await apiFetch(`/orders/${oId}/assign-delivery`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": token ? `Bearer ${token}` : ""
        },
        body: JSON.stringify({
          deliveryPersonId: Number(selectedPartnerId)
        })
      });
      if (assignRes.ok) {
        alert("Delivery partner assigned successfully!");
        if (onAssignSuccess) onAssignSuccess();
      } else {
        alert("Failed to assign delivery partner.");
      }
    } catch (err) {
      console.error(err);
      alert("Error assigning delivery partner.");
    } finally {
      setIsAssigning(false);
    }
  };

  const formatDate = (dateStr) => {
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString("en-IN", {
        day: "numeric",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
      });
    } catch {
      return dateStr;
    }
  };

  const isEnded = new Date(product.endTime) <= new Date();
  const hasBids = product.currentBid > product.basePrice;

  const renderStatus = () => {
    if (product.status === "SOLD") {
      return <SoldStatus />;
    }
    if (product.status === "NOT_SOLD" || product.status === "UNSOLD") {
      return (
        <div
          style={{
            backgroundColor: "#f8d7da",
            color: "#721c24",
            width: "max-content",
          }}
          className="rounded text-center fw-bold px-3 py-1"
        >
          NOT SOLD
        </div>
      );
    }
    if (product.status === "ACTIVE") {
      if (product.startTime && new Date(product.startTime) > new Date()) {
        return (
          <div
            style={{
              backgroundColor: "#fff3cd",
              color: "#664d03",
              width: "max-content",
            }}
            className="rounded text-center fw-bold px-3 py-1"
          >
            UPCOMING
          </div>
        );
      }
      if (isEnded) {
        if (hasBids) {
          return <SoldStatus />;
        } else {
          return (
            <div
              style={{
                backgroundColor: "#f8d7da",
                color: "#721c24",
                width: "max-content",
              }}
              className="rounded text-center fw-bold px-3 py-1"
            >
              NOT SOLD
            </div>
          );
        }
      }
      return <ActiveStatus />;
    }
    if (product.status === "UPCOMING") {
      return (
        <div
          style={{
            backgroundColor: "#fff3cd",
            color: "#664d03",
            width: "max-content",
          }}
          className="rounded text-center fw-bold px-3 py-1"
        >
          UPCOMING
        </div>
      );
    }
    if (product.status === "ENDED") {
      return hasBids ? <SoldStatus /> : (
        <div
          style={{
            backgroundColor: "#f8d7da",
            color: "#721c24",
            width: "max-content",
          }}
          className="rounded text-center fw-bold px-3 py-1"
        >
          NOT SOLD
        </div>
      );
    }
    return <ActiveStatus />;
  };

  return (
    <tr>
      <td>
        <div className="d-flex align-items-center">
          <img
            src={product.image || iphone}
            alt={product.name}
            className="rounded mx-2"
            style={{
              width: "60px",
              height: "60px",
              objectFit: "cover"
            }}
          />
          <div>
            <div className="fw-bold">{product.name}</div>
            <div style={{ color: "var(--text-secondary)", fontSize: "0.8rem" }}>
              {product.category}
            </div>
            {isEnded && hasBids && product.winnerName && (
              <div className="badge bg-success-subtle text-success border border-success-subtle mt-1" style={{ fontSize: "0.75rem" }}>
                Winner: {product.winnerName}
              </div>
            )}
          </div>
        </div>
      </td>
      <td>
        <div className="py-3">₹{product.basePrice.toLocaleString()}</div>
      </td>
      <td className="fw-bold py-3 text-primary">
        ₹{product.currentBid.toLocaleString()}
      </td>
      <td>
        <div className="py-2">
          {renderStatus()}
        </div>
      </td>
      <td style={{ color: "var(--text-secondary)" }}>
        <div className="py-3">{formatDate(product.endTime)}</div>
      </td>
      <td>
        <div className="py-2">
          {product.deliveryPersonName ? (
            <span className="text-success fw-bold">
              Assigned to: {product.deliveryPersonName} <br />
              <small className="text-secondary">({product.deliveryStatus || "ASSIGNED"})</small>
            </span>
          ) : (isEnded && hasBids) ? (
            <div className="d-flex gap-2 align-items-center">
              <select
                className="form-select form-select-sm"
                style={{ width: "160px" }}
                value={selectedPartnerId}
                onChange={(e) => setSelectedPartnerId(e.target.value)}
                disabled={isAssigning}
              >
                <option value="">Assign Partner</option>
                {deliveryPartners.map((partner) => (
                  <option key={partner.id} value={partner.id}>
                    {partner.name}
                  </option>
                ))}
              </select>
              <button
                className="btn btn-primary btn-sm"
                onClick={handleAssign}
                disabled={isAssigning || !selectedPartnerId}
              >
                {isAssigning ? "..." : "Assign"}
              </button>
            </div>
          ) : (
            <span className="text-muted small">-</span>
          )}
        </div>
      </td>
    </tr>
  );
}

export default ProductRow;
