function AdminTabs({ activeView, setActiveView }) {
  const tabs = [
    ["categories", "Categories"],
    ["users", "Users"],
    ["products", "Products"],
    ["bids", "Bids"],
    ["orders", "Orders"],
  ];

  return (
    <div className="mb-4">
      <ul className="nav nav-pills border-bottom pb-3 gap-2">
        {tabs.map(([key, label]) => (
          <li className="nav-item" key={key}>
            <button
              className={`nav-link px-4 py-2 fw-semibold ${activeView === key ? "active bg-black text-white" : "text-dark bg-light border"}`}
              onClick={() => setActiveView(key)}
              style={{ borderRadius: "8px" }}
            >
              {label}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default AdminTabs;
