package com.example.todo.api;

import com.example.todo.api.config.ApiTestConfig;
import com.example.todo.model.Todo;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Portable REST API tests that can run against local or deployed instances.
 * 
 * Configure the target environment via:
 * - application-api-test.properties
 * - Environment variables: API_BASE_URL
 * - System properties: -Dapi.base.url=...
 * 
 * Note: These tests assume the application is already running.
 * They are plain JUnit tests and do not load Spring Boot context.
 */
@DisplayName("Todo REST API Tests")
class TodoRestApiTest {

    private ApiTestConfig apiTestConfig;
    private String todosEndpoint;

    @BeforeEach
    void setUp() {
        apiTestConfig = new ApiTestConfig();
        todosEndpoint = apiTestConfig.getTodosEndpoint();
        RestAssured.baseURI = apiTestConfig.getApiBaseUrl();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("GET /api/todos should return 200 OK")
    void getAllTodos_Returns200() {
        given()
            .when()
                .get(todosEndpoint)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", isA(Object.class));
    }

    @Test
    @DisplayName("POST /api/todos should create a new todo")
    void createTodo_CreatesNewTodo() {
        Todo newTodo = new Todo("API Test Todo", "This is an API test");

        Response response = given()
            .contentType(ContentType.JSON)
            .body(newTodo)
            .when()
                .post(todosEndpoint)
            .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        Todo created = response.as(Todo.class);
        assertNotNull(created.getId(), "Created todo should have an ID");
        assertEquals("API Test Todo", created.getTitle());
        assertEquals("This is an API test", created.getDescription());
        assertFalse(created.isCompleted(), "New todo should not be completed");
    }

    @Test
    @DisplayName("GET /api/todos/{id} should return created todo")
    void getTodoById_ReturnsCreatedTodo() {
        // Create a todo first
        Todo newTodo = new Todo("Get Test Todo", "Test description");
        Response createResponse = given()
            .contentType(ContentType.JSON)
            .body(newTodo)
            .when()
                .post(todosEndpoint)
            .then()
                .statusCode(201)
                .extract()
                .response();

        Todo created = createResponse.as(Todo.class);
        String todoId = created.getId();

        // Get the todo by ID
        given()
            .when()
                .get(todosEndpoint + "/" + todoId)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(todoId))
                .body("title", equalTo("Get Test Todo"))
                .body("description", equalTo("Test description"))
                .body("completed", equalTo(false));
    }

    @Test
    @DisplayName("GET /api/todos/{id} should return 404 for non-existent todo")
    void getTodoById_NonExistent_Returns404() {
        given()
            .when()
                .get(todosEndpoint + "/non-existent-id")
            .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("PUT /api/todos/{id} should update existing todo")
    void updateTodo_UpdatesExistingTodo() {
        // Create a todo first
        Todo newTodo = new Todo("Original Title", "Original description");
        Response createResponse = given()
            .contentType(ContentType.JSON)
            .body(newTodo)
            .when()
                .post(todosEndpoint)
            .then()
                .statusCode(201)
                .extract()
                .response();

        Todo created = createResponse.as(Todo.class);
        String todoId = created.getId();

        // Update the todo
        Todo updatedTodo = new Todo(todoId, "Updated Title", "Updated description", true);

        Response updateResponse = given()
            .contentType(ContentType.JSON)
            .body(updatedTodo)
            .when()
                .put(todosEndpoint + "/" + todoId)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        Todo updated = updateResponse.as(Todo.class);
        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated description", updated.getDescription());
        assertTrue(updated.isCompleted(), "Todo should be marked as completed");
    }

    @Test
    @DisplayName("PUT /api/todos/{id} should return 404 for non-existent todo")
    void updateTodo_NonExistent_Returns404() {
        Todo todo = new Todo("non-existent-id", "Title", "Description", false);

        given()
            .contentType(ContentType.JSON)
            .body(todo)
            .when()
                .put(todosEndpoint + "/non-existent-id")
            .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("DELETE /api/todos/{id} should delete existing todo")
    void deleteTodo_DeletesExistingTodo() {
        // Create a todo first
        Todo newTodo = new Todo("Delete Test Todo", "To be deleted");
        Response createResponse = given()
            .contentType(ContentType.JSON)
            .body(newTodo)
            .when()
                .post(todosEndpoint)
            .then()
                .statusCode(201)
                .extract()
                .response();

        Todo created = createResponse.as(Todo.class);
        String todoId = created.getId();

        // Delete the todo
        given()
            .when()
                .delete(todosEndpoint + "/" + todoId)
            .then()
                .statusCode(204);

        // Verify it's deleted
        given()
            .when()
                .get(todosEndpoint + "/" + todoId)
            .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("DELETE /api/todos/{id} should return 404 for non-existent todo")
    void deleteTodo_NonExistent_Returns404() {
        given()
            .when()
                .delete(todosEndpoint + "/non-existent-id")
            .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Full CRUD workflow should work correctly")
    void fullCrudWorkflow_WorksCorrectly() {
        // Create
        Todo newTodo = new Todo("Workflow Test", "Testing full workflow");
        Response createResponse = given()
            .contentType(ContentType.JSON)
            .body(newTodo)
            .when()
                .post(todosEndpoint)
            .then()
                .statusCode(201)
                .extract()
                .response();

        Todo created = createResponse.as(Todo.class);
        String todoId = created.getId();
        assertNotNull(todoId);

        // Read
        given()
            .when()
                .get(todosEndpoint + "/" + todoId)
            .then()
                .statusCode(200)
                .body("id", equalTo(todoId))
                .body("title", equalTo("Workflow Test"));

        // Update
        Todo updatedTodo = new Todo(todoId, "Updated Workflow Test", "Updated description", true);
        given()
            .contentType(ContentType.JSON)
            .body(updatedTodo)
            .when()
                .put(todosEndpoint + "/" + todoId)
            .then()
                .statusCode(200)
                .body("completed", equalTo(true));

        // Delete
        given()
            .when()
                .delete(todosEndpoint + "/" + todoId)
            .then()
                .statusCode(204);

        // Verify deletion
        given()
            .when()
                .get(todosEndpoint + "/" + todoId)
            .then()
                .statusCode(404);
    }
}

