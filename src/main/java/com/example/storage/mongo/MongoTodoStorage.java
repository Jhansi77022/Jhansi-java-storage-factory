package com.example.storage.mongo;

import com.example.model.Todo;
import com.example.storage.StorageException;
import com.example.storage.TodoStorage;
import com.mongodb.client.*;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;

public final class MongoTodoStorage implements TodoStorage {

    private static final Logger logger = LoggerFactory.getLogger(MongoTodoStorage.class);

    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoTodoStorage(String connectionString, String database, String collectionName) {

        try {
            this.client = MongoClients.create(connectionString);
            MongoDatabase db = client.getDatabase(database);
            this.collection = db.getCollection(collectionName);

            logger.info("Connected to MongoDB: {}/{}", database, collectionName);

        } catch (Exception e) {
            logger.error("MongoDB initialization failed", e);
            throw new RuntimeException("MongoDB initialization failed", e);
        }
    }

    @Override
    public void save(Todo todo) throws StorageException {
        try {
            Document doc = new Document()
                    .append("title", todo.getTitle())
                    .append("description", todo.getDescription())
                    .append("completed", todo.isCompleted());

            collection.insertOne(doc);

            todo.setId(doc.getObjectId("_id").toHexString());
            logger.debug("Mongo: saved {}", todo.getId());

        } catch (Exception e) {
            throw new StorageException("Failed to save Mongo todo", e);
        }
    }

    @Override
    public Optional<Todo> retrieve(String id) throws StorageException {
        try {
            Document d = collection.find(eq("_id", new org.bson.types.ObjectId(id))).first();
            if (d == null) return Optional.empty();

            return Optional.of(map(d));

        } catch (Exception e) {
            throw new StorageException("Failed to retrieve Mongo todo", e);
        }
    }

    @Override
    public List<Todo> retrieveAll() throws StorageException {
        try {
            List<Todo> list = new ArrayList<>();
            MongoCursor<Document> cursor = collection.find().iterator();

            while (cursor.hasNext()) list.add(map(cursor.next()));
            return list;

        } catch (Exception e) {
            throw new StorageException("Failed to retrieve all from Mongo", e);
        }
    }

    @Override
    public void update(Todo todo) throws StorageException {
        try {
            Document update = new Document("$set", new Document()
                    .append("title", todo.getTitle())
                    .append("description", todo.getDescription())
                    .append("completed", todo.isCompleted()));

            var result = collection.updateOne(
                    eq("_id", new org.bson.types.ObjectId(todo.getId())),
                    update
            );

            if (result.getMatchedCount() == 0)
                throw new StorageException("Mongo update failed, ID not found");

        } catch (Exception e) {
            throw new StorageException("Failed to update Mongo todo", e);
        }
    }

    @Override
    public void delete(String id) throws StorageException {
        try {
            collection.deleteOne(eq("_id", new org.bson.types.ObjectId(id)));
        } catch (Exception e) {
            throw new StorageException("Failed to delete Mongo todo", e);
        }
    }

    private Todo map(Document d) {
        return new Todo(
                d.getObjectId("_id").toHexString(),
                d.getString("title"),
                d.getString("description"),
                d.getBoolean("completed")
        );
    }
}
