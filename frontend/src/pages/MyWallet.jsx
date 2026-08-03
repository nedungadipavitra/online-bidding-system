import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import LoadingState from "../components/LoadingState";
import { apiFetch, getResponseMessage } from "../api/client";

function MyWallet({ role }) {
  const navigate = useNavigate();
  const [wallet, setWallet] = useState(null);
  const [amount, setAmount] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isAddingMoney, setIsAddingMoney] = useState(false);

  const loadWallet = useCallback(async () => {
    const userId = sessionStorage.getItem("loggedInUserId");
    const token = sessionStorage.getItem("token");
    if (!userId) {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    try {
      const walletRes = await apiFetch(`/wallets/user/${userId}`, {
        headers: {
          "Authorization": token ? `Bearer ${token}` : ""
        }
      });

      if (walletRes.ok) {
        const walletData = await walletRes.json();
        const txRes = await apiFetch(`/transactions/user/${userId}`, {
          headers: {
            "Authorization": token ? `Bearer ${token}` : ""
          }
        });
        const txData = txRes.ok ? await txRes.json() : [];

        const mappedTx = txData.map((tx) => ({
          type: tx.type === "DEPOSIT" ? "Deposit" : tx.type === "REFUND" ? "Refund" : "Withdrawal",
          amount: tx.type === "DEPOSIT" || tx.type === "REFUND" ? Number(tx.amount) : -Number(tx.amount),
          date: new Date(tx.timestamp).toLocaleDateString("en-IN", {
            day: "numeric",
            month: "short",
            year: "numeric"
          })
        }));

        setWallet({
          balance: walletData.balance,
          transactions: mappedTx
        });
      } else {
        setWallet({
          balance: 0,
          transactions: []
        });
      }
    } catch (error) {
      console.error("Error fetching wallet data:", error);
      toast.error("Unable to load wallet data.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // The callback performs external data loading and updates state after its async responses.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadWallet();
  }, [role, loadWallet]);

  const handleAddMoneySubmit = async (e) => {
    e.preventDefault();
    const numericAmount = parseFloat(amount);
    if (isNaN(numericAmount) || numericAmount <= 0) {
      toast.error("Please enter a valid amount.");
      return;
    }

    const userId = sessionStorage.getItem("loggedInUserId");
    const token = sessionStorage.getItem("token");
    if (!userId) return;

    setIsAddingMoney(true);
    try {
      const response = await apiFetch(`/wallets/${userId}/deposit`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": token ? `Bearer ${token}` : ""
        },
        body: JSON.stringify({
          amount: numericAmount
        })
      });

      if (response.ok) {
        setAmount("");
        await loadWallet();
        toast.success(`Successfully added Rs. ${numericAmount.toLocaleString("en-IN")} to your wallet.`);
      } else {
        toast.error(await getResponseMessage(response, "Failed to add money."));
      }
    } catch (error) {
      console.error("Deposit error:", error);
      toast.error("Error connecting to server.");
    } finally {
      setIsAddingMoney(false);
    }
  };

  if (isLoading && !wallet) {
    return (
      <div className="main-content container py-4 text-center">
        <LoadingState message="Loading wallet data..." />
      </div>
    );
  }

  return (
    <div className="main-content container py-4 text-start" style={{ maxWidth: "800px" }}>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="fw-bold mb-1" style={{ fontSize: "2rem" }}>My Wallet</h1>
          <p className="text-muted mb-0">Manage your balance and transactions as a <span className="fw-bold text-primary">{role}</span>.</p>
        </div>
        <button onClick={() => navigate(-1)} className="btn btn-outline-secondary">
          &larr; Back
        </button>
      </div>

      <div className="card text-white bg-dark mb-4 shadow-sm">
        <div className="card-body p-4 text-center">
          <span className="text-uppercase tracking-wider small text-white-50">Available Balance</span>
          <h2 className="display-5 fw-bold my-2">Rs. {wallet.balance.toLocaleString("en-IN")}</h2>
        </div>
      </div>

      {role === "buyer" && (
        <div className="card p-4 border shadow-sm mb-4">
          <h5 className="fw-bold mb-3">Add Money to Wallet</h5>
          <form onSubmit={handleAddMoneySubmit} className="row g-3 align-items-end">
            <div className="col-sm-9">
              <label className="form-label text-secondary small">Amount (INR)</label>
              <div className="input-group">
                <span className="input-group-text bg-white">Rs.</span>
                <input
                  type="number"
                  className="form-control"
                  placeholder="Enter amount to add"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  required
                />
              </div>
            </div>
            <div className="col-sm-3">
              <button type="submit" className="btn btn-primary w-100 py-2" disabled={isAddingMoney}>
                {isAddingMoney ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                    Adding...
                  </>
                ) : "Add Money"}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="card border shadow-sm">
        <div className="card-header bg-white fw-bold py-3">
          Recent Transactions
        </div>
        <div className="card-body p-0">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr className="small text-muted">
                  <th className="px-4 py-3">Type</th>
                  <th className="py-3">Date</th>
                  <th className="px-4 py-3 text-end">Amount</th>
                </tr>
              </thead>
              <tbody>
                {wallet.transactions.length === 0 ? (
                  <tr>
                    <td colSpan="3" className="text-center py-4 text-muted">No transactions found.</td>
                  </tr>
                ) : (
                  wallet.transactions.map((tx, idx) => (
                    <tr key={idx}>
                      <td className="px-4 py-3">{tx.type}</td>
                      <td className="py-3 text-muted">{tx.date}</td>
                      <td className={`px-4 py-3 text-end fw-bold ${tx.amount > 0 ? "text-success" : "text-danger"}`}>
                        {tx.amount > 0 ? "+" : ""}Rs. {tx.amount.toLocaleString("en-IN")}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

export default MyWallet;
