import { apiJson } from "./client";

// A cache is scoped to one screen load. It deduplicates requests for repeated
// IDs without keeping stale data between refreshes.
export function createRequestCache() {
  const requests = new Map();

  const get = (key, path) => {
    if (!requests.has(key)) {
      const request = apiJson(path).catch((error) => {
        requests.delete(key);
        throw error;
      });
      requests.set(key, request);
    }
    return requests.get(key);
  };

  return {
    getProduct: (productId) => get(`product:${productId}`, `/products/${productId}`),
    getUser: (userId) => get(`user:${userId}`, `/users/${userId}`),
    getHighestBids: (auctionIds) => {
      const ids = [...new Set(auctionIds)].filter(Boolean).sort((a, b) => a - b);
      if (ids.length === 0) {
        return Promise.resolve({});
      }
      return get(`highest:${ids.join(",")}`, `/bids/highest?auctionIds=${ids.join(",")}`);
    },
  };
}
