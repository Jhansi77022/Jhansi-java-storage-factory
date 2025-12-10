package com.example.service;

import com.example.model.Todo;
import com.example.storage.StorageException;
import com.example.storage.TodoStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public final class TodoService {
    private static final Logger logger = LoggerFactory.getLogger(TodoService.class);
    private final TodoStorage storage;

    public TodoService(TodoStorage storage) {
        this.storage = storage;
    }

    public void addTodo(Todo todo) {
        try {
            storage.save(todo);
            logger.info("Added todo {}", todo.getId());
        } catch (StorageException e) {
            logger.error("Failed to add todo {}", todo.getId(), e);
            throw new TodoServiceException("Failed to add todo " + todo.getId(), e);
        }
    }

    public Optional<Todo> getTodo(String id) {
        try {
            return storage.retrieve(id);
        } catch (StorageException e) {
            logger.error("Failed to get todo {}", id, e);
            throw new TodoServiceException("Failed to retrieve todo " + id, e);
        }
    }

    public List<Todo> getAllTodos() {
        try {
            return storage.retrieveAll();
        } catch (StorageException e) {
            logger.error("Failed to fetch all todos", e);
            throw new TodoServiceException("Failed to fetch all todos", e);
        }
    }

    public void updateTodo(Todo todo) {
        try {
            storage.update(todo);
            logger.info("Updated todo {}", todo.getId());
        } catch (StorageException e) {
            logger.error("Failed to update todo {}", todo.getId(), e);
            throw new TodoServiceException("Failed to update todo " + todo.getId(), e);
        }
    }

    public void deleteTodo(String id) {
        try {
            storage.delete(id);
            logger.info("Deleted todo {}", id);
        } catch (StorageException e) {
            logger.error("Failed to delete todo {}", id, e);
            throw new TodoServiceException("Failed to delete todo " + id, e);
        }
    }
}
