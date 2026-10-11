USE karinderya_proto;

CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(10) NOT NULL DEFAULT 'staff',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS menu_items (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  category VARCHAR(50) NOT NULL,
  price DECIMAL(8,2) NOT NULL,
  available TINYINT(1) NOT NULL DEFAULT 1
);

INSERT INTO menu_items (name, category, price, available) VALUES
  ('Chicken Adobo with Rice', 'Rice Meals', 75.00, 1),
  ('Pork Sinigang with Rice', 'Rice Meals', 80.00, 1),
  ('Fried Chicken with Rice', 'Rice Meals', 70.00, 0),
  ('Pancit Canton', 'Noodles', 45.00, 1),
  ('Beef Mami', 'Noodles', 60.00, 1),
  ('Iced Tea', 'Drinks', 25.00, 1),
  ('Bottled Water', 'Drinks', 20.00, 1),
  ('Banana Cue', 'Snacks', 15.00, 1);
