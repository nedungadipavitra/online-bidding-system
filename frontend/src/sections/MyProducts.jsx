import { useState, useEffect, useCallback } from "react";
import { toast } from "react-toastify";
import LoadingState from "../components/LoadingState";
import ProductRow from "../components/ProductRow";
import { apiJson } from "../api/client";
import { createRequestCache } from "../api/resources";
import { getAuctionPhase } from "../utils/auctionStatus";

function MyProducts({ refreshTrigger, onLoadingChange }) {
  const [products, setProducts] = useState([]);
  const [deliveryPartners, setDeliveryPartners] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const loggedInUserId = Number(sessionStorage.getItem("loggedInUserId"));

  const fetchSellerProducts = useCallback(async () => {
    setIsLoading(true);
    onLoadingChange?.(true);
    try {
      const requestCache = createRequestCache();
      const [data, ordersData] = await Promise.all([
        apiJson("/products").catch(() => []),
        apiJson(`/orders/seller/${loggedInUserId}`).catch(() => [])
      ]);
      const highestBids = await requestCache
        .getHighestBids(data.map((product) => product.productId))
        .catch(() => ({}));

      const mapped = data
        .filter((product) => Number(product.sellerId) === loggedInUserId)
        .map((product) => {
          const highestBid = highestBids[product.productId];
          const matchedOrder = ordersData.find(
            (order) => Number(order.productId) === Number(product.productId)
          );

          return {
            id: product.productId,
            name: product.name,
            description: product.description,
            image: product.imageUrl,
            basePrice: product.basePrice,
            currentBid: highestBid?.amount || product.currentHighestBid || product.basePrice,
            startTime: product.auctionStartTime,
            endTime: product.auctionEndTime,
            status: getAuctionPhase(product),
            sellerId: product.sellerId,
            winnerName: highestBid?.bidderName || null,
            winnerId: highestBid?.bidderId || null,
            deliveryPersonName: matchedOrder?.deliveryPersonName || null,
            deliveryStatus: matchedOrder?.status || matchedOrder?.deliveryStatus || null,
            orderId: matchedOrder?.id || null
          };
        });

      setProducts(mapped);
    } catch (error) {
      console.error("Error fetching seller products:", error);
      toast.error("Unable to load your products.");
    } finally {
      setIsLoading(false);
      onLoadingChange?.(false);
    }
  }, [loggedInUserId, onLoadingChange]);

  useEffect(() => {
    // The callback performs external data loading and updates state after its async responses.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchSellerProducts();
  }, [refreshTrigger, fetchSellerProducts]);

  useEffect(() => {
    const fetchDeliveryPartners = async () => {
      try {
        const data = await apiJson("/users/delivery");
        setDeliveryPartners(data);
      } catch (error) {
        console.error("Error fetching delivery partners:", error);
        toast.error("Unable to load delivery partners.");
      }
    };
    fetchDeliveryPartners();
  }, []);

  return (
    <div className="card p-4">
      <h4 className="px-2">My Products</h4>
      <div className="container mt-1">
        {isLoading && products.length === 0 ? (
          <LoadingState message="Loading your products..." compact />
        ) : (
        <div className="table-responsive mt-3">
          <table className="table align-middle">
            <thead>
              <tr>
                {[
                  "Name",
                  "Base Price",
                  "Current Bid",
                  "Status",
                  "Auction End",
                  "Delivery Partner"
                ].map((e, index) => (
                  <th key={index}>{e}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <ProductRow 
                  key={product.id} 
                  product={product} 
                  deliveryPartners={deliveryPartners}
                  onAssignSuccess={fetchSellerProducts}
                />
              ))}
              {products.length === 0 && (
                <tr>
                  <td colSpan="6" className="text-center text-muted py-4">
                    No products added for auction yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        )}
      </div>
    </div>
  );
}

export default MyProducts;
