import Button from "../Button";

function ActionButtons({ children, onCancel, width = "160px" }) {
  return (
    <div className="d-flex gap-2 mt-3 justify-content-start">
      <div style={{ width }}>{children}</div>
      {onCancel && <button type="button" className="btn btn-outline-secondary px-4 py-2" onClick={onCancel}>Cancel</button>}
    </div>
  );
}

function AdminEditForms({
  activeView,
  editingId,
  name,
  setName,
  description,
  setDescription,
  onCategorySubmit,
  isCategorySaving,
  onCancelCategory,
  editingUser,
  userName,
  setUserName,
  userEmail,
  setUserEmail,
  userRole,
  setUserRole,
  onUserSubmit,
  isUserSaving,
  onCancelUser,
  editingProduct,
  productName,
  setProductName,
  productCategory,
  setProductCategory,
  productCategories,
  productBasePrice,
  setProductBasePrice,
  productEndTime,
  setProductEndTime,
  onProductSubmit,
  isProductSaving,
  onCancelProduct
}) {
  return (
    <>
      {activeView === "categories" && (
        <div className="card p-4 border shadow-sm mb-4">
          <h4 className="fw-bold mb-3">{editingId ? "Edit Category" : "Add Category"}</h4>
          <form onSubmit={onCategorySubmit}>
            <div className="row g-3">
              <div className="col-12 col-md-4 text-start">
                <label htmlFor="catName" className="form-label small fw-semibold">Category Name</label>
                <input id="catName" type="text" placeholder="Enter category name" className="form-control" value={name} onChange={(event) => setName(event.target.value)} required />
              </div>
              <div className="col-12 col-md-8 text-start">
                <label htmlFor="catDesc" className="form-label small fw-semibold">Description</label>
                <textarea id="catDesc" rows="1" placeholder="Enter category description" className="form-control" value={description} onChange={(event) => setDescription(event.target.value)} required />
              </div>
            </div>
            <ActionButtons onCancel={editingId ? onCancelCategory : undefined} width="180px">
              <Button
                color="var(--blue-primary)"
                hover="blue"
                text={editingId ? "Update Category" : "+ Add Category"}
                type="submit"
                loading={isCategorySaving}
                loadingText={editingId ? "Updating..." : "Adding..."}
              />
            </ActionButtons>
          </form>
        </div>
      )}

      {activeView === "users" && editingUser && (
        <div className="card p-4 border shadow-sm mb-4">
          <h4 className="fw-bold mb-3">Edit User: {editingUser.id}</h4>
          <form onSubmit={onUserSubmit}>
            <div className="row g-3">
              <div className="col-12 col-md-4 text-start"><label className="form-label small fw-semibold">Full Name</label><input type="text" className="form-control" value={userName} onChange={(event) => setUserName(event.target.value)} required /></div>
              <div className="col-12 col-md-4 text-start"><label className="form-label small fw-semibold">Email Address</label><input type="email" className="form-control" value={userEmail} onChange={(event) => setUserEmail(event.target.value)} required /></div>
              <div className="col-12 col-md-4 text-start"><label className="form-label small fw-semibold">Role</label><select className="form-select" value={userRole} onChange={(event) => setUserRole(event.target.value)}><option value="BUYER">BUYER</option><option value="SELLER">SELLER</option><option value="DELIVERY">DELIVERY</option><option value="ADMIN">ADMIN</option></select></div>
            </div>
            <ActionButtons onCancel={onCancelUser} width="150px">
              <Button
                color="var(--blue-primary)"
                hover="blue"
                text="Update User"
                type="submit"
                loading={isUserSaving}
                loadingText="Updating..."
              />
            </ActionButtons>
          </form>
        </div>
      )}

      {activeView === "products" && editingProduct && (
        <div className="card p-4 border shadow-sm mb-4">
          <h4 className="fw-bold mb-3">Edit Product: {editingProduct.id}</h4>
          <form onSubmit={onProductSubmit}>
            <div className="row g-3">
              <div className="col-12 col-md-4 text-start"><label className="form-label small fw-semibold">Product Name</label><input type="text" className="form-control" value={productName} onChange={(event) => setProductName(event.target.value)} required /></div>
              <div className="col-12 col-md-4 text-start">
                <label className="form-label small fw-semibold" htmlFor="productCategory">Category</label>
                <select id="productCategory" className="form-select" value={productCategory} onChange={(event) => setProductCategory(event.target.value)} required>
                  <option value="">Select Category</option>
                  {productCategories.map((category) => (
                    <option key={category.id} value={category.id}>{category.name}</option>
                  ))}
                </select>
              </div>
              <div className="col-12 col-md-4 text-start"><label className="form-label small fw-semibold">Base Price (INR)</label><input type="number" className="form-control" value={productBasePrice} onChange={(event) => setProductBasePrice(event.target.value)} required /></div>
              <div className="col-12 col-md-4 text-start"><label className="form-label small fw-semibold">End Time</label><input type="datetime-local" className="form-control" value={productEndTime ? productEndTime.substring(0, 16) : ""} onChange={(event) => setProductEndTime(event.target.value)} required /></div>
            </div>
            <ActionButtons onCancel={onCancelProduct}>
              <Button
                color="var(--blue-primary)"
                hover="blue"
                text="Update Product"
                type="submit"
                loading={isProductSaving}
                loadingText="Updating..."
              />
            </ActionButtons>
          </form>
        </div>
      )}

    </>
  );
}

export default AdminEditForms;
