import { useNavigate } from "react-router-dom";
import iphone from "../assets/iphone.jpeg";
import Button from "./Button";
import bid from "../assets/bid.png";

const fallbackProduct = {
  id: 1,
  name: "iPhone 17 Pro",
  category: "Electronics",
  basePrice: 70000,
  currentBid: 85250,
  description: "iPhone 17 Pro. Exceptional, new Center Stage front camera.",
  image: "",
  endTime: "2026-08-30T22:00",
  status: "ACTIVE"
};

function ProductCard({ product = fallbackProduct }) {
  const navigate = useNavigate();

  const handleCardClick = () => {
    navigate(`/product/${product.id}`);
  };

  return (
    <div className="auction-card mx-2 my-2" style={{ width: "290px", cursor: "pointer" }} onClick={handleCardClick}>
      <img
        src={product.imageSrc || product.image || iphone}
        alt={product.name || product.title}
        className="auction-image"
        style={{ height: "160px", objectFit: "cover" }}
      />
      <div className="card-content">
        <h3 className="fs-5 fw-bold text-center text-truncate">{product.name || product.title}</h3>
        <p className="description text-muted text-center" style={{ fontSize: "0.85rem", height: "60px", overflow: "hidden" }}>
          {product.description}
        </p>
        {product.startTime && (
          <div className="text-center text-secondary fw-semibold mt-1" style={{ fontSize: "0.75rem" }}>
            ⏰ Starts: {new Date(product.startTime).toLocaleString("en-IN", { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
          </div>
        )}
        <hr />
        <div className="price-row">
          <div>
            <div style={{ fontSize: "0.75rem", color: "var(--text-secondary)" }} className="fw-bold">
              Base Price
            </div>
            <div className="base-price">₹{product.basePrice.toLocaleString()}</div>
          </div>
          <div className="text-end">
            <div style={{ fontSize: "0.75rem", color: "var(--text-secondary)" }} className="fw-bold">
              Current Bid
            </div>
            <div className="current-bid">₹{product.currentBid.toLocaleString()}</div>
          </div>
        </div>
        <div className="mt-3" onClick={(e) => e.stopPropagation()}>
          <Button
            color={"var(--blue-primary)"}
            logo={bid}
            hover={"blue"}
            text={"View & Bid"}
            onClick={handleCardClick}
          />
        </div>
      </div>
    </div>
  );
}

export default ProductCard;
