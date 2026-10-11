// Starts auth-api: connects to MySQL, creates the first admin, listens on 3000.
const bcrypt = require("bcryptjs");
const mysql = require("mysql2/promise");
const { createApp } = require("./app");

const pool = mysql.createPool({
  host: process.env.DB_HOST || "db",
  user: process.env.DB_USER || "canteen",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "karinderya_proto",
  connectionLimit: 5,
});

const repo = {
  async findByUsername(username) {
    const [rows] = await pool.query(
      "SELECT id, username, password_hash, role FROM users WHERE username = ?",
      [username]
    );
    return rows[0] || null;
  },
  async create(username, hash, role) {
    await pool.query(
      "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
      [username, hash, role]
    );
  },
};

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

// Create the admin from ADMIN_USER / ADMIN_PASSWORD if it does not exist yet.
async function seedAdmin() {
  const { ADMIN_USER, ADMIN_PASSWORD } = process.env;
  if (!ADMIN_USER || !ADMIN_PASSWORD) {
    console.log("ADMIN_USER / ADMIN_PASSWORD not set: skipping admin seed");
    return;
  }
  for (let attempt = 1; attempt <= 30; attempt++) {
    try {
      if (!(await repo.findByUsername(ADMIN_USER))) {
        await repo.create(ADMIN_USER, await bcrypt.hash(ADMIN_PASSWORD, 10), "admin");
        console.log("Admin account created:", ADMIN_USER);
      }
      return;
    } catch (err) {
      console.log(`Database not ready (attempt ${attempt}/30): ${err.code || err.message}`);
      await sleep(2000);
    }
  }
  console.error("Could not seed admin account");
  process.exit(1);
}

seedAdmin().then(() => {
  const app = createApp(repo, process.env.JWT_SECRET || "dev-secret");
  app.listen(3000, () => console.log("auth-api listening on 3000"));
});
