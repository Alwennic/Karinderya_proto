const request = require("supertest");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const { createApp } = require("../src/app");

const SECRET = "test-secret";

function fakeRepo() {
  const users = {
    admin: { username: "admin", password_hash: bcrypt.hashSync("adminpass1", 4), role: "admin" },
    cashier: { username: "cashier", password_hash: bcrypt.hashSync("cashierpass", 4), role: "staff" },
  };
  return {
    users,
    findByUsername: async (name) => users[name] || null,
    create: async (username, hash, role) => {
      users[username] = { username, password_hash: hash, role };
    },
  };
}

function tokenFor(role) {
  return jwt.sign({ sub: "someone", role }, SECRET);
}

let app;
beforeEach(() => {
  app = createApp(fakeRepo(), SECRET);
});

test("GET /health returns ok", async () => {
  const res = await request(app).get("/health");
  expect(res.status).toBe(200);
  expect(res.body.status).toBe("ok");
});

test("login with correct password returns a token", async () => {
  const res = await request(app).post("/login").send({ username: "admin", password: "adminpass1" });
  expect(res.status).toBe(200);
  const claims = jwt.verify(res.body.token, SECRET);
  expect(claims.role).toBe("admin");
});

test("login with wrong password is rejected", async () => {
  const res = await request(app).post("/login").send({ username: "admin", password: "nope" });
  expect(res.status).toBe(401);
});

test("hash and verify: stored password is not plain text", () => {
  const hash = bcrypt.hashSync("secret123", 4);
  expect(hash).not.toBe("secret123");
  expect(bcrypt.compareSync("secret123", hash)).toBe(true);
});

test("GET /me needs a token", async () => {
  expect((await request(app).get("/me")).status).toBe(401);
  const res = await request(app).get("/me").set("Authorization", "Bearer " + tokenFor("staff"));
  expect(res.status).toBe(200);
  expect(res.body.role).toBe("staff");
});

test("register requires an admin token", async () => {
  const body = { username: "newstaff", password: "longenough1" };
  expect((await request(app).post("/register").send(body)).status).toBe(401);
  const res = await request(app).post("/register").set("Authorization", "Bearer " + tokenFor("staff")).send(body);
  expect(res.status).toBe(403);
});

test("admin can register a staff account", async () => {
  const res = await request(app)
    .post("/register")
    .set("Authorization", "Bearer " + tokenFor("admin"))
    .send({ username: "newstaff", password: "longenough1" });
  expect(res.status).toBe(201);
  expect(res.body.role).toBe("staff");
});

test("duplicate username is rejected", async () => {
  const res = await request(app)
    .post("/register")
    .set("Authorization", "Bearer " + tokenFor("admin"))
    .send({ username: "cashier", password: "longenough1" });
  expect(res.status).toBe(409);
});
