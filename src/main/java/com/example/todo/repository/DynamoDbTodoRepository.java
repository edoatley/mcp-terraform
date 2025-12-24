package com.example.todo.repository;

import com.example.todo.model.Todo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DynamoDB implementation of TodoRepository.
 * Activated when Spring profile "dynamodb" is active or when no profile is specified (default for production).
 */
@Repository
@Profile("!in-memory")
public class DynamoDbTodoRepository implements TodoRepository {

    private final DynamoDbTable<Todo> todoTable;

    public DynamoDbTodoRepository(
            DynamoDbEnhancedClient dynamoDbEnhancedClient,
            @Value("${DYNAMODB_TABLE_NAME:${dynamodb.table.name:}}") String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalStateException("DYNAMODB_TABLE_NAME environment variable or dynamodb.table.name property is required");
        }
        this.todoTable = dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(Todo.class));
    }

    @Override
    public List<Todo> findAll() {
        try {
            return todoTable.scan().items().stream().collect(Collectors.toList());
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to retrieve todos from DynamoDB", e);
        }
    }

    @Override
    public Optional<Todo> findById(String id) {
        try {
            Key key = Key.builder()
                    .partitionValue(id)
                    .build();
            Todo todo = todoTable.getItem(key);
            return Optional.ofNullable(todo);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to retrieve todo from DynamoDB", e);
        }
    }

    @Override
    public Todo save(Todo todo) {
        try {
            todoTable.putItem(todo);
            return todo;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to save todo to DynamoDB", e);
        }
    }

    @Override
    public void deleteById(String id) {
        try {
            Key key = Key.builder()
                    .partitionValue(id)
                    .build();
            todoTable.deleteItem(key);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to delete todo from DynamoDB", e);
        }
    }
}

