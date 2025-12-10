package com.example.app;

import com.example.factory.StorageFactory;
import com.example.model.Todo;
import com.example.service.TodoService;
import com.example.storage.TodoStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Scanner;

public final class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        Properties props = loadPropertiesFromEnv();

        String storageType = showDatabaseMenu();
        logger.info("User selected database: {}", storageType);

        TodoStorage storage = StorageFactory.create(storageType, props);
        TodoService service = new TodoService(storage);

        logger.info("Started TODO CLI using storage: {}", storageType);

        if (System.getProperty("skipCli") == null) {
            runCli(service);
        }

        logger.info("Shutting down CLI.");
    }

    private static String showDatabaseMenu() {
        Scanner sc = new Scanner(System.in);

        logger.info("");
        logger.info("Select Database:");
        logger.info("1. MySQL");
        logger.info("2. MongoDB");
        logger.info("3. PostgreSQL");
        logger.info("Enter choice (1-3): ");
        System.out.flush();

        String choice = sc.nextLine().trim();
        logger.debug("Database choice input: {}", choice);

        return switch (choice) {
            case "1" -> "mysql";
            case "2" -> "mongodb";
            case "3" -> "postgres";
            default -> {
                logger.warn("Invalid DB choice '{}' — defaulting to MySQL", choice);
                yield "mysql";
            }
        };
    }

    private static Properties loadPropertiesFromEnv() {
        Properties p = new Properties();

        // MySQL props
        p.setProperty("mysql.jdbcUrl",
                System.getenv().getOrDefault("MYSQL_JDBC_URL",
                        "jdbc:mysql://localhost:3306/todos_db?useSSL=false&allowPublicKeyRetrieval=true"));

        p.setProperty("mysql.user",
                System.getenv().getOrDefault("MYSQL_USER", "root"));

        p.setProperty("mysql.password",
                System.getenv().getOrDefault("MYSQL_PASS", "0000"));


        // PostgreSQL props
        p.setProperty("postgres.jdbcUrl",
                System.getenv().getOrDefault("POSTGRES_JDBC_URL",
                        "jdbc:postgresql://localhost:5432/todos_db"));

        p.setProperty("postgres.user",
                System.getenv().getOrDefault("POSTGRES_USER", "postgres"));

        p.setProperty("postgres.password",
                System.getenv().getOrDefault("POSTGRES_PASS", "0000"));


        // MongoDB props
        p.setProperty("mongo.connectionString",
                System.getenv().getOrDefault("MONGO_CONN", "mongodb://localhost:27017"));

        p.setProperty("mongo.database",
                System.getenv().getOrDefault("MONGO_DB", "todos_db"));

        p.setProperty("mongo.collection",
                System.getenv().getOrDefault("MONGO_COLLECTION", "todos"));

        return p;
    }


    static void runCli(TodoService service) {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;

            while (running) {
                printMenu();

                logger.info("> ");
                System.out.flush();

                String input = scanner.nextLine().trim();
                logger.debug("User input: '{}'", input);

                if (input.isEmpty()) {
                    logger.warn("Empty input received.");
                    continue;
                }

                String[] parts = input.split("\\s+", 2);
                String cmd = parts[0];
                String arg = parts.length > 1 ? parts[1] : "";

                switch (cmd) {
                    case "add" -> {
                        logger.info("Executing ADD command.");
                        createTodoInteractive(service, scanner);
                    }
                    case "get" -> {
                        logger.info("Executing GET command with id: {}", arg);
                        getTodoCmd(service, arg);
                    }
                    case "list" -> {
                        logger.info("Executing LIST command.");
                        listCmd(service);
                    }
                    case "update" -> {
                        logger.info("Executing UPDATE command with id: {}", arg);
                        updateTodoInteractive(service, scanner, arg);
                    }
                    case "delete" -> {
                        logger.info("Executing DELETE command with id: {}", arg);
                        deleteCmd(service, arg);
                    }
                    case "exit" -> {
                        logger.info("Exit command received.");
                        running = false;
                    }
                    default -> logger.warn("Invalid command received: {}", cmd);
                }
            }
        } catch (Exception ex) {
            logger.error("Unhandled error in CLI loop: ", ex);
        }
    }

    private static void printMenu() {
        logger.info("");
        logger.info("Options:");
        logger.info("  add               - Add a new todo");
        logger.info("  get <id>          - Get todo by id");
        logger.info("  list              - List all todos");
        logger.info("  update <id>       - Update a todo");
        logger.info("  delete <id>       - Delete a todo");
        logger.info("  exit              - Exit program");
    }

    private static void createTodoInteractive(TodoService service, Scanner scanner) {
        logger.info("Title: ");
        System.out.flush();
        String title = scanner.nextLine();

        logger.info("Description: ");
        System.out.flush();
        String desc = scanner.nextLine();

        Todo todo = new Todo(null, title, desc, false);
        service.addTodo(todo);

        logger.info("Successfully created TODO with ID: {}", todo.getId());
    }

    private static void getTodoCmd(TodoService service, String id) {
        if (id.isBlank()) {
            logger.warn("GET command called without ID.");
            logger.info("Please provide id: get <id>");
            return;
        }

        service.getTodo(id).ifPresentOrElse(
                t -> logger.info("Retrieved TODO: {}", t),
                () -> logger.warn("TODO not found for id: {}", id)
        );
    }

    private static void listCmd(TodoService service) {
        logger.debug("Listing all TODOs.");

        var list = service.getAllTodos();

        if (list.isEmpty()) {
            logger.info("No TODOs in database.");
            return;
        }

        list.forEach(todo -> logger.info(todo.toString()));
    }

    private static void updateTodoInteractive(TodoService service, Scanner scanner, String id) {
        if (id.isBlank()) {
            logger.warn("UPDATE command called without ID.");
            logger.info("Please provide id: update <id>");
            return;
        }

        var opt = service.getTodo(id);
        if (opt.isEmpty()) {
            logger.warn("TODO not found for update: {}", id);
            return;
        }

        var todo = opt.get();

        logger.info("Current title: {}", todo.getTitle());
        logger.info("New title (blank to keep): ");
        System.out.flush();
        String title = scanner.nextLine();
        if (!title.isBlank()) todo.setTitle(title);

        logger.info("Current description: {}", todo.getDescription());
        logger.info("New description (blank to keep): ");
        System.out.flush();
        String desc = scanner.nextLine();
        if (!desc.isBlank()) todo.setDescription(desc);

        logger.info("Current completed: {}", todo.isCompleted());
        logger.info("Mark completed? (y/N): ");
        System.out.flush();
        String comp = scanner.nextLine();
        if ("y".equalsIgnoreCase(comp.trim())) todo.setCompleted(true);

        service.updateTodo(todo);
        logger.info("Updated TODO with id {}", id);
    }

    private static void deleteCmd(TodoService service, String id) {
        if (id.isBlank()) {
            logger.warn("DELETE command called without ID.");
            logger.info("Please provide id: delete <id>");
            return;
        }

        service.deleteTodo(id);
        logger.info("Deleted TODO with id {}", id);
    }
}
