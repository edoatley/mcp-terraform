package com.example.todo.repository;

import com.example.todo.model.Todo;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of TodoRepository for local development and testing.
 * Activated when Spring profile "in-memory" is active.
 */
@Repository
@Profile("in-memory")
public class InMemoryTodoRepository implements TodoRepository {
    
    private final Map<String, Todo> todos = new ConcurrentHashMap<>();
    
    @Override
    public List<Todo> findAll() {
        return new ArrayList<>(todos.values());
    }
    
    @Override
    public Optional<Todo> findById(String id) {
        return Optional.ofNullable(todos.get(id));
    }
    
    @Override
    public Todo save(Todo todo) {
        todos.put(todo.getId(), todo);
        return todo;
    }
    
    @Override
    public void deleteById(String id) {
        todos.remove(id);
    }
}



