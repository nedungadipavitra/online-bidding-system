const TERMINAL_STATUSES = new Set(["SOLD", "UNSOLD", "NOT_SOLD", "CANCELLED"]);

function getDateValue(product, primaryKey, fallbackKey) {
  return product?.[primaryKey] || product?.[fallbackKey];
}

export function getAuctionPhase(product, now = new Date()) {
  const status = String(product?.status || "ACTIVE").toUpperCase();
  if (TERMINAL_STATUSES.has(status)) {
    return status;
  }

  const startValue = getDateValue(product, "auctionStartTime", "startTime");
  const endValue = getDateValue(product, "auctionEndTime", "endTime");
  const startTime = startValue ? new Date(startValue) : null;
  const endTime = endValue ? new Date(endValue) : null;

  if (startTime && !Number.isNaN(startTime.getTime()) && startTime > now) {
    return "UPCOMING";
  }

  if (endTime && !Number.isNaN(endTime.getTime()) && endTime <= now) {
    return "ENDED";
  }

  return "ACTIVE";
}

export function isAuctionOpen(product, now = new Date()) {
  return getAuctionPhase(product, now) === "ACTIVE";
}
