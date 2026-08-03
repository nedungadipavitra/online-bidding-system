import { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import Button from "../components/Button";
import LoadingState from "../components/LoadingState";
import bidIcon from "../assets/bid.png";
import { createWebSocketClient } from "../utils/websocket";
import { apiFetch, getResponseMessage } from "../api/client";
import { getAuctionPhase } from "../utils/auctionStatus";

function ProductDescription() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [bids, setBids] = useState([]);
  const [unavailableMessage, setUnavailableMessage] = useState("");
  const [bidAmount, setBidAmount] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmittingBid, setIsSubmittingBid] = useState(false);
  const userName = sessionStorage.getItem("loggedInUserName");
  const userRole = sessionStorage.getItem("loggedInUserRole");

  const normalizeBid = useCallback((bid) => {
    const amount = Number(bid.amount ?? bid.bidAmount);
    return {
      id: bid.id ?? `${bid.auctionId}-${bid.bidderId}-${bid.bidTime}-${amount}`,
      auctionId: bid.auctionId,
      bidderId: bid.bidderId,
      bidderName: bid.bidderName || `Buyer ${bid.bidderId || ""}`.trim(),
      amount,
      bidTime: bid.bidTime || new Date().toISOString()
    };
  }, []);

  const sortBids = useCallback((bidList) => [...bidList].sort((first, second) => {
    if (Number(second.amount) !== Number(first.amount)) {
      return Number(second.amount) - Number(first.amount);
    }
    return new Date(second.bidTime).getTime() - new Date(first.bidTime).getTime();
  }), []);

  const mergeBid = useCallback((bidUpdate) => {
    const normalized = normalizeBid(bidUpdate);
    if (!Number.isFinite(normalized.amount) || String(normalized.auctionId) !== String(id)) {
      return;
    }

    setBids((currentBids) => {
      const withoutDuplicate = currentBids.filter((bid) => String(bid.id) !== String(normalized.id));
      return sortBids([normalized, ...withoutDuplicate]);
    });

    setProduct((prevProduct) => {
      if (!prevProduct || String(prevProduct.id) !== String(normalized.auctionId)) {
        return prevProduct;
      }
      if (normalized.amount <= Number(prevProduct.currentBid)) {
        return prevProduct;
      }
      return {
        ...prevProduct,
        currentBid: normalized.amount
      };
    });
  }, [id, normalizeBid, sortBids]);

  const loadProduct = useCallback(async () => {
    setIsLoading(true);
    try {
      const token = sessionStorage.getItem("token");
      const response = await apiFetch(`/products/${id}`, {
        headers: {
          "Authorization": token ? `Bearer ${token}` : ""
        }
      });

      if (!response.ok) {
        setUnavailableMessage("");
        setProduct(null);
        toast.error(await getResponseMessage(response, "Unable to load product details."));
        return;
      }

      const p = await response.json();
      if (userRole === "BUYER" && !["ACTIVE"].includes(getAuctionPhase(p))) {
        setUnavailableMessage("This auction is not currently open for bidding.");
        setProduct(null);
        setBids([]);
        return;
      }

      setUnavailableMessage("");

      const [highestBidData, bidHistory] = await Promise.all([
        apiFetch(`/bids/highest?auctionIds=${p.productId}`, {
          headers: {
            "Authorization": token ? `Bearer ${token}` : ""
          }
        })
          .then(async (bidRes) => (bidRes.ok ? bidRes.json() : {}))
          .catch(() => ({})),
        apiFetch(`/bids/auction/${p.productId}`, {
          headers: {
            "Authorization": token ? `Bearer ${token}` : ""
          }
        })
          .then(async (bidRes) => (bidRes.ok ? bidRes.json() : []))
          .catch(() => [])
      ]);

      const normalizedBids = sortBids((Array.isArray(bidHistory) ? bidHistory : []).map(normalizeBid));
      const highestBid = highestBidData[String(p.productId)] || highestBidData[p.productId];
      const currentBid = Number(highestBid?.amount ?? normalizedBids[0]?.amount ?? p.currentHighestBid ?? p.basePrice);

      let sellerName = "";
      try {
        const sellerRes = await apiFetch(`/users/${p.sellerId}`, {
          headers: {
            "Authorization": token ? `Bearer ${token}` : ""
          }
        });
        if (sellerRes.ok) {
          const sellerData = await sellerRes.json();
          sellerName = sellerData?.name || "";
        }
      } catch (e) {
        console.error("Error fetching seller details:", e);
      }

      setBids(normalizedBids);
      setProduct({
        id: p.productId,
        name: p.name,
        description: p.description,
        image: p.imageUrl,
        imageSrc: p.imageUrl,
        basePrice: p.basePrice,
        currentBid,
        endTime: p.auctionEndTime,
        startTime: p.auctionStartTime,
        status: p.status,
        sellerId: p.sellerId,
        seller: sellerName,
        category: "General",
        features: []
      });
    } catch (error) {
      console.error("Error fetching product details:", error);
      setUnavailableMessage("");
      setProduct(null);
      toast.error("Unable to load product details.");
    } finally {
      setIsLoading(false);
    }
  }, [id, normalizeBid, sortBids, userRole]);

  useEffect(() => {
    // The callback performs external data loading and updates state after its async responses.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadProduct();

    const disconnect = createWebSocketClient((bidUpdate) => {
      mergeBid(bidUpdate);
    }, id);

    return () => {
      disconnect();
    };
  }, [id, loadProduct, mergeBid]);

  const handleBidSubmit = async (e) => {
    e.preventDefault();

    if (!userName) {
      toast.error("You must be logged in to place a bid.");
      navigate("/login");
      return;
    }

    if (userRole !== "BUYER") {
      toast.error("Only buyers are allowed to place bids.");
      return;
    }

    const numericBid = parseFloat(bidAmount);
    if (isNaN(numericBid)) {
      toast.error("Please enter a valid bid amount.");
      return;
    }

    if (numericBid <= Number(product.currentBid)) {
      toast.error("Your bid must be higher than the current bid.");
      return;
    }

    if (product.startTime && new Date(product.startTime) > new Date()) {
      toast.error("Bidding has not started yet.");
      return;
    }

    const token = sessionStorage.getItem("token");
    const loggedInUserId = sessionStorage.getItem("loggedInUserId");

    setIsSubmittingBid(true);
    try {
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
        toast.success("Bid placed successfully. Wallet balances were updated by the server.");
        setBidAmount("");
        mergeBid(placedBid);
      } else {
        toast.error(await getResponseMessage(response, "Failed to place bid."));
      }
    } catch (error) {
      console.error("Bid error:", error);
      toast.error("Error connecting to server.");
    } finally {
      setIsSubmittingBid(false);
    }
  };

  const formatCurrency = (value) => `Rs. ${Number(value || 0).toLocaleString("en-IN")}`;
  const formatDateTime = (dateValue) => {
    if (!dateValue) return "-";
    return new Date(dateValue).toLocaleString("en-IN", {
      day: "numeric",
      month: "short",
      hour: "2-digit",
      minute: "2-digit"
    });
  };

  if (isLoading && !product) {
    return (
      <div className="main-content container text-center py-5">
        <LoadingState message="Loading product details..." />
      </div>
    );
  }

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
              <span className="fw-bold text-success fs-5">{formatCurrency(product.basePrice)}</span>
            </div>
            <div className="text-end">
              <span className="text-secondary small d-block">Current Bid</span>
              <span className="fw-bold text-primary fs-5">{formatCurrency(product.currentBid)}</span>
            </div>
          </div>

          <form onSubmit={handleBidSubmit} className="border p-3 rounded bg-white shadow-sm">
            <div className="mb-3">
              <label className="form-label small text-secondary fw-semibold">Place Your Bid (INR)</label>
              <input
                type="number"
                className="form-control"
                placeholder={`Higher than ${formatCurrency(product.currentBid)}`}
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
              loading={isSubmittingBid}
              loadingText="Placing..."
            />
          </form>
        </div>
      </div>

      <div className="card border shadow-sm mt-4">
        <div className="card-header bg-white d-flex justify-content-between align-items-center">
          <div>
            <h5 className="fw-bold mb-0">Live Bid History</h5>
            <span className="text-muted small">Highest bids appear first and update without refreshing.</span>
          </div>
          <span className="badge bg-primary-subtle text-primary border border-primary-subtle">{bids.length} bids</span>
        </div>
        <div className="table-responsive">
          <table className="table align-middle mb-0 table-hover">
            <thead className="table-light">
              <tr className="text-muted">
                <th className="px-4 py-3">Rank</th>
                <th className="py-3">Bidder</th>
                <th className="py-3">Bid Amount</th>
                <th className="px-4 py-3">Time</th>
              </tr>
            </thead>
            <tbody>
              {bids.length === 0 ? (
                <tr>
                  <td colSpan="4" className="text-center text-muted py-4">No bids placed yet.</td>
                </tr>
              ) : (
                bids.map((bid, index) => (
                  <tr key={bid.id}>
                    <td className="px-4 py-3 fw-bold text-secondary">#{index + 1}</td>
                    <td className="py-3 fw-semibold">{bid.bidderName}</td>
                    <td className="py-3 fw-bold text-primary">{formatCurrency(bid.amount)}</td>
                    <td className="px-4 py-3 text-muted">{formatDateTime(bid.bidTime)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default ProductDescription;
