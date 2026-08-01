import { useState, useEffect } from "react";
import ProductCard from "../components/ProductCard";
import ButtonOutlined from "../components/ButtonOutlined";
import { getAuctions } from "../utils/db";
import share from "../assets/share-blue.png";

function ActiveAuction() {
  const [auctions, setAuctions] = useState([]);

  const loadAuctions = () => {
    const allAuctions = getAuctions();
    const activeAuctions = allAuctions.filter((a) => a.status === "ACTIVE");
    setAuctions(activeAuctions);
  };

  useEffect(() => {
    // The callback performs external data loading and updates state after its async responses.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadAuctions();
  }, []);

  return (
    <div className="container">
      <div className="d-flex justify-content-center">
        <div style={{ width: "200px" }}>
          <h4 className="d-flex justify-content-center">Active Auctions</h4>
          <div className="d-flex justify-content-center my-3">
            <div
              style={{
                borderBottom: "2px solid",
                borderColor: "var(--blue-primary)",
              }}
              className=" w-75"
            ></div>
          </div>
        </div>
      </div>
      <div className="d-flex justify-content-center flex-wrap gap-4 py-3">
        {auctions.map((auction) => (
          <ProductCard
            key={auction.id}
            product={auction}
          />
        ))}
        {auctions.length === 0 && (
          <div className="text-muted py-4 text-center w-100">
            No active auctions available right now.
          </div>
        )}
      </div>
      <div className="d-flex justify-content-center">
        <div className="d-flex my-3">
          <ButtonOutlined
            color={"var(--blue-primary)"}
            logo={share}
            text={"View All Auctions"}
          />
        </div>
      </div>
    </div>
  );
}

export default ActiveAuction;
