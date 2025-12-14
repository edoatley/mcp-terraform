package com.example.todo.service;

import com.example.todo.model.Todo;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TodoService {
    private final Map<String, Todo> todos = new ConcurrentHashMap<>();
    
    public List<Todo> getAllTodos() {
        return new ArrayList<>(todos.values());
    }
    
    public Optional<Todo> getTodoById(String id) {
        return Optional.ofNullable(todos.get(id));
    }
    
    public Todo createTodo(Todo todo) {
        if (todo.getId() == null) {
            todo = new Todo(todo.getTitle(), todo.getDescription());
        }
        todos.put(todo.getId(), todo);
        return todo;
    }
    
    public Optional<Todo> updateTodo(String id, Todo todo) {
        if (todos.containsKey(id)) {
            todo.setId(id);
            todos.put(id, todo);
            return Optional.of(todo);
        }
        return Optional.empty();
    }
    
    public boolean deleteTodo(String id) {
        return todos.remove(id) != null;
    }
}

