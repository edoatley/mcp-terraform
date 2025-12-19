package com.example.todo.integration;

import com.example.todo.model.Todo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Todo REST API Integration Tests")
class TodoRestIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/todos";
    }

    @Test
    @DisplayName("GET /api/todos should return empty list initially")
    void getAllTodos_Initially_ReturnsEmptyList() {
        ResponseEntity<Todo[]> response = restTemplate.getForEntity(baseUrl, Todo[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().length);
    }

    @Test
    @DisplayName("POST /api/todos should create a new todo")
    void createTodo_CreatesNewTodo() {
        Todo newTodo = new Todo("Integration Test Todo", "This is an integration test");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Todo> request = new HttpEntity<>(newTodo, headers);

        ResponseEntity<Todo> response = restTemplate.postForEntity(baseUrl, request, Todo.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        Todo created = response.getBody();
        assertNotNull(created.getId());
        assertEquals("Integration Test Todo", created.getTitle());
        assertEquals("This is an integration test", created.getDescription());
        assertFalse(created.isCompleted());
    }

    @Test
    @DisplayName("GET /api/todos/{id} should return created todo")
    void getTodoById_ReturnsCreatedTodo() {
        // Create a todo first
        Todo newTodo = new Todo("Get Test Todo", "Description for get test");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Todo> createRequest = new HttpEntity<>(newTodo, headers);
        ResponseEntity<Todo> createResponse = restTemplate.postForEntity(baseUrl, createRequest, Todo.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        Todo created = createResponse.getBody();
        assertNotNull(created);
        String todoId = created.getId();

        // Get the todo
        ResponseEntity<Todo> getResponse = restTemplate.getForEntity(baseUrl + "/" + todoId, Todo.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        Todo todo = getResponse.getBody();
        assertNotNull(todo);
        assertEquals(todoId, todo.getId());
        assertEquals("Get Test Todo", todo.getTitle());
        assertEquals("Description for get test", todo.getDescription());
    }

    @Test
    @DisplayName("GET /api/todos/{id} should return 404 for non-existent todo")
    void getTodoById_NonExistent_ReturnsNotFound() {
        ResponseEntity<Todo> response = restTemplate.getForEntity(baseUrl + "/non-existent-id", Todo.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("PUT /api/todos/{id} should update existing todo")
    void updateTodo_UpdatesExistingTodo() {
        // Create a todo first
        Todo newTodo = new Todo("Original Title", "Original Description");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Todo> createRequest = new HttpEntity<>(newTodo, headers);
        ResponseEntity<Todo> createResponse = restTemplate.postForEntity(baseUrl, createRequest, Todo.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        Todo created = createResponse.getBody();
        assertNotNull(created);
        String todoId = created.getId();

        // Update the todo
        Todo updatedTodo = new Todo(todoId, "Updated Title", "Updated Description", true);
        HttpEntity<Todo> updateRequest = new HttpEntity<>(updatedTodo, headers);
        ResponseEntity<Todo> updateResponse = restTemplate.exchange(
                baseUrl + "/" + todoId,
                HttpMethod.PUT,
                updateRequest,
                Todo.class
        );

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        Todo updated = updateResponse.getBody();
        assertNotNull(updated);
        assertEquals(todoId, updated.getId());
        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated Description", updated.getDescription());
        assertTrue(updated.isCompleted());

        // Verify the update persisted
        ResponseEntity<Todo> getResponse = restTemplate.getForEntity(baseUrl + "/" + todoId, Todo.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        Todo retrieved = getResponse.getBody();
        assertNotNull(retrieved);
        assertEquals("Updated Title", retrieved.getTitle());
        assertTrue(retrieved.isCompleted());
    }

    @Test
    @DisplayName("PUT /api/todos/{id} should return 404 for non-existent todo")
    void updateTodo_NonExistent_ReturnsNotFound() {
        Todo todo = new Todo("non-existent-id", "Title", "Description", false);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Todo> request = new HttpEntity<>(todo, headers);
        ResponseEntity<Todo> response = restTemplate.exchange(
                baseUrl + "/non-existent-id",
                HttpMethod.PUT,
                request,
                Todo.class
        );
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("DELETE /api/todos/{id} should delete existing todo")
    void deleteTodo_DeletesExistingTodo() {
        // Create a todo first
        Todo newTodo = new Todo("To Delete", "This will be deleted");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Todo> createRequest = new HttpEntity<>(newTodo, headers);
        ResponseEntity<Todo> createResponse = restTemplate.postForEntity(baseUrl, createRequest, Todo.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        Todo created = createResponse.getBody();
        assertNotNull(created);
        String todoId = created.getId();

        // Delete the todo
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                baseUrl + "/" + todoId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // Verify the todo is deleted
        ResponseEntity<Todo> getResponse = restTemplate.getForEntity(baseUrl + "/" + todoId, Todo.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    @DisplayName("DELETE /api/todos/{id} should return 404 for non-existent todo")
    void deleteTodo_NonExistent_ReturnsNotFound() {
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/non-existent-id",
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("Full CRUD workflow should work correctly")
    void fullCrudWorkflow_WorksCorrectly() {
        // Create multiple todos
        Todo todo1 = new Todo("Todo 1", "Description 1");
        Todo todo2 = new Todo("Todo 2", "Description 2");
        Todo todo3 = new Todo("Todo 3", "Description 3");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Todo> request1 = new HttpEntity<>(todo1, headers);
        HttpEntity<Todo> request2 = new HttpEntity<>(todo2, headers);
        HttpEntity<Todo> request3 = new HttpEntity<>(todo3, headers);

        ResponseEntity<Todo> response1 = restTemplate.postForEntity(baseUrl, request1, Todo.class);
        ResponseEntity<Todo> response2 = restTemplate.postForEntity(baseUrl, request2, Todo.class);
        ResponseEntity<Todo> response3 = restTemplate.postForEntity(baseUrl, request3, Todo.class);

        assertEquals(HttpStatus.CREATED, response1.getStatusCode());
        assertEquals(HttpStatus.CREATED, response2.getStatusCode());
        assertEquals(HttpStatus.CREATED, response3.getStatusCode());

        Todo created1 = response1.getBody();
        Todo created2 = response2.getBody();
        Todo created3 = response3.getBody();

        assertNotNull(created1);
        assertNotNull(created2);
        assertNotNull(created3);

        // Verify all todos exist
        ResponseEntity<Todo[]> allResponse = restTemplate.getForEntity(baseUrl, Todo[].class);
        assertEquals(HttpStatus.OK, allResponse.getStatusCode());
        List<Todo> allTodos = Arrays.asList(allResponse.getBody());
        assertNotNull(allTodos);
        assertEquals(3, allTodos.size());

        // Update one todo
        Todo updated = new Todo(created2.getId(), "Updated Todo 2", "Updated Description 2", true);
        HttpEntity<Todo> updateRequest = new HttpEntity<>(updated, headers);
        ResponseEntity<Todo> updateResponse = restTemplate.exchange(
                baseUrl + "/" + created2.getId(),
                HttpMethod.PUT,
                updateRequest,
                Todo.class
        );
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());

        // Delete one todo
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                baseUrl + "/" + created3.getId(),
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // Verify final state
        ResponseEntity<Todo[]> remainingResponse = restTemplate.getForEntity(baseUrl, Todo[].class);
        assertEquals(HttpStatus.OK, remainingResponse.getStatusCode());
        List<Todo> remainingTodos = Arrays.asList(remainingResponse.getBody());
        assertNotNull(remainingTodos);
        assertEquals(2, remainingTodos.size());
        assertTrue(remainingTodos.stream().anyMatch(t -> t.getId().equals(created1.getId())));
        assertTrue(remainingTodos.stream().anyMatch(t -> t.getId().equals(created2.getId())));
        assertTrue(remainingTodos.stream().noneMatch(t -> t.getId().equals(created3.getId())));

        // Verify updated todo
        ResponseEntity<Todo> getResponse = restTemplate.getForEntity(baseUrl + "/" + created2.getId(), Todo.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        Todo updatedTodo = getResponse.getBody();
        assertNotNull(updatedTodo);
        assertEquals("Updated Todo 2", updatedTodo.getTitle());
        assertTrue(updatedTodo.isCompleted());
    }

    @Test
    @DisplayName("POST /api/todos should handle todo with null ID by generating one")
    void createTodo_WithNullId_GeneratesId() {
        Todo todo = new Todo();
        todo.setTitle("Auto ID Todo");
        todo.setDescription("Should get auto-generated ID");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Todo> request = new HttpEntity<>(todo, headers);

        ResponseEntity<Todo> response = restTemplate.postForEntity(baseUrl, request, Todo.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Todo created = response.getBody();
        assertNotNull(created);
        assertNotNull(created.getId());
        assertFalse(created.getId().isEmpty());
        assertEquals("Auto ID Todo", created.getTitle());
    }
}

