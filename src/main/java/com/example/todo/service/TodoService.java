package com.example.todo.service;

import com.example.todo.model.Todo;
import com.example.todo.repository.TodoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TodoService {
    private final TodoRepository todoRepository;
    
    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    public List<Todo> getAllTodos() {
        return todoRepository.findAll();
    }
    
    public Optional<Todo> getTodoById(String id) {
        return todoRepository.findById(id);
    }
    
    public Todo createTodo(Todo todo) {
        if (todo.getId() == null) {
            todo = new Todo(todo.getTitle(), todo.getDescription());
        }
        return todoRepository.save(todo);
    }
    
    public Optional<Todo> updateTodo(String id, Todo todo) {
        Optional<Todo> existing = todoRepository.findById(id);
        if (existing.isPresent()) {
            todo.setId(id);
            Todo updated = todoRepository.save(todo);
            return Optional.of(updated);
        }
        return Optional.empty();
    }
    
    public boolean deleteTodo(String id) {
        Optional<Todo> existing = todoRepository.findById(id);
        if (existing.isPresent()) {
            todoRepository.deleteById(id);
            return true;
        }
        return false;
    }
}

