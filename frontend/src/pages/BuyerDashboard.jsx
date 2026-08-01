import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import ProductCard from "../components/ProductCard";
import Button from "../components/Button";
import "../styles/BuyerDashboard.css";

import share from "../assets/share-white.png";
import { apiFetch } from "../api/client";
import { createRequestCache } from "../api/resources";
import { createWebSocketClient } from "../utils/websocket";
import { isAuctionOpen } from "../utils/auctionStatus";

function BuyerDashboard() {
  const navigate = useNavigate();
  const [auctions, setAuctions] = useState([]);
  const userName = sessionStorage.getItem("loggedInUserName") || "Buyer";

  const loadAuctions = useCallback(async () => {
    try {
      const token = sessionStorage.getItem("token");
      const response = await apiFetch("/products", {
        headers: {
          "Authorization": token ? `Bearer ${token}` : ""
        }
      });
      if (response.ok) {
        const data = await response.json();
        const highestBids = await createRequestCache()
          .getHighestBids(data.map((product) => product.productId))
          .catch(() => ({}));
        const mapped = data.map(p => ({
          id: p.productId,
          name: p.name,
          description: p.description,
          image: p.imageUrl,
          basePrice: p.basePrice,
          currentBid: highestBids[p.productId]?.amount || p.currentHighestBid || p.basePrice,
          endTime: p.auctionEndTime,
          startTime: p.auctionStartTime,
          status: p.status
        }));
        const activeAuctions = mapped.filter((auction) => isAuctionOpen(auction));
        setAuctions(activeAuctions);
      }
    } catch (error) {
      console.error("Error fetching auctions:", error);
    }
  }, []);

  useEffect(() => {
    // The callback performs external data loading and updates state after its async responses.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadAuctions();
  }, [loadAuctions]);

  const auctionIdsKey = auctions.map((auction) => auction.id).join(",");

  useEffect(() => {
    const auctionIds = auctionIdsKey ? auctionIdsKey.split(",") : [];
    if (auctionIds.length === 0) {
      return undefined;
    }

    const disconnect = createWebSocketClient((bidUpdate) => {
      const nextBid = Number(bidUpdate.amount);
      if (!Number.isFinite(nextBid)) {
        return;
      }
      setAuctions((currentAuctions) => currentAuctions.map((auction) => {
        if (String(auction.id) !== String(bidUpdate.auctionId)
            || nextBid <= Number(auction.currentBid)) {
          return auction;
        }
        return { ...auction, currentBid: nextBid };
      }));
    }, auctionIds);

    return disconnect;
  }, [auctionIdsKey]);

  return (
    <>
      <div className="main-content dashboard-container">
        {/* Header Section */}
        <div className="dashboard-header">
          <div>
            <h1 className="dashboard-title">Buyer Dashboard</h1>
            <br />

            <p className="welcome-text">Welcome, {userName}</p>

            <p className="user-subtext">
              Browse active auctions and place your bids.
            </p>
          </div>

          <div style={{ width: "200px" }}>
            <Button
              color={"var(--green-primary)"}
              logo={share}
              hover={"green"}
              text={"My Orders"}
              onClick={() => navigate("/orders")}
            />
          </div>
        </div>

        {/* Active Auctions */}
        <div className="section-header">
          <h2>Active Auctions</h2>
        </div>

        {/* Product Cards */}
        <div className="auctions-grid">
          {auctions.map((auction) => (
            <ProductCard
              key={auction.id}
              product={auction}
              onBidPlaced={loadAuctions}
            />
          ))}
          {auctions.length === 0 && (
            <div className="text-muted py-5 text-center w-100 grid-span-4" style={{ gridColumn: "1 / -1" }}>
              No auctions available.
            </div>
          )}
        </div>
        <div className="view-all-container">
          <div style={{ width: "210px" }}>
            <Button
              color={"var(--blue-primary)"}
              logo={share}
              hover={"blue"}
              text={"View All Auctions"}
              onClick={loadAuctions}
            />
          </div>
        </div>
      </div>
    </>
  );
}

export default BuyerDashboard;
