package com.karinderya_proto.catalog;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcItemRepository implements ItemRepository {

    private static final String COLUMNS = "id, name, category, price, available";

    private static final RowMapper<Item> MAPPER = (rs, rowNum) -> new Item(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getBigDecimal("price").doubleValue(),
            rs.getBoolean("available"));

    private final JdbcTemplate jdbc;

    public JdbcItemRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Item> list() {
        return jdbc.query("SELECT " + COLUMNS + " FROM menu_items ORDER BY category, name", MAPPER);
    }

    @Override
    public Optional<Item> find(long id) {
        List<Item> rows = jdbc.query("SELECT " + COLUMNS + " FROM menu_items WHERE id = ?", MAPPER, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public long create(String name, String category, double price, boolean available) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO menu_items (name, category, price, available) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setDouble(3, price);
            ps.setBoolean(4, available);
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    @Override
    public boolean update(long id, String name, String category, double price, boolean available) {
        if (find(id).isEmpty()) {
            return false;
        }
        jdbc.update("UPDATE menu_items SET name = ?, category = ?, price = ?, available = ? WHERE id = ?",
                name, category, price, available, id);
        return true;
    }
}
