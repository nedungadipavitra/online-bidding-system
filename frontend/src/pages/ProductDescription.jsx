import { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import Button from "../components/Button";
import bidIcon from "../assets/bid.png";
import { createWebSocketClient } from "../utils/websocket";
import { apiFetch } from "../api/client";
import { getAuctionPhase } from "../utils/auctionStatus";

function ProductDescription() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [unavailableMessage, setUnavailableMessage] = useState("");
  const [bidAmount, setBidAmount] = useState("");
  const userName = sessionStorage.getItem("loggedInUserName");
  const userRole = sessionStorage.getItem("loggedInUserRole");

  const loadProduct = useCallback(async () => {
    try {
      const token = sessionStorage.getItem("token");
      const response = await apiFetch(`/products/${id}`, {
        headers: {
          "Authorization": token ? `Bearer ${token}` : ""
        }
      });
      if (response.ok) {
        const p = await response.json();

        if (userRole === "BUYER" && !["ACTIVE"].includes(getAuctionPhase(p))) {
          setUnavailableMessage("This auction is not currently open for bidding.");
          setProduct(null);
          return;
        }
        setUnavailableMessage("");
        
        let highestBid = p.currentHighestBid || p.basePrice;
        try {
          const bidRes = await apiFetch(`/bids/highest?auctionIds=${p.productId}`, {
            headers: {
              "Authorization": token ? `Bearer ${token}` : ""
            }
          });
          if (bidRes.ok) {
            const highestBids = await bidRes.json();
            const bidData = highestBids[String(p.productId)] || highestBids[p.productId];
            if (bidData && bidData.amount) {
              highestBid = bidData.amount;
            }
          }
        } catch (e) {
          console.error("Error fetching highest bid:", e);
        }

        let sellerName = "";
        try {
          const sellerRes = await apiFetch(`/users/${p.sellerId}`, {
            headers: {
              "Authorization": token ? `Bearer ${token}` : ""
            }
          });
          if (sellerRes.ok) {
            const sellerData = await sellerRes.json();
            if (sellerData && sellerData.name) {
              sellerName = sellerData.name;
            }
          }
        } catch (e) {
          console.error("Error fetching seller details:", e);
        }

        setProduct({
          id: p.productId,
          name: p.name,
          description: p.description,
          image: p.imageUrl,
          imageSrc: p.imageUrl,
          basePrice: p.basePrice,
          currentBid: highestBid,
          endTime: p.auctionEndTime,
          startTime: p.auctionStartTime,
          status: p.status,
          sellerId: p.sellerId,
          seller: sellerName,
          category: "General", // Default/fallback category
          features: [] // Fallback features
        });
      } else {
        setUnavailableMessage("");
        setProduct(null);
      }
    } catch (error) {
      console.error("Error fetching product details:", error);
      setUnavailableMessage("");
      setProduct(null);
    }
  }, [id, userRole]);

  useEffect(() => {
    // The callback performs external data loading and updates state after its async responses.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadProduct();

    const disconnect = createWebSocketClient((bidUpdate) => {
      // Receive bid updates in real time and update only the currentBid state
      setProduct((prevProduct) => {
        if (!prevProduct || String(prevProduct.id) !== String(bidUpdate.auctionId)) {
          return prevProduct;
        }
        const nextBid = Number(bidUpdate.amount);
        if (!Number.isFinite(nextBid) || nextBid <= Number(prevProduct.currentBid)) {
          return prevProduct;
        }
        return {
          ...prevProduct,
          currentBid: nextBid
        };
      });
    }, id);

    return () => {
      disconnect();
    };
  }, [id, loadProduct]);

  if (!product) {
    return (
      <div className="main-content container text-center py-5">
        <h3>{unavailableMessage || "Product not found"}</h3>
        <button className="btn btn-primary mt-3" onClick={() => navigate("/")}>
          Back to Home
        </button>
      </div>
    );
  }

  const handleBidSubmit = async (e) => {
    e.preventDefault();

    if (!userName) {
      alert("You must be logged in to place a bid.");
      navigate("/login");
      return;
    }

    if (userRole !== "BUYER") {
      alert("Only buyers are allowed to place bids.");
      return;
    }

    const numericBid = parseFloat(bidAmount);
    if (isNaN(numericBid)) {
      alert("Please enter a valid bid amount.");
      return;
    }

    if (product.startTime && new Date(product.startTime) > new Date()) {
      alert("Bidding has not started yet!");
      return;
    }

    const token = sessionStorage.getItem("token");
    const loggedInUserId = sessionStorage.getItem("loggedInUserId");

    try {
      // The backend performs wallet debit, seller credit, and outbid refund
      // atomically as part of placing the bid.
      const response = await apiFetch("/bids", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": token ? `Bearer ${token}` : ""
        },
        body: JSON.stringify({
          auctionId: product.id,
          bidderId: loggedInUserId,
          bidderName: userName,
          amount: numericBid
        })
      });

      if (response.ok) {
        const placedBid = await response.json();
        alert("Bid placed successfully! Wallet balances updated by the server.");
        setBidAmount("");
        setProduct((previousProduct) => previousProduct
          ? { ...previousProduct, currentBid: Number(placedBid.amount) }
          : previousProduct);
      } else {
        const errorData = await response.json();
        alert(errorData.error || errorData.message || "Failed to place bid.");
      }
    } catch (error) {
      console.error("Bid error:", error);
      alert("Error connecting to server");
    }
  };

  return (
    <div className="main-content container py-4 text-start">
      <button onClick={() => navigate(-1)} className="btn btn-link mb-3 p-0 text-decoration-none">
        &larr; Back
      </button>

      <div className="row g-4">
        <div className="col-md-6 text-center bg-light p-3 rounded border d-flex align-items-center justify-content-center" style={{ minHeight: "300px" }}>
          <img 
            src={product.imageSrc} 
            alt={product.name || product.title} 
            className="img-fluid rounded shadow-sm" 
            style={{ maxHeight: "350px", objectFit: "contain" }}
          />
        </div>
        <div className="col-md-6">
          <h1 className="fw-bold">{product.name || product.title}</h1>
          <div className="d-flex align-items-center gap-2 mb-2">
            <div className="badge bg-secondary">{product.category}</div>
            {product.seller && (
              <span className="text-muted small">
                Seller: <strong>{product.seller}</strong>
              </span>
            )}
          </div>
          <p className="text-muted leading-relaxed">{product.description}</p>
          
          {product.features && product.features.length > 0 && (
            <div className="mb-4">
              <h5 className="fw-bold fs-6">Key Specifications:</h5>
              <ul className="ps-3 small text-secondary">
                {product.features.map((feature, idx) => (
                  <li key={idx}>{feature}</li>
                ))}
              </ul>
            </div>
          )}

          <hr />
          
          <div className="d-flex justify-content-between bg-light p-3 rounded mb-3 border">
            <div>
              <span className="text-secondary small d-block">Base Price</span>
              <span className="fw-bold text-success fs-5">₹{product.basePrice.toLocaleString("en-IN")}</span>
            </div>
            <div className="text-end">
              <span className="text-secondary small d-block">Current Bid</span>
              <span className="fw-bold text-primary fs-5">₹{product.currentBid.toLocaleString("en-IN")}</span>
            </div>
          </div>

          <form onSubmit={handleBidSubmit} className="border p-3 rounded bg-white shadow-sm">
            <div className="mb-3">
              <label className="form-label small text-secondary fw-semibold">Place Your Bid (INR)</label>
              <input
                type="number"
                className="form-control"
                placeholder={`Higher than ₹${product.currentBid.toLocaleString("en-IN")}`}
                value={bidAmount}
                onChange={(e) => setBidAmount(e.target.value)}
                required
              />
            </div>
            <Button
              color={"var(--blue-primary)"}
              logo={bidIcon}
              hover={"blue"}
              text={"Place Bid"}
              type="submit"
            />
          </form>
        </div>
      </div>
    </div>
  );
}

export default ProductDescription;
