package com.example.todo.service;

import com.example.todo.model.Todo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TodoService Unit Tests")
class TodoServiceTest {

    private TodoService todoService;

    @BeforeEach
    void setUp() {
        todoService = new TodoService();
    }

    @Test
    @DisplayName("Should return empty list when no todos exist")
    void getAllTodos_WhenNoTodos_ReturnsEmptyList() {
        List<Todo> todos = todoService.getAllTodos();
        
        assertNotNull(todos);
        assertTrue(todos.isEmpty());
    }

    @Test
    @DisplayName("Should return all todos when todos exist")
    void getAllTodos_WhenTodosExist_ReturnsAllTodos() {
        // Given
        Todo todo1 = new Todo("Title 1", "Description 1");
        Todo todo2 = new Todo("Title 2", "Description 2");
        todoService.createTodo(todo1);
        todoService.createTodo(todo2);

        // When
        List<Todo> todos = todoService.getAllTodos();

        // Then
        assertNotNull(todos);
        assertEquals(2, todos.size());
        assertTrue(todos.contains(todo1));
        assertTrue(todos.contains(todo2));
    }

    @Test
    @DisplayName("Should return empty Optional when todo does not exist")
    void getTodoById_WhenTodoDoesNotExist_ReturnsEmptyOptional() {
        Optional<Todo> result = todoService.getTodoById("non-existent-id");
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return todo when it exists")
    void getTodoById_WhenTodoExists_ReturnsTodo() {
        // Given
        Todo todo = new Todo("Test Title", "Test Description");
        todoService.createTodo(todo);
        String todoId = todo.getId();

        // When
        Optional<Todo> result = todoService.getTodoById(todoId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(todo, result.get());
        assertEquals("Test Title", result.get().getTitle());
        assertEquals("Test Description", result.get().getDescription());
    }

    @Test
    @DisplayName("Should create todo with generated ID when ID is null")
    void createTodo_WhenIdIsNull_GeneratesIdAndCreatesTodo() {
        // Given
        Todo todo = new Todo();
        todo.setTitle("New Todo");
        todo.setDescription("New Description");
        assertNull(todo.getId());

        // When
        Todo created = todoService.createTodo(todo);

        // Then
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("New Todo", created.getTitle());
        assertEquals("New Description", created.getDescription());
        assertFalse(created.isCompleted());
    }

    @Test
    @DisplayName("Should create todo with existing ID when ID is provided")
    void createTodo_WhenIdIsProvided_UsesExistingId() {
        // Given
        Todo todo = new Todo("custom-id", "Title", "Description", false);

        // When
        Todo created = todoService.createTodo(todo);

        // Then
        assertNotNull(created);
        assertEquals("custom-id", created.getId());
        assertEquals("Title", created.getTitle());
    }

    @Test
    @DisplayName("Should store todo in service after creation")
    void createTodo_StoresTodoInService() {
        // Given
        Todo todo = new Todo("Test", "Description");

        // When
        Todo created = todoService.createTodo(todo);

        // Then
        Optional<Todo> retrieved = todoService.getTodoById(created.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(created, retrieved.get());
    }

    @Test
    @DisplayName("Should return empty Optional when updating non-existent todo")
    void updateTodo_WhenTodoDoesNotExist_ReturnsEmptyOptional() {
        // Given
        Todo todo = new Todo("Title", "Description");

        // When
        Optional<Todo> result = todoService.updateTodo("non-existent-id", todo);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should update existing todo and return it")
    void updateTodo_WhenTodoExists_UpdatesAndReturnsTodo() {
        // Given
        Todo original = new Todo("Original Title", "Original Description");
        todoService.createTodo(original);
        String todoId = original.getId();

        Todo updated = new Todo("Updated Title", "Updated Description");
        updated.setCompleted(true);

        // When
        Optional<Todo> result = todoService.updateTodo(todoId, updated);

        // Then
        assertTrue(result.isPresent());
        Todo updatedTodo = result.get();
        assertEquals(todoId, updatedTodo.getId());
        assertEquals("Updated Title", updatedTodo.getTitle());
        assertEquals("Updated Description", updatedTodo.getDescription());
        assertTrue(updatedTodo.isCompleted());
    }

    @Test
    @DisplayName("Should persist updated todo in service")
    void updateTodo_PersistsUpdatedTodo() {
        // Given
        Todo original = new Todo("Original", "Description");
        todoService.createTodo(original);
        String todoId = original.getId();

        Todo updated = new Todo("Updated", "New Description");
        updated.setCompleted(true);

        // When
        todoService.updateTodo(todoId, updated);

        // Then
        Optional<Todo> retrieved = todoService.getTodoById(todoId);
        assertTrue(retrieved.isPresent());
        assertEquals("Updated", retrieved.get().getTitle());
        assertTrue(retrieved.get().isCompleted());
    }

    @Test
    @DisplayName("Should return false when deleting non-existent todo")
    void deleteTodo_WhenTodoDoesNotExist_ReturnsFalse() {
        boolean result = todoService.deleteTodo("non-existent-id");
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return true and remove todo when deleting existing todo")
    void deleteTodo_WhenTodoExists_ReturnsTrueAndRemovesTodo() {
        // Given
        Todo todo = new Todo("To Delete", "Description");
        todoService.createTodo(todo);
        String todoId = todo.getId();

        // When
        boolean result = todoService.deleteTodo(todoId);

        // Then
        assertTrue(result);
        Optional<Todo> deleted = todoService.getTodoById(todoId);
        assertTrue(deleted.isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple operations correctly")
    void multipleOperations_WorkCorrectly() {
        // Create multiple todos
        Todo todo1 = new Todo("Todo 1", "Description 1");
        Todo todo2 = new Todo("Todo 2", "Description 2");
        Todo todo3 = new Todo("Todo 3", "Description 3");
        
        todoService.createTodo(todo1);
        todoService.createTodo(todo2);
        todoService.createTodo(todo3);

        // Verify all exist
        assertEquals(3, todoService.getAllTodos().size());

        // Update one
        Todo updated = new Todo("Updated Todo 2", "Updated Description 2");
        todoService.updateTodo(todo2.getId(), updated);

        // Delete one
        todoService.deleteTodo(todo3.getId());

        // Verify final state
        List<Todo> remaining = todoService.getAllTodos();
        assertEquals(2, remaining.size());
        assertTrue(remaining.stream().anyMatch(t -> t.getId().equals(todo1.getId())));
        assertTrue(remaining.stream().anyMatch(t -> t.getId().equals(todo2.getId())));
        assertTrue(remaining.stream().noneMatch(t -> t.getId().equals(todo3.getId())));
    }
}

