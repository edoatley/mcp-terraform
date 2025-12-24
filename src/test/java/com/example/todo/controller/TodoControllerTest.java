package com.example.todo.controller;

import com.example.todo.model.Todo;
import com.example.todo.service.TodoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TodoController.class)
@DisplayName("TodoController Unit Tests")
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoService todoService;

    @Autowired
    private ObjectMapper objectMapper;

    private Todo testTodo;

    @BeforeEach
    void setUp() {
        testTodo = new Todo("test-id", "Test Title", "Test Description", false);
    }

    @Test
    @DisplayName("GET /api/todos should return all todos")
    void getAllTodos_ReturnsAllTodos() throws Exception {
        // Given
        List<Todo> todos = Arrays.asList(
                new Todo("id1", "Title 1", "Description 1", false),
                new Todo("id2", "Title 2", "Description 2", true)
        );
        when(todoService.getAllTodos()).thenReturn(todos);

        // When & Then
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("id1"))
                .andExpect(jsonPath("$[0].title").value("Title 1"))
                .andExpect(jsonPath("$[1].id").value("id2"))
                .andExpect(jsonPath("$[1].title").value("Title 2"));

        verify(todoService, times(1)).getAllTodos();
    }

    @Test
    @DisplayName("GET /api/todos/{id} should return todo when it exists")
    void getTodoById_WhenExists_ReturnsTodo() throws Exception {
        // Given
        when(todoService.getTodoById("test-id")).thenReturn(Optional.of(testTodo));

        // When & Then
        mockMvc.perform(get("/api/todos/test-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("test-id"))
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.completed").value(false));

        verify(todoService, times(1)).getTodoById("test-id");
    }

    @Test
    @DisplayName("GET /api/todos/{id} should return 404 when todo does not exist")
    void getTodoById_WhenNotExists_ReturnsNotFound() throws Exception {
        // Given
        when(todoService.getTodoById("non-existent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/todos/non-existent"))
                .andExpect(status().isNotFound());

        verify(todoService, times(1)).getTodoById("non-existent");
    }

    @Test
    @DisplayName("POST /api/todos should create todo and return 201")
    void createTodo_CreatesTodoAndReturnsCreated() throws Exception {
        // Given
        Todo newTodo = new Todo("New Title", "New Description");
        Todo createdTodo = new Todo("new-id", "New Title", "New Description", false);
        when(todoService.createTodo(any(Todo.class))).thenReturn(createdTodo);

        // When & Then
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTodo)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("new-id"))
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.description").value("New Description"));

        verify(todoService, times(1)).createTodo(any(Todo.class));
    }

    @Test
    @DisplayName("PUT /api/todos/{id} should update todo when it exists")
    void updateTodo_WhenExists_UpdatesAndReturnsTodo() throws Exception {
        // Given
        Todo updatedTodo = new Todo("test-id", "Updated Title", "Updated Description", true);
        when(todoService.updateTodo(eq("test-id"), any(Todo.class)))
                .thenReturn(Optional.of(updatedTodo));

        // When & Then
        mockMvc.perform(put("/api/todos/test-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedTodo)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("test-id"))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.completed").value(true));

        verify(todoService, times(1)).updateTodo(eq("test-id"), any(Todo.class));
    }

    @Test
    @DisplayName("PUT /api/todos/{id} should return 404 when todo does not exist")
    void updateTodo_WhenNotExists_ReturnsNotFound() throws Exception {
        // Given
        Todo todoToUpdate = new Todo("Updated Title", "Updated Description");
        when(todoService.updateTodo(eq("non-existent"), any(Todo.class)))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/todos/non-existent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(todoToUpdate)))
                .andExpect(status().isNotFound());

        verify(todoService, times(1)).updateTodo(eq("non-existent"), any(Todo.class));
    }

    @Test
    @DisplayName("DELETE /api/todos/{id} should return 204 when todo exists")
    void deleteTodo_WhenExists_ReturnsNoContent() throws Exception {
        // Given
        when(todoService.deleteTodo("test-id")).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/todos/test-id"))
                .andExpect(status().isNoContent());

        verify(todoService, times(1)).deleteTodo("test-id");
    }

    @Test
    @DisplayName("DELETE /api/todos/{id} should return 404 when todo does not exist")
    void deleteTodo_WhenNotExists_ReturnsNotFound() throws Exception {
        // Given
        when(todoService.deleteTodo("non-existent")).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/todos/non-existent"))
                .andExpect(status().isNotFound());

        verify(todoService, times(1)).deleteTodo("non-existent");
    }

    @Test
    @DisplayName("POST /api/todos should handle invalid JSON gracefully")
    void createTodo_WithInvalidJson_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest());

        verify(todoService, never()).createTodo(any());
    }

    @Test
    @DisplayName("GET /api/todos should return empty array when no todos exist")
    void getAllTodos_WhenNoTodos_ReturnsEmptyArray() throws Exception {
        // Given
        when(todoService.getAllTodos()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(todoService, times(1)).getAllTodos();
    }

    @Test
    @DisplayName("POST /api/todos should return 400 when title is null")
    void createTodo_WithNullTitle_ReturnsBadRequest() throws Exception {
        // Given
        Todo invalidTodo = new Todo();
        invalidTodo.setTitle(null);
        invalidTodo.setDescription("Description");

        // When & Then
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTodo)))
                .andExpect(status().isBadRequest());

        verify(todoService, never()).createTodo(any());
    }

    @Test
    @DisplayName("POST /api/todos should return 400 when title is blank")
    void createTodo_WithBlankTitle_ReturnsBadRequest() throws Exception {
        // Given
        Todo invalidTodo = new Todo();
        invalidTodo.setTitle("   ");
        invalidTodo.setDescription("Description");

        // When & Then
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTodo)))
                .andExpect(status().isBadRequest());

        verify(todoService, never()).createTodo(any());
    }

    @Test
    @DisplayName("POST /api/todos should return 400 when title exceeds 200 characters")
    void createTodo_WithTitleTooLong_ReturnsBadRequest() throws Exception {
        // Given
        Todo invalidTodo = new Todo();
        invalidTodo.setTitle("a".repeat(201));
        invalidTodo.setDescription("Description");

        // When & Then
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTodo)))
                .andExpect(status().isBadRequest());

        verify(todoService, never()).createTodo(any());
    }

    @Test
    @DisplayName("POST /api/todos should return 400 when description exceeds 500 characters")
    void createTodo_WithDescriptionTooLong_ReturnsBadRequest() throws Exception {
        // Given
        Todo invalidTodo = new Todo();
        invalidTodo.setTitle("Valid Title");
        invalidTodo.setDescription("a".repeat(501));

        // When & Then
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTodo)))
                .andExpect(status().isBadRequest());

        verify(todoService, never()).createTodo(any());
    }

    @Test
    @DisplayName("PUT /api/todos/{id} should return 400 when title is blank")
    void updateTodo_WithBlankTitle_ReturnsBadRequest() throws Exception {
        // Given
        Todo invalidTodo = new Todo();
        invalidTodo.setId("test-id");
        invalidTodo.setTitle("");
        invalidTodo.setDescription("Description");

        // When & Then
        mockMvc.perform(put("/api/todos/test-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTodo)))
                .andExpect(status().isBadRequest());

        verify(todoService, never()).updateTodo(anyString(), any());
    }
}

