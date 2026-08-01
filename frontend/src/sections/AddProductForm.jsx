import { useEffect, useState } from "react";
import Button from "../components/Button";
import add from "../assets/add.png";
import { apiFetch, apiJson } from "../api/client";

function AddProductForm({ onProductAdded }) {
  const [name, setName] = useState("");
  const [category, setCategory] = useState("");
  const [basePrice, setBasePrice] = useState("");
  const [description, setDescription] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [image, setImage] = useState("");
  const [dragActive, setDragActive] = useState(false);
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    let active = true;

    apiJson("/categories")
      .then((categoryData) => {
        if (active) {
          setCategories(categoryData);
        }
      })
      .catch((error) => {
        console.error("Error loading categories:", error);
      });

    return () => {
      active = false;
    };
  }, []);

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFile(e.dataTransfer.files[0]);
    }
  };

  const handleFileInput = (e) => {
    if (e.target.files && e.target.files[0]) {
      handleFile(e.target.files[0]);
    }
  };

  const handleFile = (file) => {
    if (!file.type.startsWith("image/")) {
      alert("Please upload an image file.");
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => {
      setImage(e.target.result); // Base64 encoding
    };
    reader.readAsDataURL(file);
  };

  const removeImage = () => {
    setImage("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!name || !category || !basePrice || !description || !startTime || !endTime) {
      alert("Please fill in all fields.");
      return;
    }

    const price = Number(basePrice);
    if (isNaN(price) || price <= 0) {
      alert("Base price must be a positive number.");
      return;
    }

    if (new Date(startTime) >= new Date(endTime)) {
      alert("Auction Start Time must be before Auction End Time.");
      return;
    }

    const token = sessionStorage.getItem("token");
    const loggedInUserId = sessionStorage.getItem("loggedInUserId");

    try {
      const response = await apiFetch("/products", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": token ? `Bearer ${token}` : ""
        },
        body: JSON.stringify({
          name,
          description,
          imageUrl: image || "",
          basePrice: price,
          auctionStartTime: startTime,
          auctionEndTime: endTime,
          sellerId: loggedInUserId ? Number(loggedInUserId) : null,
          categoryId: Number(category)
        })
      });

      if (response.ok) {
        alert("Product added successfully for auction!");
        // Reset Form
        setName("");
        setCategory("");
        setBasePrice("");
        setDescription("");
        setStartTime("");
        setEndTime("");
        setImage("");

        // Trigger state reload in parent
        if (onProductAdded) {
          onProductAdded();
        }
      } else {
        const errorText = await response.text();
        let errorMessage = "Failed to add product";
        try {
          const errJson = JSON.parse(errorText);
          if (errJson.errors && Array.isArray(errJson.errors)) {
            errorMessage = errJson.errors.map(err => err.defaultMessage || JSON.stringify(err)).join(", ");
          } else {
            errorMessage = errJson.message || errJson.error || JSON.stringify(errJson) || errorMessage;
          }
        } catch {
          errorMessage = errorText || errorMessage;
        }
        alert(errorMessage);
      }
    } catch (error) {
      console.error("Add product error:", error);
      alert("Error connecting to server: " + error.message);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <div className="card p-4">
        <h4 className="px-2">Add Product for Auction</h4>
        <div className="container mt-1">
          <div className="row g-3">
            <div className="col-12 col-md-4">
              <label htmlFor="pname" className="form-label">
                Product Name
              </label>
              <input
                type="text"
                id="pname"
                placeholder="Enter product name"
                className="form-control"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            <div className="col-12 col-md-4">
              <label htmlFor="pcategory" className="form-label">
                Select Category
              </label>
              <select
                id="pcategory"
                className="form-select"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                required
              >
                <option value="">Select Category</option>
                {categories.map((item) => (
                  <option key={item.id} value={item.id}>{item.name}</option>
                ))}
              </select>
            </div>

            <div className="col-12 col-md-4">
              <label htmlFor="basePrice" className="form-label">
                Base Price (₹)
              </label>
              <input
                type="number"
                id="basePrice"
                placeholder="Enter base price"
                className="form-control"
                value={basePrice}
                onChange={(e) => setBasePrice(e.target.value)}
                required
              />
            </div>
          </div>
          <div className="row mt-1 g-3">
            <div className="col-12 col-md-6">
              <label htmlFor="pdesc" className="form-label">
                Product Description
              </label>
              <textarea
                style={{ width: "100%" }}
                id="pdesc"
                rows="3"
                placeholder="Product Description"
                className="form-control"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
              ></textarea>
            </div>
            <div className="col-12 col-md-3">
              <label htmlFor="pstarttime" className="form-label">
                Auction Start Time
              </label>
              <input
                type="datetime-local"
                id="pstarttime"
                className="form-control"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                required
              />
            </div>
            <div className="col-12 col-md-3">
              <label htmlFor="pdate" className="form-label">
                Auction End Time
              </label>
              <input
                type="datetime-local"
                id="pdate"
                className="form-control"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                required
              />
            </div>
          </div>

          {/* Image Drag and Drop Zone */}
          <div className="row mt-3 g-3">
            <div className="col-12 text-start">
              <label className="form-label">Product Image</label>
              <div
                className="p-4 border rounded text-center d-flex flex-column align-items-center justify-content-center"
                style={{
                  borderStyle: "dashed",
                  borderWidth: "2px",
                  borderColor: dragActive ? "var(--blue-primary)" : "#ccc",
                  backgroundColor: dragActive ? "#f0f4ff" : "#fafafa",
                  transition: "all 0.2s ease-in-out",
                  minHeight: "150px",
                  position: "relative"
                }}
                onDragEnter={handleDrag}
                onDragOver={handleDrag}
                onDragLeave={handleDrag}
                onDrop={handleDrop}
              >
                {image ? (
                  <div className="d-flex flex-column align-items-center gap-2">
                    <img
                      src={image}
                      alt="Product Preview"
                      style={{ maxWidth: "120px", maxHeight: "120px", objectFit: "contain", borderRadius: "8px" }}
                    />
                    <button
                      type="button"
                      className="btn btn-outline-danger btn-sm px-3"
                      onClick={removeImage}
                    >
                      Remove Image
                    </button>
                  </div>
                ) : (
                  <div className="d-flex flex-column align-items-center gap-2">
                    <span className="fs-2">📤</span>
                    <p className="mb-1 text-muted small">Drag & Drop product image here</p>
                    <p className="mb-2 text-muted small">or</p>
                    <label
                      htmlFor="image-upload"
                      className="btn btn-outline-primary btn-sm px-3 m-0"
                      style={{ cursor: "pointer" }}
                    >
                      Browse Files
                    </label>
                    <input
                      type="file"
                      id="image-upload"
                      accept="image/*"
                      className="d-none"
                      onChange={handleFileInput}
                    />
                  </div>
                )}
              </div>
            </div>
          </div>

        </div>
        <div className="d-flex justify-content-start mt-3">
          <div className="d-flex px-2">
            <Button
              color={"var(--green-primary)"}
              logo={add}
              hover={"green"}
              text={"Add Product"}
              type="submit"
            />
          </div>
        </div>
      </div>
    </form>
  );
}

export default AddProductForm;
