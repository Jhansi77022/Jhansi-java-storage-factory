package com.example.factory;

import com.example.storage.TodoStorage;
import com.example.storage.StorageException;
import com.example.storage.mysql.MySqlTodoStorage;
import com.example.storage.mongo.MongoTodoStorage;
import com.example.storage.postgres.PostgresTodoStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public final class StorageFactory {

    private static final Logger logger = LoggerFactory.getLogger(StorageFactory.class);

    private StorageFactory() {}

    public static TodoStorage create(String type, Properties props) {
        logger.info("Creating storage of type: {}", type);

        return switch (type.toLowerCase()) {

            case "mysql" -> {
                String url  = props.getProperty("mysql.jdbcUrl");
                String user = props.getProperty("mysql.user");
                String pass = props.getProperty("mysql.password");
                yield new MySqlTodoStorage(url, user, pass);
            }

            case "mongodb", "mongo" -> {
                String conn = props.getProperty("mongo.connectionString");
                String db   = props.getProperty("mongo.database");
                String col  = props.getProperty("mongo.collection");
                yield new MongoTodoStorage(conn, db, col);
            }

            case "postgres", "postgresql" -> {
                String url  = props.getProperty("postgres.jdbcUrl");
                String user = props.getProperty("postgres.user");
                String pass = props.getProperty("postgres.password");
                yield new PostgresTodoStorage(url, user, pass);
            }

            default -> throw new StorageException("Unknown storage type: " + type);
        };
    }
}
