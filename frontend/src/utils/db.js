import iphoneImg from "../assets/iphone.jpeg";
import rolexImg from "../assets/rolex.png";
import ps6Img from "../assets/ps6.png";
import teslaImg from "../assets/tesla.png";
import macbookImg from "../assets/macbook.png";

const imageMap = {
  "iphone.jpeg": iphoneImg,
  "rolex.png": rolexImg,
  "ps6.png": ps6Img,
  "tesla.png": teslaImg,
  "macbook.png": macbookImg
};

const defaultAuctions = [];
const defaultDeliveries = [
  {
    id: "DLV-9018",
    productName: "iPhone 16 Pro",
    winner: "Amit K.",
    price: 82000,
    status: "Assigned",
    estimatedDelivery: "June 05, 2026"
  },
  {
    id: "DLV-3482",
    productName: "Sony WH-1000XM5 Headphones",
    winner: "Priya M.",
    price: 24500,
    status: "Out for Delivery",
    estimatedDelivery: "June 12, 2026"
  },
  {
    id: "DLV-7712",
    productName: "Keychron Q1 Mechanical Keyboard",
    winner: "Siddharth R.",
    price: 14000,
    status: "Assigned",
    estimatedDelivery: "June 14, 2026"
  },
  {
    id: "DLV-1029",
    productName: "iPad Air M2",
    winner: "Ayush S.",
    price: 54000,
    status: "Delivered",
    estimatedDelivery: "June 16, 2026"
  },
  {
    id: "DLV-5520",
    productName: "Razer BlackWidow Keyboard",
    winner: "Devansh S.",
    price: 12500,
    status: "Assigned",
    estimatedDelivery: "June 18, 2026"
  },
  {
    id: "DLV-4019",
    productName: "Logitech G Pro X Superlight",
    winner: "Pavitra N.",
    price: 9500,
    status: "Out for Delivery",
    estimatedDelivery: "June 15, 2026"
  },
  {
    id: "DLV-8812",
    productName: "Apple Watch Ultra 2",
    winner: "Tejas B.",
    price: 68000,
    status: "Assigned",
    estimatedDelivery: "June 20, 2026"
  },
  {
    id: "DLV-3029",
    productName: "Bellroy Leather Wallet",
    winner: "Amit K.",
    price: 6200,
    status: "Delivered",
    estimatedDelivery: "June 13, 2026"
  }
];

const defaultWallets = {
  buyer: {
    balance: 0,
    transactions: [
      { type: "Deposit", amount: 50000, date: "June 08, 2026" },
      { type: "Auction Bid Hold", amount: -85250, date: "June 07, 2026" },
      { type: "Refund", amount: 45000, date: "June 05, 2026" }
    ]
  },
  seller: {
    balance: 0,
    transactions: [
      { type: "Auction Payout", amount: 82000, date: "June 06, 2026" },
      { type: "Auction Payout", amount: 24500, date: "June 05, 2026" }
    ]
  }
};

export function initDb() {
  if (!localStorage.getItem("auctions")) {
    localStorage.setItem("auctions", JSON.stringify(defaultAuctions));
  }
  if (!localStorage.getItem("deliveries")) {
    localStorage.setItem("deliveries", JSON.stringify(defaultDeliveries));
  }
  if (!localStorage.getItem("wallets")) {
    localStorage.setItem("wallets", JSON.stringify(defaultWallets));
  }
  if (!localStorage.getItem("categories")) {
    localStorage.setItem("categories", JSON.stringify(defaultCategories));
  }
  if (!localStorage.getItem("orders")) {
    localStorage.setItem("orders", JSON.stringify(defaultOrders));
  }
  if (!localStorage.getItem("users")) {
    localStorage.setItem("users", JSON.stringify(defaultUsers));
  }
  if (!localStorage.getItem("bids")) {
    localStorage.setItem("bids", JSON.stringify(defaultBids));
  }
}

export function getAuctions() {
  initDb();
  const list = JSON.parse(localStorage.getItem("auctions")) || [];
  return list.map(item => ({
    ...item,
    imageSrc: item.image && String(item.image).startsWith("data:") ? item.image : (imageMap[item.image] || item.imageSrc || "")
  }));
}

export function addAuction(product) {
  initDb();
  const auctions = JSON.parse(localStorage.getItem("auctions")) || [];
  const newProduct = {
    id: auctions.length > 0 ? "prod_" + (Math.max(...auctions.map(a => {
      const match = String(a.id).match(/\d+/);
      return match ? parseInt(match[0]) : 0;
    })) + 1) : "prod_1",
    name: product.name,
    category: product.category,
    basePrice: Number(product.basePrice),
    currentBid: Number(product.basePrice),
    description: product.description,
    image: product.image || "",
    endTime: product.endTime,
    startTime: product.startTime || new Date().toISOString(),
    status: "ACTIVE",
    seller: sessionStorage.getItem("loggedInUserName") || "Seller"
  };
  auctions.push(newProduct);
  localStorage.setItem("auctions", JSON.stringify(auctions));
  return newProduct;
}

export function placeBid(productId, amount, buyerName) {
  initDb();
  const auctions = JSON.parse(localStorage.getItem("auctions")) || [];
  const index = auctions.findIndex(a => String(a.id) === String(productId));
  if (index === -1) {
    return { success: false, message: "Product not found" };
  }

  const auction = auctions[index];
  if (auction.status !== "ACTIVE") {
    return { success: false, message: "This auction is no longer active." };
  }

  const bidAmount = Number(amount);
  if (isNaN(bidAmount) || bidAmount <= auction.currentBid) {
    return { success: false, message: `Bid must be greater than current bid (₹${auction.currentBid}).` };
  }

  if (bidAmount < auction.basePrice) {
    return { success: false, message: `Bid must be at least the base price (₹${auction.basePrice}).` };
  }

  auctions[index] = {
    ...auction,
    currentBid: bidAmount,
    lastBidder: buyerName
  };

  localStorage.setItem("auctions", JSON.stringify(auctions));

  const bids = JSON.parse(localStorage.getItem("bids")) || [];
  const newBidId = "BID-" + String(bids.length + 1).padStart(3, '0');
  bids.push({
    id: newBidId,
    productId: auction.id,
    productName: auction.name,
    bidderName: buyerName,
    bidAmount: bidAmount,
    bidTime: new Date().toISOString()
  });
  localStorage.setItem("bids", JSON.stringify(bids));

  // Deduct/hold money from buyer's wallet (optional, but very neat!)
  const wallets = JSON.parse(localStorage.getItem("wallets")) || defaultWallets;
  if (wallets.buyer && wallets.buyer.balance >= bidAmount) {
    wallets.buyer.balance -= bidAmount;
    wallets.buyer.transactions.unshift({
      type: `Hold for ${auction.name || auction.title}`,
      amount: -bidAmount,
      date: new Date().toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })
    });
    localStorage.setItem("wallets", JSON.stringify(wallets));
  }

  return { success: true, auction: auctions[index] };
}

export function getDeliveries() {
  initDb();
  return JSON.parse(localStorage.getItem("deliveries")) || [];
}

export function getWallet(role) {
  initDb();
  const wallets = JSON.parse(localStorage.getItem("wallets")) || defaultWallets;
  return wallets[role];
}

export function addMoney(role, amount) {
  initDb();
  const wallets = JSON.parse(localStorage.getItem("wallets")) || defaultWallets;
  if (!wallets[role]) {
    wallets[role] = { balance: 0, transactions: [] };
  }
  wallets[role].balance += amount;
  wallets[role].transactions.unshift({
    type: "Deposit",
    amount,
    date: new Date().toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })
  });
  localStorage.setItem("wallets", JSON.stringify(wallets));
  return wallets[role];
}

const defaultCategories = [
  { id: 1, name: "Electronics", description: "Devices and gadgets including phones, laptops, cameras and more." },
  { id: 2, name: "Furniture", description: "Home and office furniture including chairs, tables, beds and more." },
  { id: 3, name: "Fashion", description: "Clothing, shoes, watches, and accessories for men and women." },
  { id: 4, name: "Books", description: "Fiction, non-fiction, academic and children's books." }
];

const defaultOrders = [
  {
    id: "ORD-1001",
    productName: "iPhone 14 Pro Max",
    specifications: "256GB, Space Black",
    price: 85250,
    status: "ASSIGNED",
    deliveryPerson: "Ramesh Kumar",
    image: "iphone.jpeg"
  },
  {
    id: "ORD-1002",
    productName: "MacBook Air M2",
    specifications: "8GB RAM, 256GB SSD",
    price: 92500,
    status: "OUT_FOR_DELIVERY",
    deliveryPerson: "Suresh Yadav",
    image: "macbook.png"
  },
  {
    id: "ORD-1003",
    productName: "Canon EOS 90D",
    specifications: "18-135mm Lens, DSLR Camera",
    price: 52750,
    status: "DELIVERED",
    deliveryPerson: "Ramesh Kumar",
    image: ""
  },
  {
    id: "ORD-1004",
    productName: "PlayStation 5",
    specifications: "Digital Edition, 8K Gaming Console",
    price: 47000,
    status: "ASSIGNED",
    deliveryPerson: "Not Assigned",
    image: "ps6.png"
  },
  {
    id: "ORD-1005",
    productName: "iPad Air (5th Gen)",
    specifications: "64GB, Wi-Fi",
    price: 43000,
    status: "DELIVERED",
    deliveryPerson: "Anil Verma",
    image: ""
  }
];

export function getCategories() {
  initDb();
  return JSON.parse(localStorage.getItem("categories")) || [];
}

export function addCategory(category) {
  initDb();
  const categories = getCategories();
  const newCat = {
    id: categories.length > 0 ? Math.max(...categories.map(c => c.id)) + 1 : 1,
    name: category.name,
    description: category.description
  };
  categories.push(newCat);
  localStorage.setItem("categories", JSON.stringify(categories));
  return newCat;
}

export function deleteCategory(id) {
  initDb();
  let categories = getCategories();
  categories = categories.filter(c => c.id !== Number(id));
  localStorage.setItem("categories", JSON.stringify(categories));
  return categories;
}

export function updateCategory(id, updatedCat) {
  initDb();
  const categories = getCategories();
  const idx = categories.findIndex(c => c.id === Number(id));
  if (idx !== -1) {
    categories[idx] = { ...categories[idx], ...updatedCat };
    localStorage.setItem("categories", JSON.stringify(categories));
  }
}

export function getOrders() {
  initDb();
  return JSON.parse(localStorage.getItem("orders")) || [];
}

const defaultUsers = [];
const defaultBids = [
  { id: "BID-001", productId: "iphone_17", productName: "iPhone 17 Pro", bidderName: "Amit Kumar", bidAmount: 85250, bidTime: "2026-07-28T14:30" },
  { id: "BID-002", productId: "rolex_watch", productName: "Vintage Rolex Submariner", bidderName: "Priya Sharma", bidAmount: 510000, bidTime: "2026-07-28T15:00" },
  { id: "BID-003", productId: "ps6_console", productName: "PlayStation 6 Console", bidderName: "Siddharth R.", bidAmount: 58000, bidTime: "2026-07-28T16:15" },
  { id: "BID-004", productId: "tesla_model_s", productName: "Tesla Model S Toy Edition", bidderName: "Amit Kumar", bidAmount: 6200, bidTime: "2026-07-28T16:45" },
  { id: "BID-005", productId: "macbook_pro", productName: "MacBook Pro M5 Max", bidderName: "Priya Sharma", bidAmount: 215000, bidTime: "2026-07-28T17:00" }
];

export function getUsers() {
  initDb();
  return JSON.parse(localStorage.getItem("users")) || [];
}

export function deleteUser(id) {
  initDb();
  let users = getUsers();
  users = users.filter(u => String(u.id) !== String(id));
  localStorage.setItem("users", JSON.stringify(users));
  return users;
}

export function updateUser(id, updatedFields) {
  initDb();
  const users = getUsers();
  const idx = users.findIndex(u => String(u.id) === String(id));
  if (idx !== -1) {
    users[idx] = { ...users[idx], ...updatedFields };
    localStorage.setItem("users", JSON.stringify(users));
  }
}

export function getBids() {
  initDb();
  return JSON.parse(localStorage.getItem("bids")) || [];
}

export function deleteBid(id) {
  initDb();
  let bids = getBids();
  bids = bids.filter(b => String(b.id) !== String(id));
  localStorage.setItem("bids", JSON.stringify(bids));
  return bids;
}

export function updateBid(id, updatedFields) {
  initDb();
  const bids = getBids();
  const idx = bids.findIndex(b => String(b.id) === String(id));
  if (idx !== -1) {
    bids[idx] = { ...bids[idx], ...updatedFields };
    localStorage.setItem("bids", JSON.stringify(bids));
  }
}

export function deleteAuction(id) {
  initDb();
  let auctions = JSON.parse(localStorage.getItem("auctions")) || [];
  auctions = auctions.filter(a => String(a.id) !== String(id));
  localStorage.setItem("auctions", JSON.stringify(auctions));
  return auctions;
}

export function updateAuction(id, updatedFields) {
  initDb();
  const auctions = JSON.parse(localStorage.getItem("auctions")) || [];
  const idx = auctions.findIndex(a => String(a.id) === String(id));
  if (idx !== -1) {
    auctions[idx] = { ...auctions[idx], ...updatedFields };
    localStorage.setItem("auctions", JSON.stringify(auctions));
  }
}
