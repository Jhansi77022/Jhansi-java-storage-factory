package com.example.storage.mysql;

import com.example.model.Todo;
import com.example.storage.StorageException;
import com.example.storage.TodoStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MySqlTodoStorage implements TodoStorage {
    private static final Logger logger = LoggerFactory.getLogger(MySqlTodoStorage.class);
    private final String jdbcUrl;
    private final String user;
    private final String password;

    public MySqlTodoStorage(String jdbcUrl, String user, String password) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;

        initializeDatabase();
    }

    private void initializeDatabase() {
    try {
        if (jdbcUrl.startsWith("jdbc:h2:")) {
            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
                 Statement stmt = conn.createStatement()) {

                stmt.execute("""
            CREATE TABLE IF NOT EXISTS todos (
                id INT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(255),
                description TEXT,
                completed BOOLEAN
            );
        """);
            }
            return;
        }

        // STEP 1: Connect without a database
        String rootUrl = jdbcUrl.substring(0, jdbcUrl.lastIndexOf("/"));
        try (Connection conn = DriverManager.getConnection(rootUrl, user, password);
             Statement stmt = conn.createStatement()) {
            //Helps create connection with the databse.
        }

        // STEP 2: Connect to the database and create table if missing
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS todos (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(255),
                    description TEXT,
                    completed BOOLEAN
                );
                    """);

            logger.info("Table 'todos' verified/created.");
        }

    } catch (Exception e) {
        logger.error("Failed to initialize database", e);
    }
}

    private Connection getConn() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

@Override
public void save(Todo todo) throws StorageException {
    final String sql = "INSERT INTO todos (title, description, completed) VALUES (?, ?, ?)";

    try (Connection conn = getConn();
         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

        ps.setString(1, todo.getTitle());
        ps.setString(2, todo.getDescription());
        ps.setBoolean(3, todo.isCompleted());
        ps.executeUpdate();

        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                int generatedId = rs.getInt(1);
                todo.setId(generatedId + "");
                logger.debug("Saved todo {}", generatedId);
            }
        }

    } catch (SQLException e) {
        throw new StorageException("Failed to save todo", e);
    }
}


    @Override
    public Optional<Todo> retrieve(String id) throws StorageException {
        final String sql = "SELECT id, title, description, completed FROM todos WHERE id = ?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Todo t = mapRow(rs);
                    return Optional.of(t);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to retrieve todo", e);
        }
    }

    @Override
    public List<Todo> retrieveAll() throws StorageException {
        final String sql = "SELECT id, title, description, completed FROM todos";
        List<Todo> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new StorageException("Failed to retrieve all todos", e);
        }
    }

    @Override
    public void update(Todo todo) throws StorageException {
        final String sql = "UPDATE todos SET title = ?, description = ?, completed = ? WHERE id = ?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, todo.getTitle());
            ps.setString(2, todo.getDescription());
            ps.setBoolean(3, todo.isCompleted());
            ps.setString(4, todo.getId());
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new StorageException("No todo updated, id not found: " + todo.getId());
            }
            logger.debug("Updated todo {}", todo.getId());
        } catch (SQLException e) {
            throw new StorageException("Failed to update todo", e);
        }
    }

    @Override
    public void delete(String id) throws StorageException {
        final String sql = "DELETE FROM todos WHERE id = ?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
            logger.debug("Deleted todo {}", id);
        } catch (SQLException e) {
            throw new StorageException("Failed to delete todo", e);
        }
    }

    private Todo mapRow(ResultSet rs) throws SQLException {
        return new Todo(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getBoolean("completed")
        );
    }
}
