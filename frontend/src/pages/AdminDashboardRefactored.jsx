import { useState, useEffect, useCallback } from "react";
import { toast } from "react-toastify";
import { apiFetch, getResponseMessage } from "../api/client";
import { createRequestCache } from "../api/resources";
import { useConfirm } from "../components/confirmContext";
import ReloadButton from "../components/ReloadButton";
import AdminStatsCards from "../components/admin/AdminStatsCards";
import AdminTabs from "../components/admin/AdminTabs";
import AdminEditForms from "../components/admin/AdminEditForms";
import AdminDataTables from "../components/admin/AdminDataTables";
import { getAuctionPhase } from "../utils/auctionStatus";

function AdminDashboard() {
  const [categories, setCategories] = useState([]);
  const [users, setUsers] = useState([]);
  const [auctions, setAuctions] = useState([]);
  const [bids, setBids] = useState([]);
  const [orders, setOrders] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [isLoadingData, setIsLoadingData] = useState(true);
  const [isCategorySaving, setIsCategorySaving] = useState(false);
  const [deletingCategoryId, setDeletingCategoryId] = useState(null);
  const [isUserSaving, setIsUserSaving] = useState(false);
  const [deletingUserId, setDeletingUserId] = useState(null);
  const [isProductSaving, setIsProductSaving] = useState(false);
  const [deletingProductId, setDeletingProductId] = useState(null);
  const confirm = useConfirm();
  
  // Navigation State
  const [activeView, setActiveView] = useState("categories");

  // Category Edit State
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [editingId, setEditingId] = useState(null);

  // User Edit State
  const [editingUser, setEditingUser] = useState(null);
  const [uName, setUName] = useState("");
  const [uEmail, setUEmail] = useState("");
  const [uRole, setURole] = useState("BUYER");

  // Product Edit State
  const [editingProduct, setEditingProduct] = useState(null);
  const [pName, setPName] = useState("");
  const [pCategory, setPCategory] = useState("");
  const [pBasePrice, setPBasePrice] = useState("");
  const [pEndTime, setPEndTime] = useState("");

  // Load dynamic data from DB
  const loadData = useCallback(async () => {
    setIsLoadingData(true);
    const token = sessionStorage.getItem("token");
    const requestCache = createRequestCache();

    let categoryList = [];
    try {
      const response = await apiFetch("/categories", {
        headers: {
          "Authorization": token ? `Bearer ${token}` : ""
        }
      });
      if (response.ok) {
        categoryList = await response.json();
        setCategories(categoryList);
      } else {
        setCategories([]);
        console.error("Error loading categories: request failed", response.status);
      }
    } catch (e) {
      setCategories([]);
      console.error("Error loading categories:", e);
    }

    // Fetch users
    try {
      const response = await apiFetch("/users", {
        headers: {
          "Authorization": token ? `Bearer ${token}` : ""
        }
      });
      if (response.ok) {
        const uData = await response.json();
        setUsers(uData);
      }
    } catch (e) {
      console.error("Error loading users:", e);
    }

    // Fetch products
    try {
      const response = await apiFetch("/products", {
        headers: {
          "Authorization": token ? `Bearer ${token}` : ""
        }
      });
      if (response.ok) {
        const pData = await response.json();
        const mapped = pData.map(p => ({
          id: p.productId,
          name: p.name,
          description: p.description,
          image: p.imageUrl,
          basePrice: p.basePrice,
          currentBid: p.currentHighestBid || p.basePrice,
          categoryId: p.categoryId,
          endTime: p.auctionEndTime,
          startTime: p.auctionStartTime,
          status: getAuctionPhase(p),
          category: categoryList.find((category) => Number(category.id) === Number(p.categoryId))?.name || "Uncategorized"
        }));
        setAuctions(mapped);
      }
    } catch (e) {
      console.error("Error loading products:", e);
    }

    // Fetch bids
    try {
      const response = await apiFetch("/bids", {
        headers: {
          "Authorization": token ? `Bearer ${token}` : ""
        }
      });
      if (response.ok) {
        const bidsData = await response.json();
        const mappedBids = bidsData.map(b => ({
          id: b.id,
          bidderName: b.bidderName,
          bidAmount: b.amount,
          bidTime: b.bidTime
        }));
        setBids(mappedBids);
      }
    } catch (e) {
      console.error("Error loading bids:", e);
    }

    // Fetch orders
    try {
      const response = await apiFetch("/orders", {
        headers: {
          "Authorization": token ? `Bearer ${token}` : ""
        }
      });
      if (response.ok) {
        const oData = await response.json();
        const mapped = await Promise.all(oData.map(async (order) => {
          let productName = "Product";
          let buyerName = "Buyer";
          let sellerName = "Seller";

          // Fetch product
          try {
            const prodData = await requestCache.getProduct(order.productId);
            if (prodData) productName = prodData.name;
          } catch (error) {
            console.error("Error loading order product:", error);
          }

          // Fetch buyer
          try {
            const buyerData = await requestCache.getUser(order.buyerId);
            if (buyerData) buyerName = buyerData.name;
          } catch (error) {
            console.error("Error loading order buyer:", error);
          }

          // Fetch seller
          try {
            const sellerData = await requestCache.getUser(order.sellerId);
            if (sellerData) sellerName = sellerData.name;
          } catch (error) {
            console.error("Error loading order seller:", error);
          }

          return {
            ...order,
            productName,
            buyerName,
            sellerName
          };
        }));
        setOrders(mapped);
      }
    } catch (e) {
      console.error("Error loading orders in admin:", e);
    }
    setIsLoadingData(false);
  }, []);

  useEffect(() => {
    // The callback performs external data loading and updates state after its async responses.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  // ---------------- CATEGORY CRUD ----------------
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim() || !description.trim()) {
      toast.error("Please fill in both category name and description.");
      return;
    }

    const token = sessionStorage.getItem("token");
    const isEditing = editingId !== null;

    setIsCategorySaving(true);
    try {
      const response = await apiFetch(
        isEditing ? `/categories/${editingId}` : "/categories",
        {
          method: isEditing ? "PUT" : "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": token ? `Bearer ${token}` : ""
          },
          body: JSON.stringify({
            name: name.trim(),
            description: description.trim()
          })
        }
      );

      if (!response.ok) {
        toast.error(await getResponseMessage(response, "Failed to save category."));
        return;
      }

      toast.success(isEditing ? "Category updated successfully." : "Category added successfully.");
      handleCancelEdit();
      await loadData();
    } catch (error) {
      console.error("Category save error:", error);
      toast.error("Unable to save the category. Please try again.");
    } finally {
      setIsCategorySaving(false);
    }
  };

  const handleEditClick = (cat) => {
    setEditingId(cat.id);
    setName(cat.name);
    setDescription(cat.description);
    window.scrollTo({ top: 300, behavior: "smooth" });
  };


  const handleDeleteClick = async (id) => {
    const confirmed = await confirm({
      title: "Delete category?",
      message: "Products using this category may no longer display its name.",
      confirmLabel: "Delete category",
      danger: true
    });
    if (!confirmed) return;

    setDeletingCategoryId(id);
    const token = sessionStorage.getItem("token");
    try {
      const response = await apiFetch(`/categories/${id}`, {
        method: "DELETE",
        headers: {
          "Authorization": token ? `Bearer ${token}` : ""
        }
      });

      if (!response.ok) {
        toast.error(await getResponseMessage(response, "Failed to delete category."));
        return;
      }

      toast.success("Category deleted successfully.");
      if (editingId === id) {
        handleCancelEdit();
      }
      await loadData();
    } catch (error) {
      console.error("Category delete error:", error);
      toast.error("Unable to delete the category. Please try again.");
    } finally {
      setDeletingCategoryId(null);
    }
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setName("");
    setDescription("");
  };

  // ---------------- USER CRUD ----------------
  const handleEditUserClick = (usr) => {
    setEditingUser(usr);
    setUName(usr.name);
    setUEmail(usr.email);
    setURole(usr.role);
    window.scrollTo({ top: 300, behavior: "smooth" });
  };

  const handleUpdateUserSubmit = async (e) => {
    e.preventDefault();
    if (!uName.trim() || !uEmail.trim()) {
      toast.error("Name and email cannot be empty.");
      return;
    }
    const token = sessionStorage.getItem("token");
    setIsUserSaving(true);
    try {
      const response = await apiFetch(`/users/${editingUser.id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          "Authorization": token ? `Bearer ${token}` : ""
        },
        body: JSON.stringify({
          name: uName,
          email: uEmail,
          role: uRole,
          enabled: editingUser.enabled !== undefined ? editingUser.enabled : true
        })
      });
      if (response.ok) {
        toast.success("User updated successfully.");
        setEditingUser(null);
        await loadData();
      } else {
        toast.error(await getResponseMessage(response, "Failed to update user."));
      }
    } catch (err) {
      console.error(err);
      toast.error("Unable to update the user. Please try again.");
    } finally {
      setIsUserSaving(false);
    }
  };

  const handleDeleteUser = async (id) => {
    const loggedInUserId = sessionStorage.getItem("loggedInUserId");
    if (String(id) === String(loggedInUserId)) {
      toast.error("You cannot delete your own account.");
      return;
    }

    const confirmed = await confirm({
      title: "Delete user?",
      message: "This action cannot be undone.",
      confirmLabel: "Delete user",
      danger: true
    });
    if (!confirmed) return;

    setDeletingUserId(id);
    const token = sessionStorage.getItem("token");
    try {
      const response = await apiFetch(`/users/${id}`, {
        method: "DELETE",
        headers: {
          "Authorization": token ? `Bearer ${token}` : ""
        }
      });
      if (response.ok) {
        toast.success("User deleted successfully.");
        await loadData();
        if (editingUser && editingUser.id === id) {
          setEditingUser(null);
        }
      } else {
        toast.error(await getResponseMessage(response, "Failed to delete user."));
      }
    } catch (err) {
      console.error(err);
      toast.error("Unable to delete the user. Please try again.");
    } finally {
      setDeletingUserId(null);
    }
  };

  const handleCancelUserEdit = () => {
    setEditingUser(null);
    setUName("");
    setUEmail("");
    setURole("BUYER");
  };

  // ---------------- PRODUCT CRUD ----------------
  const handleEditProductClick = (prod) => {
    setEditingProduct(prod);
    setPName(prod.name);
    setPCategory(prod.categoryId ? String(prod.categoryId) : "");
    setPBasePrice(prod.basePrice);
    setPEndTime(prod.endTime);
    window.scrollTo({ top: 300, behavior: "smooth" });
  };

  const handleUpdateProductSubmit = async (e) => {
    e.preventDefault();
    if (!pName.trim()) {
      toast.error("Product name cannot be empty.");
      return;
    }
    const token = sessionStorage.getItem("token");
    setIsProductSaving(true);
    try {
      const response = await apiFetch("/products", {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          "Authorization": token ? `Bearer ${token}` : ""
        },
        body: JSON.stringify({
          productId: editingProduct.id,
          name: pName,
          basePrice: Number(pBasePrice),
          categoryId: pCategory ? Number(pCategory) : null,
          auctionEndTime: pEndTime
        })
      });
      if (response.ok) {
        toast.success("Product updated successfully.");
        setEditingProduct(null);
        await loadData();
      } else {
        toast.error(await getResponseMessage(response, "Failed to update product."));
      }
    } catch (err) {
      console.error(err);
      toast.error("Unable to update the product. Please try again.");
    } finally {
      setIsProductSaving(false);
    }
  };

  const handleDeleteProduct = async (id) => {
    const confirmed = await confirm({
      title: "Delete product?",
      message: "This action cannot be undone.",
      confirmLabel: "Delete product",
      danger: true
    });
    if (!confirmed) return;

    setDeletingProductId(id);
    const token = sessionStorage.getItem("token");
    try {
      const response = await apiFetch(`/products/${id}`, {
        method: "DELETE",
        headers: {
          "Authorization": token ? `Bearer ${token}` : ""
        }
      });
      if (response.ok) {
        toast.success("Product deleted successfully.");
        await loadData();
        if (editingProduct && editingProduct.id === id) {
          setEditingProduct(null);
        }
      } else {
        toast.error(await getResponseMessage(response, "Failed to delete product."));
      }
    } catch (err) {
      console.error(err);
      toast.error("Unable to delete the product. Please try again.");
    } finally {
      setDeletingProductId(null);
    }
  };

  const handleCancelProductEdit = () => {
    setEditingProduct(null);
    setPName("");
    setPCategory("");
    setPBasePrice("");
    setPEndTime("");
  };

  return (
    <div className="main-content container py-4 text-start" style={{ maxWidth: "1200px" }}>
      {/* Title Header */}
      <div className="d-flex justify-content-between align-items-start gap-3 mb-4 flex-wrap">
        <div>
          <h1 className="fw-bold mb-1" style={{ fontSize: "2.2rem" }}>Admin Dashboard</h1>
          <p className="text-muted mb-0">Welcome back, Admin! Here's what's happening with your platform.</p>
        </div>
        <ReloadButton onClick={loadData} loading={isLoadingData} label="Reload data" />
      </div>

      <AdminStatsCards
        activeView={activeView}
        setActiveView={setActiveView}
        userCount={users.length}
        productCount={auctions.length}
        bidCount={bids.length}
      />

      <AdminTabs activeView={activeView} setActiveView={setActiveView} />

      <AdminEditForms
        activeView={activeView}
        editingId={editingId}
        name={name}
        setName={setName}
        description={description}
        setDescription={setDescription}
        onCategorySubmit={handleSubmit}
        isCategorySaving={isCategorySaving}
        onCancelCategory={handleCancelEdit}
        editingUser={editingUser}
        userName={uName}
        setUserName={setUName}
        userEmail={uEmail}
        setUserEmail={setUEmail}
        userRole={uRole}
        setUserRole={setURole}
        onUserSubmit={handleUpdateUserSubmit}
        isUserSaving={isUserSaving}
        onCancelUser={handleCancelUserEdit}
        editingProduct={editingProduct}
        productName={pName}
        setProductName={setPName}
        productCategory={pCategory}
        setProductCategory={setPCategory}
        productCategories={categories}
        productBasePrice={pBasePrice}
        setProductBasePrice={setPBasePrice}
        productEndTime={pEndTime}
        setProductEndTime={setPEndTime}
        onProductSubmit={handleUpdateProductSubmit}
        isProductSaving={isProductSaving}
        onCancelProduct={handleCancelProductEdit}
      />

      <AdminDataTables
        activeView={activeView}
        categories={categories}
        users={users}
        products={auctions}
        bids={bids}
        orders={orders}
        statusFilter={statusFilter}
        setStatusFilter={setStatusFilter}
        onEditCategory={handleEditClick}

        onDeleteCategory={handleDeleteClick}
        onEditUser={handleEditUserClick}
        onDeleteUser={handleDeleteUser}
        onEditProduct={handleEditProductClick}
        onDeleteProduct={handleDeleteProduct}
        isLoading={isLoadingData}
        deletingCategoryId={deletingCategoryId}
        deletingUserId={deletingUserId}
        deletingProductId={deletingProductId}
      />
    </div>
  );
}

export default AdminDashboard;
