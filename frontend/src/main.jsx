import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.js";
import "react-toastify/dist/ReactToastify.css";
globalThis.global = globalThis;
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.jsx";
import "./App.css";
import { ToastContainer } from "react-toastify";
import { ConfirmProvider } from "./components/ConfirmDialog";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <ConfirmProvider>
      <App />
      <ToastContainer position="top-right" autoClose={3500} newestOnTop pauseOnFocusLoss theme="colored" />
    </ConfirmProvider>
  </React.StrictMode>
);
