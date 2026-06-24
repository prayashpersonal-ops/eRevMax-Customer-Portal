const SELLABLE_STORAGE_KEY = "hotelSellableState";

function readStore() {
  try {
    return JSON.parse(localStorage.getItem(SELLABLE_STORAGE_KEY) || "{}");
  } catch {
    return {};
  }
}

function writeStore(store) {
  localStorage.setItem(SELLABLE_STORAGE_KEY, JSON.stringify(store));
}

export function sellableKey(userEmail, hotelName) {
  return `${String(userEmail || "unknown").toLowerCase()}::${String(hotelName || "unknown").toLowerCase()}`;
}

export function getSellableState(userEmail, hotelName, fallback = false) {
  const store = readStore();
  const key = sellableKey(userEmail, hotelName);
  return Object.prototype.hasOwnProperty.call(store, key) ? Boolean(store[key]) : Boolean(fallback);
}

export function setSellableState(userEmail, hotelName, value) {
  const store = readStore();
  store[sellableKey(userEmail, hotelName)] = Boolean(value);
  writeStore(store);
  window.dispatchEvent(new CustomEvent("hotel-sellable-updated"));
}

export function getSellableSnapshot() {
  return readStore();
}
