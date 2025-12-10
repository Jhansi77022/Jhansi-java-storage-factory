package com.example.storage.postgres;

import com.example.model.Todo;
import com.example.storage.StorageException;
import com.example.storage.TodoStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PostgresTodoStorage implements TodoStorage {

    private static final Logger logger = LoggerFactory.getLogger(PostgresTodoStorage.class);
    private final String jdbcUrl;
    private final String user;
    private final String password;

    public PostgresTodoStorage(String jdbcUrl, String user, String password) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        initialize();
    }

    private void initialize() {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS todos (
                    id SERIAL PRIMARY KEY,
                    title VARCHAR(255),
                    description TEXT,
                    completed BOOLEAN
                );
            """);

            logger.info("PostgreSQL todos table verified/created.");
        } catch (Exception e) {
            logger.error("Failed to initialize PostgreSQL DB", e);
            throw new RuntimeException(e);
        }
    }

    private Connection getConn() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    @Override
    public void save(Todo todo) throws StorageException {
        String sql = "INSERT INTO todos(title,description,completed) VALUES(?,?,?) RETURNING id";

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, todo.getTitle());
            ps.setString(2, todo.getDescription());
            ps.setBoolean(3, todo.isCompleted());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    todo.setId(String.valueOf(rs.getInt(1)));
                }
            }

        } catch (Exception e) {
            throw new StorageException("Failed to save todo", e);
        }
    }

    @Override
    public Optional<Todo> retrieve(String id) throws StorageException {
        String sql = "SELECT * FROM todos WHERE id=?";

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, Integer.parseInt(id));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return Optional.of(map(rs));
            return Optional.empty();

        } catch (Exception e) {
            throw new StorageException("Failed to retrieve todo", e);
        }
    }

    @Override
    public List<Todo> retrieveAll() throws StorageException {
        String sql = "SELECT * FROM todos";

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Todo> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;

        } catch (Exception e) {
            throw new StorageException("Failed to retrieve all", e);
        }
    }

    @Override
    public void update(Todo todo) throws StorageException {
        String sql = "UPDATE todos SET title=?,description=?,completed=? WHERE id=?";

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, todo.getTitle());
            ps.setString(2, todo.getDescription());
            ps.setBoolean(3, todo.isCompleted());
            ps.setInt(4, Integer.parseInt(todo.getId()));

            if (ps.executeUpdate() == 0)
                throw new StorageException("ID not found");

        } catch (Exception e) {
            throw new StorageException("Failed to update", e);
        }
    }

    @Override
    public void delete(String id) throws StorageException {
        String sql = "DELETE FROM todos WHERE id=?";

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, Integer.parseInt(id));
            ps.executeUpdate();

        } catch (Exception e) {
            throw new StorageException("Failed to delete", e);
        }
    }

    private Todo map(ResultSet rs) throws SQLException {
        return new Todo(
                String.valueOf(rs.getInt("id")),
                rs.getString("title"),
                rs.getString("description"),
                rs.getBoolean("completed")
        );
    }
}
