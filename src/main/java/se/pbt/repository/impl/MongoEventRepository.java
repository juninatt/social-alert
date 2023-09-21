package se.pbt.repository.impl;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.inject.Singleton;
import org.bson.Document;
import org.bson.types.ObjectId;
import se.pbt.config.MongoConfig;
import se.pbt.converter.EventConverter;
import se.pbt.domain.Event;
import se.pbt.exception.*;
import se.pbt.repository.EventRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the {@link EventRepository} interface using MongoDB as the underlying datastore.
 * This repository handles CRUD operations for {@link Event} objects.
 */
@Singleton
public class MongoEventRepository implements EventRepository {

    private final MongoCollection<Document> collection;


    /**
     * Constructor initializing the MongoDB collection to be used for operations.
     *
     * @param mongoClient  The MongoDB client instance.
     * @param config       The configuration object containing MongoDB settings.
     */
    public MongoEventRepository(MongoClient mongoClient, MongoConfig config) {
        try {
        MongoDatabase database = mongoClient.getDatabase(config.getDatabase());
        this.collection = database.getCollection(config.getCollection());
        } catch (Exception exception) {
            throw new DatabaseConnectionException("Failed to connect to the database", exception.getCause());
        }
    }

    /**
     * Saves an event object to the MongoDB collection.
     *
     * @param event The event to be saved.
     * @return The saved event with its generated ID.
     */
    @Override
    public Event save(Event event) {
        Document document = EventConverter.toDocument(event);
        try {
            collection.insertOne(document);
             return new Event(event, document.getObjectId("_id").toString());
        } catch (Exception exception) {
            throw new EventSavingException("Problem saving event. Save aborted");
        }
    }

    /**
     * Retrieves all events from the MongoDB collection.
     *
     * @return A list of all events.
     */
    @Override
    public List<Event> findAll() {
        List<Event> events = new ArrayList<>();
        try {
            for (Document document : collection.find()) {
                events.add(EventConverter.toEvent(document));
            }
        } catch (RuntimeException exception) {
            throw new EventConversionException("Failed to convert documents to objects while fetching from the database");
        }
        return events;
    }

    /**
     * Finds an event by its ID.
     *
     * @param id The ID of the event to be retrieved.
     * @return An Optional containing the found event, or empty if no event was found.
     */
    @Override
    public Optional<Event> findById(String id) {
        Document document = collection.find(new Document("_id", new ObjectId(id))).first();
        if (document == null) {
            throw new EventNotFoundException(id);
        }
        return Optional.of(EventConverter.toEvent(document));
    }

    /**
     * Deletes an event by its ID.
     *
     * @param id The ID of the event to be deleted.
     * @return True if the deletion was successful, false otherwise.
     */
    @Override
    public boolean deleteById(String id) {
        try {
            collection.deleteOne(new Document("_id", new ObjectId(id)));
            return true;
        } catch (RuntimeException exception) {
            throw new EventDeletionException("Failed to delete event with ID: " + id, exception.getCause());
        }
    }

    @Override
    public void clearAll() {
        collection.drop();
    }
}
