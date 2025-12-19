package com.example.todo.api;

import com.example.todo.api.config.ApiTestConfig;
import com.example.todo.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Portable gRPC API tests that can run against local or deployed instances.
 * 
 * Configure the target environment via:
 * - application-api-test.properties
 * - Environment variables: GRPC_HOST, GRPC_PORT
 * - System properties: -Dgrpc.host=..., -Dgrpc.port=...
 * 
 * Note: These tests assume the application is already running.
 * They are plain JUnit tests and do not load Spring Boot context.
 */
@DisplayName("Todo gRPC API Tests")
class TodoGrpcApiTest {

    private ApiTestConfig apiTestConfig;
    private ManagedChannel channel;
    private TodoServiceGrpc.TodoServiceBlockingStub stub;

    @BeforeEach
    void setUp() {
        apiTestConfig = new ApiTestConfig();
        channel = ManagedChannelBuilder.forAddress(
                apiTestConfig.getGrpcHost(),
                apiTestConfig.getGrpcPort()
        ).usePlaintext().build();
        stub = TodoServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    @Test
    @DisplayName("ListTodos should return a list (may not be empty due to shared state)")
    void listTodos_ReturnsList() {
        ListTodosRequest request = ListTodosRequest.newBuilder().build();
        ListTodosResponse response = stub.listTodos(request);

        assertThat(response).isNotNull();
        assertThat(response.getTodosList()).isNotNull();
        // Note: List may not be empty if other tests have created todos
    }

    @Test
    @DisplayName("CreateTodo should create a new todo")
    void createTodo_CreatesNewTodo() {
        CreateTodoRequest request = CreateTodoRequest.newBuilder()
                .setTitle("gRPC API Test Todo")
                .setDescription("This is a gRPC API test")
                .build();

        CreateTodoResponse response = stub.createTodo(request);

        assertThat(response).isNotNull();
        assertThat(response.hasTodo()).isTrue();
        Todo created = response.getTodo();
        assertThat(created.getId()).isNotEmpty();
        assertThat(created.getTitle()).isEqualTo("gRPC API Test Todo");
        assertThat(created.getDescription()).isEqualTo("This is a gRPC API test");
        assertThat(created.getCompleted()).isFalse();
    }

    @Test
    @DisplayName("GetTodo should return created todo")
    void getTodo_ReturnsCreatedTodo() {
        // Create a todo first
        CreateTodoRequest createRequest = CreateTodoRequest.newBuilder()
                .setTitle("Get Test Todo")
                .setDescription("Test description")
                .build();
        CreateTodoResponse createResponse = stub.createTodo(createRequest);
        String todoId = createResponse.getTodo().getId();

        // Get the todo
        GetTodoRequest getRequest = GetTodoRequest.newBuilder()
                .setId(todoId)
                .build();
        GetTodoResponse getResponse = stub.getTodo(getRequest);

        assertThat(getResponse).isNotNull();
        assertThat(getResponse.hasTodo()).isTrue();
        Todo todo = getResponse.getTodo();
        assertThat(todo.getId()).isEqualTo(todoId);
        assertThat(todo.getTitle()).isEqualTo("Get Test Todo");
        assertThat(todo.getDescription()).isEqualTo("Test description");
    }

    @Test
    @DisplayName("GetTodo should return NOT_FOUND for non-existent todo")
    void getTodo_NonExistent_ReturnsNotFound() {
        GetTodoRequest request = GetTodoRequest.newBuilder()
                .setId("non-existent-id")
                .build();

        assertThatThrownBy(() -> stub.getTodo(request))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(exception -> {
                    StatusRuntimeException sre = (StatusRuntimeException) exception;
                    assertThat(sre.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("UpdateTodo should update existing todo")
    void updateTodo_UpdatesExistingTodo() {
        // Create a todo first
        CreateTodoRequest createRequest = CreateTodoRequest.newBuilder()
                .setTitle("Original Title")
                .setDescription("Original description")
                .build();
        CreateTodoResponse createResponse = stub.createTodo(createRequest);
        String todoId = createResponse.getTodo().getId();

        // Update the todo
        UpdateTodoRequest updateRequest = UpdateTodoRequest.newBuilder()
                .setId(todoId)
                .setTitle("Updated Title")
                .setDescription("Updated description")
                .setCompleted(true)
                .build();
        UpdateTodoResponse updateResponse = stub.updateTodo(updateRequest);

        assertThat(updateResponse).isNotNull();
        assertThat(updateResponse.hasTodo()).isTrue();
        Todo updated = updateResponse.getTodo();
        assertThat(updated.getId()).isEqualTo(todoId);
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getCompleted()).isTrue();
    }

    @Test
    @DisplayName("UpdateTodo should return NOT_FOUND for non-existent todo")
    void updateTodo_NonExistent_ReturnsNotFound() {
        UpdateTodoRequest request = UpdateTodoRequest.newBuilder()
                .setId("non-existent-id")
                .setTitle("Title")
                .setDescription("Description")
                .build();

        assertThatThrownBy(() -> stub.updateTodo(request))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(exception -> {
                    StatusRuntimeException sre = (StatusRuntimeException) exception;
                    assertThat(sre.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("DeleteTodo should delete existing todo")
    void deleteTodo_DeletesExistingTodo() {
        // Create a todo first
        CreateTodoRequest createRequest = CreateTodoRequest.newBuilder()
                .setTitle("Delete Test Todo")
                .setDescription("To be deleted")
                .build();
        CreateTodoResponse createResponse = stub.createTodo(createRequest);
        String todoId = createResponse.getTodo().getId();

        // Delete the todo
        DeleteTodoRequest deleteRequest = DeleteTodoRequest.newBuilder()
                .setId(todoId)
                .build();
        DeleteTodoResponse deleteResponse = stub.deleteTodo(deleteRequest);

        assertThat(deleteResponse).isNotNull();
        assertThat(deleteResponse.getSuccess()).isTrue();

        // Verify it's deleted
        GetTodoRequest getRequest = GetTodoRequest.newBuilder()
                .setId(todoId)
                .build();
        assertThatThrownBy(() -> stub.getTodo(getRequest))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(exception -> {
                    StatusRuntimeException sre = (StatusRuntimeException) exception;
                    assertThat(sre.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("DeleteTodo should return success=false for non-existent todo")
    void deleteTodo_NonExistent_ReturnsFalse() {
        DeleteTodoRequest request = DeleteTodoRequest.newBuilder()
                .setId("non-existent-id")
                .build();

        DeleteTodoResponse response = stub.deleteTodo(request);

        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isFalse();
    }

    @Test
    @DisplayName("Full CRUD workflow should work correctly")
    void fullCrudWorkflow_WorksCorrectly() {
        // Create
        CreateTodoRequest createRequest = CreateTodoRequest.newBuilder()
                .setTitle("Workflow Test")
                .setDescription("Testing full workflow")
                .build();
        CreateTodoResponse createResponse = stub.createTodo(createRequest);
        String todoId = createResponse.getTodo().getId();
        assertThat(todoId).isNotEmpty();

        // Read
        GetTodoRequest getRequest = GetTodoRequest.newBuilder()
                .setId(todoId)
                .build();
        GetTodoResponse getResponse = stub.getTodo(getRequest);
        assertThat(getResponse.getTodo().getTitle()).isEqualTo("Workflow Test");

        // Update
        UpdateTodoRequest updateRequest = UpdateTodoRequest.newBuilder()
                .setId(todoId)
                .setTitle("Updated Workflow Test")
                .setDescription("Updated description")
                .setCompleted(true)
                .build();
        UpdateTodoResponse updateResponse = stub.updateTodo(updateRequest);
        assertThat(updateResponse.getTodo().getCompleted()).isTrue();

        // Delete
        DeleteTodoRequest deleteRequest = DeleteTodoRequest.newBuilder()
                .setId(todoId)
                .build();
        DeleteTodoResponse deleteResponse = stub.deleteTodo(deleteRequest);
        assertThat(deleteResponse.getSuccess()).isTrue();

        // Verify deletion
        assertThatThrownBy(() -> stub.getTodo(getRequest))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(exception -> {
                    StatusRuntimeException sre = (StatusRuntimeException) exception;
                    assertThat(sre.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("ListTodos should return all created todos")
    void listTodos_ReturnsAllCreatedTodos() {
        // Create multiple todos
        CreateTodoRequest create1 = CreateTodoRequest.newBuilder()
                .setTitle("List Test 1")
                .setDescription("First todo")
                .build();
        CreateTodoRequest create2 = CreateTodoRequest.newBuilder()
                .setTitle("List Test 2")
                .setDescription("Second todo")
                .build();

        stub.createTodo(create1);
        stub.createTodo(create2);

        // List all todos
        ListTodosRequest listRequest = ListTodosRequest.newBuilder().build();
        ListTodosResponse listResponse = stub.listTodos(listRequest);

        assertThat(listResponse).isNotNull();
        List<Todo> todos = listResponse.getTodosList();
        assertThat(todos.size()).isGreaterThanOrEqualTo(2);
        
        // Verify our created todos are in the list
        assertThat(todos).extracting(Todo::getTitle)
                .contains("List Test 1", "List Test 2");
    }
}

