// Karinderya_proto auth-api: staff login and staff accounts.
const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");

function createApp(repo, secret) {
  const app = express();
  app.use(express.json());

  function requireToken(req, res, next) {
    const [type, token] = (req.headers.authorization || "").split(" ");
    if (type !== "Bearer" || !token) {
      return res.status(401).json({ error: "Missing token" });
    }
    try {
      req.user = jwt.verify(token, secret);
      next();
    } catch (err) {
      return res.status(401).json({ error: "Invalid or expired token" });
    }
  }

  function requireAdmin(req, res, next) {
    if (req.user.role !== "admin") {
      return res.status(403).json({ error: "Admin access only" });
    }
    next();
  }

  app.get("/health", (req, res) => res.json({ status: "ok" }));

  // POST /login {username, password} -> {token}
  app.post("/login", async (req, res) => {
    const { username, password } = req.body || {};
    if (!username || !password) {
      return res.status(400).json({ error: "Username and password are required" });
    }
    const user = await repo.findByUsername(username);
    const ok = user && (await bcrypt.compare(password, user.password_hash));
    if (!ok) {
      return res.status(401).json({ error: "Wrong username or password" });
    }
    const token = jwt.sign({ sub: user.username, role: user.role }, secret, { expiresIn: "8h" });
    res.json({ token });
  });

  // GET /me -> who is logged in
  app.get("/me", requireToken, (req, res) => {
    res.json({ username: req.user.sub, role: req.user.role });
  });

  // POST /register {username, password, role?} -> creates a staff account (admin only)
  app.post("/register", requireToken, requireAdmin, async (req, res) => {
    const { username, password, role } = req.body || {};
    if (!username || username.length < 3) {
      return res.status(400).json({ error: "Username must be at least 3 characters" });
    }
    if (!password || password.length < 8) {
      return res.status(400).json({ error: "Password must be at least 8 characters" });
    }
    const newRole = role === "admin" ? "admin" : "staff";
    if (await repo.findByUsername(username)) {
      return res.status(409).json({ error: "Username already exists" });
    }
    const hash = await bcrypt.hash(password, 10);
    await repo.create(username, hash, newRole);
    res.status(201).json({ username, role: newRole });
  });

  return app;
}

module.exports = { createApp };
