// ---------------------------------------------------------------
// Karinderya_proto shared script
// USE_MOCK = true  -> works on its own (no backend), data kept in the browser
// USE_MOCK = false -> talks to the real APIs through the Nginx proxy
// Change it to false once catalog-api and auth-api are running.
// ---------------------------------------------------------------
const USE_MOCK = false;

const API = { menu: "/api/menu", auth: "/api/auth" };

const SEED_ITEMS = [
  { id: 1, name: "Chicken Adobo with Rice", category: "Rice Meals", price: 75, available: true },
  { id: 2, name: "Pork Sinigang with Rice", category: "Rice Meals", price: 80, available: true },
  { id: 3, name: "Fried Chicken with Rice", category: "Rice Meals", price: 70, available: false },
  { id: 4, name: "Pancit Canton", category: "Noodles", price: 45, available: true },
  { id: 5, name: "Beef Mami", category: "Noodles", price: 60, available: true },
  { id: 6, name: "Iced Tea", category: "Drinks", price: 25, available: true },
  { id: 7, name: "Bottled Water", category: "Drinks", price: 20, available: true },
  { id: 8, name: "Banana Cue", category: "Snacks", price: 15, available: true }
];

// ---------- helpers ----------
function formatPrice(value) {
  return "\u20B1" + Number(value).toFixed(2);
}

function getToken() {
  return sessionStorage.getItem("cg_token");
}
function setToken(token) {
  sessionStorage.setItem("cg_token", token);
}
function clearToken() {
  sessionStorage.removeItem("cg_token");
}

function mockLoad() {
  const raw = localStorage.getItem("cg_items");
  if (!raw) {
    localStorage.setItem("cg_items", JSON.stringify(SEED_ITEMS));
    return SEED_ITEMS.slice();
  }
  return JSON.parse(raw);
}
function mockSave(items) {
  localStorage.setItem("cg_items", JSON.stringify(items));
}

// ---------- catalog-api ----------
// Expected: GET /items -> [{id, name, category, price, available}]
async function getItems() {
  if (USE_MOCK) return mockLoad();
  const res = await fetch(API.menu + "/items");
  if (!res.ok) throw new Error("Could not load the menu. Try again in a moment.");
  return res.json();
}

// Expected: POST /items (new) or PUT /items/<id> (edit), with a Bearer token
async function saveItem(item) {
  if (USE_MOCK) {
    const items = mockLoad();
    if (item.id) {
      const i = items.findIndex((x) => x.id === item.id);
      items[i] = item;
    } else {
      item.id = items.reduce((max, x) => Math.max(max, x.id), 0) + 1;
      items.push(item);
    }
    mockSave(items);
    return item;
  }
  const isEdit = Boolean(item.id);
  const res = await fetch(API.menu + "/items" + (isEdit ? "/" + item.id : ""), {
    method: isEdit ? "PUT" : "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: "Bearer " + getToken()
    },
    body: JSON.stringify(item)
  });
  if (res.status === 401 || res.status === 403) {
    clearToken();
    throw new Error("Your session expired. Log in again.");
  }
  if (!res.ok) throw new Error("Could not save the item. Check the fields and try again.");
  return res.json();
}

// ---------- auth-api ----------
// Expected: POST /login {username, password} -> {token}
async function login(username, password) {
  if (USE_MOCK) {
    if (username === "admin" && password === "admin123") return "mock-token";
    throw new Error("Wrong username or password.");
  }
  const res = await fetch(API.auth + "/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password })
  });
  if (!res.ok) throw new Error("Wrong username or password.");
  const data = await res.json();
  return data.token;
}
