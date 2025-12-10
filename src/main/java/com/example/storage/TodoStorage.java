package com.example.storage;

import com.example.model.Todo;

import java.util.List;
import java.util.Optional;

public interface TodoStorage {
    void save(Todo todo) throws StorageException;
    Optional<Todo> retrieve(String id) throws StorageException;
    List<Todo> retrieveAll() throws StorageException;
    void update(Todo todo) throws StorageException;
    void delete(String id) throws StorageException;
}
