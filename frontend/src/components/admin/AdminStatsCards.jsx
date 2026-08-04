function AdminStatsCards({ activeView, setActiveView, userCount, productCount, bidCount }) {
  const cards = [
    { key: "users", label: "Users", count: userCount, color: "var(--blue-primary)", iconColor: "#e8f0fe", textColor: "text-primary", icon: "User" },
    { key: "products", label: "Products", count: productCount, color: "var(--green-primary)", iconColor: "#e6f4ea", textColor: "text-success", icon: "Box" },
    { key: "bids", label: "Bids", count: bidCount, color: "#8a3ffc", iconColor: "#f3e8fd", textColor: "", icon: "Bid" },
  ];

  return (
    <div className="row g-4 mb-4">
      {cards.map((card) => (
        <div
          className="col-12 col-sm-6 col-lg-3"
          key={card.key}
          onClick={() => setActiveView(card.key)}
          style={{ cursor: "pointer" }}
        >
          <div
            className="card h-100 p-3 border shadow-sm d-flex flex-row align-items-center gap-3"
            style={{
              borderColor: activeView === card.key ? card.color : "#dee2e6",
              borderWidth: activeView === card.key ? "2px" : "1px",
              backgroundColor: activeView === card.key ? "#f8f9fa" : "#ffffff",
              transform: activeView === card.key ? "scale(1.02)" : "scale(1)",
              transition: "all 0.2s ease-in-out"
            }}
          >
            <div
              style={{ width: "60px", height: "60px", backgroundColor: card.iconColor, borderRadius: "12px" }}
              className="d-flex align-items-center justify-content-center fs-6 fw-bold"
            >
              {card.icon}
            </div>
            <div>
              <span className="text-muted small fw-semibold text-start d-block">{card.label}</span>
              <h2 className={`fw-bold mb-0 ${card.textColor}`} style={{ color: card.textColor ? undefined : card.color, fontSize: "2rem" }}>
                {card.count}
              </h2>
            </div>
          </div>
        </div>
      ))}

      <div className="col-12 col-sm-6 col-lg-3">
        <div className="card h-100 p-3 border shadow-sm d-flex flex-row align-items-center gap-3">
          <div
            style={{ width: "60px", height: "60px", backgroundColor: "#fef3d6", borderRadius: "12px" }}
            className="d-flex align-items-center justify-content-center fs-3"
          >
            ₹
          </div>
          <div>
            <span className="text-muted small fw-semibold text-start d-block">Revenue</span>
            <h2 className="fw-bold mb-0" style={{ color: "#d97706", fontSize: "2rem" }}>₹0</h2>
          </div>
        </div>
      </div>
    </div>
  );
}

export default AdminStatsCards;
