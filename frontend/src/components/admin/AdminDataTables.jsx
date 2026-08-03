import LoadingState from "../LoadingState";

function TableShell({ title, children }) {
  return (
    <div className="card p-4 border shadow-sm">
      <h4 className="fw-bold mb-3">{title}</h4>
      <div className="table-responsive">{children}</div>
    </div>
  );
}

function EmptyRow({ colSpan, children }) {
  return (
    <tr>
      <td colSpan={colSpan} className="text-center py-4 text-muted">{children}</td>
    </tr>
  );
}

export function CategoriesTable({ categories, onEdit, onDelete, deletingCategoryId }) {
  return (
    <TableShell title="Categories">
      <table className="table align-middle table-hover">
        <thead className="table-light"><tr className="text-muted"><th>ID</th><th>Name</th><th>Description</th><th className="text-center">Actions</th></tr></thead>
        <tbody>
          {categories.map((category) => (
            <tr key={category.id}>
              <td className="fw-bold text-secondary">{category.id}</td>
              <td className="fw-semibold">{category.name}</td>
              <td className="text-muted">{category.description}</td>
              <td className="text-center">
                <button className="btn btn-outline-primary btn-sm me-2" onClick={() => onEdit(category)} disabled={deletingCategoryId === category.id}>Edit</button>
                <button className="btn btn-outline-danger btn-sm" onClick={() => onDelete(category.id)} disabled={deletingCategoryId === category.id}>
                  {deletingCategoryId === category.id ? "Deleting..." : "Delete"}
                </button>
              </td>
            </tr>
          ))}
          {categories.length === 0 && <EmptyRow colSpan="4">No categories configured.</EmptyRow>}
        </tbody>
      </table>
    </TableShell>
  );
}

export function UsersTable({ users, onEdit, onDelete, deletingUserId }) {
  return (
    <TableShell title="Users">
      <table className="table align-middle table-hover">
        <thead className="table-light"><tr className="text-muted"><th>User ID</th><th>Name</th><th>Email</th><th>Role</th><th className="text-center">Actions</th></tr></thead>
        <tbody>
          {users.map((user) => (
            <tr key={user.id}>
              <td className="fw-bold text-secondary">{user.id}</td>
              <td className="fw-semibold">{user.name}</td>
              <td className="text-muted">{user.email}</td>
              <td><span className={`badge ${user.role === "ADMIN" ? "bg-danger" : user.role === "SELLER" ? "bg-success" : user.role === "DELIVERY" ? "bg-warning text-dark" : "bg-primary"}`}>{user.role}</span></td>
              <td className="text-center">
                <button className="btn btn-outline-primary btn-sm me-2" onClick={() => onEdit(user)} disabled={deletingUserId === user.id}>Update</button>
                <button className="btn btn-outline-danger btn-sm" onClick={() => onDelete(user.id)} disabled={deletingUserId === user.id}>
                  {deletingUserId === user.id ? "Deleting..." : "Delete"}
                </button>
              </td>
            </tr>
          ))}
          {users.length === 0 && <EmptyRow colSpan="5">No users found.</EmptyRow>}
        </tbody>
      </table>
    </TableShell>
  );
}

export function ProductsTable({ products, onEdit, onDelete, deletingProductId }) {
  return (
    <TableShell title="Products (Auctions)">
      <table className="table align-middle table-hover">
        <thead className="table-light"><tr className="text-muted"><th>Product ID</th><th>Name</th><th>Category</th><th>Base Price</th><th>End Time</th><th>Status</th><th className="text-center">Actions</th></tr></thead>
        <tbody>
          {products.map((product) => (
            <tr key={product.id}>
              <td className="fw-bold text-secondary">{product.id}</td>
              <td className="fw-semibold">{product.name}</td>
              <td className="text-muted">{product.category}</td>
              <td>₹{product.basePrice.toLocaleString("en-IN")}</td>
              <td className="text-muted">{new Date(product.endTime).toLocaleString("en-IN", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" })}</td>
              <td><span className={`badge ${product.status === "ACTIVE" ? "bg-success" : "bg-secondary"}`}>{product.status}</span></td>
              <td className="text-center">
                <button className="btn btn-outline-primary btn-sm me-2" onClick={() => onEdit(product)} disabled={deletingProductId === product.id}>Update</button>
                <button className="btn btn-outline-danger btn-sm" onClick={() => onDelete(product.id)} disabled={deletingProductId === product.id}>
                  {deletingProductId === product.id ? "Deleting..." : "Delete"}
                </button>
              </td>
            </tr>
          ))}
          {products.length === 0 && <EmptyRow colSpan="7">No products found.</EmptyRow>}
        </tbody>
      </table>
    </TableShell>
  );
}

export function BidsTable({ bids }) {
  return (
    <TableShell title="Bids">
      <table className="table align-middle table-hover">
        <thead className="table-light"><tr className="text-muted"><th>Bid ID</th><th>Product Name</th><th>Bidder Name</th><th>Bid Amount</th><th>Time</th></tr></thead>
        <tbody>
          {bids.map((bid) => (
            <tr key={bid.id}>
              <td className="fw-bold text-secondary">{bid.id}</td>
              <td className="fw-semibold">{bid.productName}</td>
              <td className="text-muted">{bid.bidderName}</td>
              <td className="text-success fw-bold">₹{bid.bidAmount.toLocaleString("en-IN")}</td>
              <td className="text-secondary">{new Date(bid.bidTime).toLocaleString("en-IN", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" })}</td>
            </tr>
          ))}
          {bids.length === 0 && <EmptyRow colSpan="5">No bids found.</EmptyRow>}
        </tbody>
      </table>
    </TableShell>
  );
}

export function OrdersTable({ orders, statusFilter, setStatusFilter }) {
  const filteredOrders = orders.filter((order) => statusFilter === "ALL" || (order.status || order.deliveryStatus || "").toUpperCase() === statusFilter);

  return (
    <TableShell title="Orders">
      <div className="d-flex justify-content-end align-items-center gap-2 mb-3">
        <label className="small fw-semibold text-muted mb-0">Filter Status:</label>
        <select className="form-select form-select-sm" style={{ width: "150px" }} value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
          <option value="ALL">All Statuses</option>
          <option value="PENDING">PENDING</option>
          <option value="ASSIGNED">ASSIGNED</option>
          <option value="DISPATCHED">DISPATCHED</option>
          <option value="DELIVERED">DELIVERED</option>
        </select>
      </div>
      <table className="table align-middle table-hover">
        <thead className="table-light"><tr className="text-muted"><th>Order ID</th><th>Product Name</th><th>Buyer Name</th><th>Seller Name</th><th>Price</th><th>Delivery Partner</th><th className="text-center">Status</th></tr></thead>
        <tbody>
          {filteredOrders.map((order) => (
            <tr key={order.id}>
              <td className="fw-bold text-secondary">{order.id}</td>
              <td className="fw-semibold">{order.productName}</td>
              <td className="text-muted">{order.buyerName}</td>
              <td className="text-muted">{order.sellerName}</td>
              <td className="fw-semibold text-success">₹{order.finalPrice.toLocaleString("en-IN")}</td>
              <td className="text-secondary fw-semibold">{order.deliveryPersonName || "Not Assigned"}</td>
              <td className="text-center"><span className={`badge rounded-pill ${order.status === "DELIVERED" ? "bg-success" : order.status === "DISPATCHED" ? "bg-primary" : "bg-warning text-dark"}`}>{order.status}</span></td>
            </tr>
          ))}
          {filteredOrders.length === 0 && <EmptyRow colSpan="7">No orders found matching the filter.</EmptyRow>}
        </tbody>
      </table>
    </TableShell>
  );
}

function AdminDataTables({
  activeView,
  categories,
  users,
  products,
  bids,
  orders,
  statusFilter,
  setStatusFilter,
  onEditCategory,
  onDeleteCategory,
  onEditUser,
  onDeleteUser,
  onEditProduct,
  onDeleteProduct,
  isLoading,
  deletingCategoryId,
  deletingUserId,
  deletingProductId
}) {
  if (isLoading) {
    return <LoadingState message="Loading admin data..." compact />;
  }

  switch (activeView) {
    case "categories":
      return <CategoriesTable categories={categories} onEdit={onEditCategory} onDelete={onDeleteCategory} deletingCategoryId={deletingCategoryId} />;
    case "users":
      return <UsersTable users={users} onEdit={onEditUser} onDelete={onDeleteUser} deletingUserId={deletingUserId} />;
    case "products":
      return <ProductsTable products={products} onEdit={onEditProduct} onDelete={onDeleteProduct} deletingProductId={deletingProductId} />;
    case "bids":
      return <BidsTable bids={bids} />;
    case "orders":
      return <OrdersTable orders={orders} statusFilter={statusFilter} setStatusFilter={setStatusFilter} />;
    default:
      return null;
  }
}

export default AdminDataTables;
