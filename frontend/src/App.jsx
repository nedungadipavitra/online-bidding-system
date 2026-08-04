import { lazy, Suspense } from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";

import Navbar from "./components/Navbar";
import ProtectedRoute from "./components/ProtectedRoute";

const Home = lazy(() => import("./pages/Home"));
const SellerDashboard = lazy(() => import("./pages/SellerDashboard"));
const Login = lazy(() => import("./pages/Login"));
const Register = lazy(() => import("./pages/Register"));
const BuyerDashboard = lazy(() => import("./pages/BuyerDashboard"));
const ProductDescription = lazy(() => import("./pages/ProductDescription"));
const DeliveryDashboard = lazy(() => import("./pages/DeliveryDashboard"));
const MyWallet = lazy(() => import("./pages/MyWallet"));
const AdminDashboard = lazy(() => import("./pages/AdminDashboard"));
const MyOrders = lazy(() => import("./pages/MyOrders"));

function App() {
  return (
    <Router>
      <div className="app-container">
        {/* Background backdrop elements */}
        <div className="home-blob home-blob-1"></div>
        <div className="home-blob home-blob-2"></div>
        <div className="home-blob home-blob-3"></div>
        <div className="home-pattern"></div>

        <Navbar />

        <div className="app-content">
          <Suspense fallback={<div className="main-content container py-5 text-center">Loading...</div>}>
            <Routes>
            <Route path="/" element={<Home />} />

            <Route
              path="/seller/dashboard"
              element={
                <ProtectedRoute allowedRoles={["SELLER"]}>
                  <div className="container">
                    <SellerDashboard />
                  </div>
                </ProtectedRoute>
              }
            />

            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            
            <Route
              path="/buyer/dashboard"
              element={
                <ProtectedRoute allowedRoles={["BUYER"]}>
                  <BuyerDashboard />
                </ProtectedRoute>
              }
            />

            <Route
              path="/delivery"
              element={
                <ProtectedRoute allowedRoles={["DELIVERY"]}>
                  <DeliveryDashboard />
                </ProtectedRoute>
              }
            />

            <Route
              path="/buyer-wallet"
              element={
                <ProtectedRoute allowedRoles={["BUYER"]}>
                  <MyWallet role="buyer" />
                </ProtectedRoute>
              }
            />

            <Route
              path="/seller-wallet"
              element={
                <ProtectedRoute allowedRoles={["SELLER"]}>
                  <MyWallet role="seller" />
                </ProtectedRoute>
              }
            />

            <Route
              path="/admin/dashboard"
              element={
                <ProtectedRoute allowedRoles={["ADMIN"]}>
                  <AdminDashboard />
                </ProtectedRoute>
              }
            />

            <Route
              path="/orders"
              element={
                <ProtectedRoute allowedRoles={["BUYER"]}>
                  <MyOrders />
                </ProtectedRoute>
              }
            />

            <Route path="/product/:id" element={<ProductDescription />} />
            </Routes>
          </Suspense>
        </div>
      </div>
    </Router>
  );
}

export default App;
