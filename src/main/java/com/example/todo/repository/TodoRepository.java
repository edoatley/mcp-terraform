package com.example.todo.repository;

import com.example.todo.model.Todo;

import java.util.List;
import java.util.Optional;

public interface TodoRepository {
    List<Todo> findAll();
    
    Optional<Todo> findById(String id);
    
    Todo save(Todo todo);
    
    void deleteById(String id);
}

