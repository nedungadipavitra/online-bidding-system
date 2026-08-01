import { Client } from "@stomp/stompjs";
import { getAccessToken, getRefreshToken, getValidAccessToken, getWebSocketUrl } from "../api/client";

export const createWebSocketClient = (onMessageReceived, auctionId) => {
  const auctionIds = (Array.isArray(auctionId) ? auctionId : [auctionId])
    .filter((id) => id !== undefined && id !== null)
    .map((id) => String(id));
  let stompClient = null;
  let cancelled = false;

  if (auctionIds.length === 0) {
    return () => {
      cancelled = true;
    };
  }

  if (!getAccessToken() && !getRefreshToken()) {
    return () => {
      cancelled = true;
    };
  }

  stompClient = new Client({
    brokerURL: getWebSocketUrl("/ws"),
    connectHeaders: {},
    beforeConnect: async () => {
      const token = await getValidAccessToken();
      if (cancelled || !token) {
        throw new Error("No valid access token available for live bid updates");
      }
      // Resolve a fresh token for every initial connection and reconnect.
      stompClient.connectHeaders = {
        Authorization: `Bearer ${token}`
      };
    },
    debug: function (str) {
      console.log("[STOMP Debug] ", str);
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000
  });

  stompClient.onConnect = () => {
    console.log("Connected to STOMP over WebSocket");

    auctionIds.forEach((id) => {
      stompClient.subscribe(`/topic/auction/${id}`, (message) => {
        if (message.body) {
          try {
            const bidUpdate = JSON.parse(message.body);
            onMessageReceived(bidUpdate);
          } catch (error) {
            console.error("Failed to parse live bid update message:", error);
          }
        }
      });
    });
  };

  stompClient.onStompError = (frame) => {
    console.error("STOMP protocol error encountered:", frame.headers["message"]);
    console.error("Error details:", frame.body);
  };

  stompClient.onWebSocketClose = () => {
    console.log("WebSocket connection closed");
  };

  stompClient.activate();

  // Return unsubscribe/deactivate clean cleanup function
  return () => {
    cancelled = true;
    if (stompClient?.active) {
      stompClient.deactivate();
      console.log("Deactivated STOMP connection client");
    }
  };
};
