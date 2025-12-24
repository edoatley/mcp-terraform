package com.example.todo.service;

import com.example.todo.model.Todo;
import com.example.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("TodoService Unit Tests")
class TodoServiceTest {

    private TodoService todoService;
    private TodoRepository todoRepository;

    @BeforeEach
    void setUp() {
        todoRepository = mock(TodoRepository.class);
        todoService = new TodoService(todoRepository);
    }

    @Test
    @DisplayName("Should return empty list when no todos exist")
    void getAllTodos_WhenNoTodos_ReturnsEmptyList() {
        // Given
        when(todoRepository.findAll()).thenReturn(List.of());
        
        // When
        List<Todo> todos = todoService.getAllTodos();
        
        // Then
        assertNotNull(todos);
        assertTrue(todos.isEmpty());
        verify(todoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return all todos when todos exist")
    void getAllTodos_WhenTodosExist_ReturnsAllTodos() {
        // Given
        Todo todo1 = new Todo("Title 1", "Description 1");
        Todo todo2 = new Todo("Title 2", "Description 2");
        when(todoRepository.findAll()).thenReturn(List.of(todo1, todo2));

        // When
        List<Todo> todos = todoService.getAllTodos();

        // Then
        assertNotNull(todos);
        assertEquals(2, todos.size());
        assertTrue(todos.contains(todo1));
        assertTrue(todos.contains(todo2));
        verify(todoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty Optional when todo does not exist")
    void getTodoById_WhenTodoDoesNotExist_ReturnsEmptyOptional() {
        // Given
        when(todoRepository.findById("non-existent-id")).thenReturn(Optional.empty());
        
        // When
        Optional<Todo> result = todoService.getTodoById("non-existent-id");
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(todoRepository, times(1)).findById("non-existent-id");
    }

    @Test
    @DisplayName("Should return todo when it exists")
    void getTodoById_WhenTodoExists_ReturnsTodo() {
        // Given
        Todo todo = new Todo("Test Title", "Test Description");
        String todoId = todo.getId();
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));

        // When
        Optional<Todo> result = todoService.getTodoById(todoId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(todo, result.get());
        assertEquals("Test Title", result.get().getTitle());
        assertEquals("Test Description", result.get().getDescription());
        verify(todoRepository, times(1)).findById(todoId);
    }

    @Test
    @DisplayName("Should create todo with generated ID when ID is null")
    void createTodo_WhenIdIsNull_GeneratesIdAndCreatesTodo() {
        // Given
        Todo todo = new Todo();
        todo.setTitle("New Todo");
        todo.setDescription("New Description");
        assertNull(todo.getId());
        
        Todo savedTodo = new Todo("New Todo", "New Description");
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> {
            Todo t = invocation.getArgument(0);
            return t;
        });

        // When
        Todo created = todoService.createTodo(todo);

        // Then
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("New Todo", created.getTitle());
        assertEquals("New Description", created.getDescription());
        assertFalse(created.isCompleted());
        verify(todoRepository, times(1)).save(any(Todo.class));
    }

    @Test
    @DisplayName("Should create todo with existing ID when ID is provided")
    void createTodo_WhenIdIsProvided_UsesExistingId() {
        // Given
        Todo todo = new Todo("custom-id", "Title", "Description", false);
        when(todoRepository.save(todo)).thenReturn(todo);

        // When
        Todo created = todoService.createTodo(todo);

        // Then
        assertNotNull(created);
        assertEquals("custom-id", created.getId());
        assertEquals("Title", created.getTitle());
        verify(todoRepository, times(1)).save(todo);
    }

    @Test
    @DisplayName("Should store todo in service after creation")
    void createTodo_StoresTodoInService() {
        // Given
        Todo todo = new Todo("Test", "Description");
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(todoRepository.findById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            Todo saved = new Todo("Test", "Description");
            saved.setId(id);
            return Optional.of(saved);
        });

        // When
        Todo created = todoService.createTodo(todo);

        // Then
        Optional<Todo> retrieved = todoService.getTodoById(created.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(created.getTitle(), retrieved.get().getTitle());
        verify(todoRepository, times(1)).save(any(Todo.class));
    }

    @Test
    @DisplayName("Should return empty Optional when updating non-existent todo")
    void updateTodo_WhenTodoDoesNotExist_ReturnsEmptyOptional() {
        // Given
        Todo todo = new Todo("Title", "Description");
        when(todoRepository.findById("non-existent-id")).thenReturn(Optional.empty());

        // When
        Optional<Todo> result = todoService.updateTodo("non-existent-id", todo);

        // Then
        assertTrue(result.isEmpty());
        verify(todoRepository, times(1)).findById("non-existent-id");
        verify(todoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update existing todo and return it")
    void updateTodo_WhenTodoExists_UpdatesAndReturnsTodo() {
        // Given
        String todoId = "test-id";
        Todo original = new Todo(todoId, "Original Title", "Original Description", false);
        Todo updated = new Todo(todoId, "Updated Title", "Updated Description", true);
        
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(original));
        when(todoRepository.save(any(Todo.class))).thenReturn(updated);

        // When
        Optional<Todo> result = todoService.updateTodo(todoId, updated);

        // Then
        assertTrue(result.isPresent());
        Todo updatedTodo = result.get();
        assertEquals(todoId, updatedTodo.getId());
        assertEquals("Updated Title", updatedTodo.getTitle());
        assertEquals("Updated Description", updatedTodo.getDescription());
        assertTrue(updatedTodo.isCompleted());
        verify(todoRepository, times(1)).findById(todoId);
        verify(todoRepository, times(1)).save(any(Todo.class));
    }

    @Test
    @DisplayName("Should persist updated todo in service")
    void updateTodo_PersistsUpdatedTodo() {
        // Given
        String todoId = "test-id";
        Todo original = new Todo(todoId, "Original", "Description", false);
        Todo updated = new Todo(todoId, "Updated", "New Description", true);
        
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(original));
        when(todoRepository.save(any(Todo.class))).thenReturn(updated);
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(updated));

        // When
        todoService.updateTodo(todoId, updated);

        // Then
        Optional<Todo> retrieved = todoService.getTodoById(todoId);
        assertTrue(retrieved.isPresent());
        assertEquals("Updated", retrieved.get().getTitle());
        assertTrue(retrieved.get().isCompleted());
        verify(todoRepository, atLeastOnce()).findById(todoId);
        verify(todoRepository, times(1)).save(any(Todo.class));
    }

    @Test
    @DisplayName("Should return false when deleting non-existent todo")
    void deleteTodo_WhenTodoDoesNotExist_ReturnsFalse() {
        // Given
        when(todoRepository.findById("non-existent-id")).thenReturn(Optional.empty());
        
        // When
        boolean result = todoService.deleteTodo("non-existent-id");
        
        // Then
        assertFalse(result);
        verify(todoRepository, times(1)).findById("non-existent-id");
        verify(todoRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("Should return true and remove todo when deleting existing todo")
    void deleteTodo_WhenTodoExists_ReturnsTrueAndRemovesTodo() {
        // Given
        String todoId = "todo-to-delete";
        Todo todo = new Todo(todoId, "To Delete", "Description", false);
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        doNothing().when(todoRepository).deleteById(todoId);

        // When
        boolean result = todoService.deleteTodo(todoId);

        // Then
        assertTrue(result);
        verify(todoRepository, times(1)).findById(todoId);
        verify(todoRepository, times(1)).deleteById(todoId);
    }

    @Test
    @DisplayName("Should handle multiple operations correctly")
    void multipleOperations_WorkCorrectly() {
        // Given
        Todo todo1 = new Todo("todo-1", "Todo 1", "Description 1", false);
        Todo todo2 = new Todo("todo-2", "Todo 2", "Description 2", false);
        Todo todo3 = new Todo("todo-3", "Todo 3", "Description 3", false);
        Todo updatedTodo2 = new Todo("todo-2", "Updated Todo 2", "Updated Description 2", false);
        
        when(todoRepository.findAll())
                .thenReturn(List.of(todo1, todo2, todo3))
                .thenReturn(List.of(todo1, updatedTodo2));
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(todoRepository.findById("todo-2")).thenReturn(Optional.of(todo2));
        when(todoRepository.findById("todo-3")).thenReturn(Optional.of(todo3));
        when(todoRepository.save(updatedTodo2)).thenReturn(updatedTodo2);
        doNothing().when(todoRepository).deleteById("todo-3");

        // Verify all exist
        assertEquals(3, todoService.getAllTodos().size());

        // Update one
        todoService.updateTodo(todo2.getId(), updatedTodo2);

        // Delete one
        todoService.deleteTodo(todo3.getId());

        // Verify final state
        List<Todo> remaining = todoService.getAllTodos();
        assertEquals(2, remaining.size());
        assertTrue(remaining.stream().anyMatch(t -> t.getId().equals(todo1.getId())));
        assertTrue(remaining.stream().anyMatch(t -> t.getId().equals(todo2.getId())));
    }
}

