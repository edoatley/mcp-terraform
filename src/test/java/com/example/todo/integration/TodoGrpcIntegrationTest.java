package com.example.todo.integration;

import com.example.todo.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Todo gRPC API Integration Tests")
class TodoGrpcIntegrationTest {

    private ManagedChannel channel;
    private TodoServiceGrpc.TodoServiceBlockingStub stub;

    @BeforeEach
    void setUp() {
        // Use fixed port 9090 as configured in application-test.properties
        channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();
        stub = TodoServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (channel != null) {
            channel.shutdown();
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("listTodos should return empty list initially")
    void listTodos_Initially_ReturnsEmptyList() {
        ListTodosRequest request = ListTodosRequest.newBuilder().build();

        ListTodosResponse response = stub.listTodos(request);

        assertNotNull(response);
        assertEquals(0, response.getTodosCount());
    }

    @Test
    @DisplayName("createTodo should create a new todo")
    void createTodo_CreatesNewTodo() {
        CreateTodoRequest request = CreateTodoRequest.newBuilder()
                .setTitle("gRPC Test Todo")
                .setDescription("This is a gRPC integration test")
                .build();

        CreateTodoResponse response = stub.createTodo(request);

        assertNotNull(response);
        assertNotNull(response.getTodo());
        assertNotNull(response.getTodo().getId());
        assertFalse(response.getTodo().getId().isEmpty());
        assertEquals("gRPC Test Todo", response.getTodo().getTitle());
        assertEquals("This is a gRPC integration test", response.getTodo().getDescription());
        assertFalse(response.getTodo().getCompleted());
    }

    @Test
    @DisplayName("getTodo should return created todo")
    void getTodo_ReturnsCreatedTodo() {
        // Create a todo first
        CreateTodoRequest createRequest = CreateTodoRequest.newBuilder()
                .setTitle("Get Test Todo")
                .setDescription("Description for get test")
                .build();
        CreateTodoResponse created = stub.createTodo(createRequest);
        String todoId = created.getTodo().getId();

        // Get the todo
        GetTodoRequest getRequest = GetTodoRequest.newBuilder()
                .setId(todoId)
                .build();
        GetTodoResponse response = stub.getTodo(getRequest);

        assertNotNull(response);
        assertNotNull(response.getTodo());
        assertEquals(todoId, response.getTodo().getId());
        assertEquals("Get Test Todo", response.getTodo().getTitle());
        assertEquals("Description for get test", response.getTodo().getDescription());
    }

    @Test
    @DisplayName("getTodo should throw NOT_FOUND for non-existent todo")
    void getTodo_NonExistent_ThrowsNotFound() {
        GetTodoRequest request = GetTodoRequest.newBuilder()
                .setId("non-existent-id")
                .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            stub.getTodo(request);
        });

        assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        assertEquals("Todo not found", exception.getStatus().getDescription());
    }

    @Test
    @DisplayName("updateTodo should update existing todo")
    void updateTodo_UpdatesExistingTodo() {
        // Create a todo first
        CreateTodoRequest createRequest = CreateTodoRequest.newBuilder()
                .setTitle("Original Title")
                .setDescription("Original Description")
                .build();
        CreateTodoResponse created = stub.createTodo(createRequest);
        String todoId = created.getTodo().getId();

        // Update the todo
        UpdateTodoRequest updateRequest = UpdateTodoRequest.newBuilder()
                .setId(todoId)
                .setTitle("Updated Title")
                .setDescription("Updated Description")
                .setCompleted(true)
                .build();
        UpdateTodoResponse updateResponse = stub.updateTodo(updateRequest);

        assertNotNull(updateResponse);
        assertNotNull(updateResponse.getTodo());
        assertEquals(todoId, updateResponse.getTodo().getId());
        assertEquals("Updated Title", updateResponse.getTodo().getTitle());
        assertEquals("Updated Description", updateResponse.getTodo().getDescription());
        assertTrue(updateResponse.getTodo().getCompleted());

        // Verify the update persisted
        GetTodoRequest getRequest = GetTodoRequest.newBuilder()
                .setId(todoId)
                .build();
        GetTodoResponse getResponse = stub.getTodo(getRequest);

        assertNotNull(getResponse);
        assertEquals("Updated Title", getResponse.getTodo().getTitle());
        assertTrue(getResponse.getTodo().getCompleted());
    }

    @Test
    @DisplayName("updateTodo should throw NOT_FOUND for non-existent todo")
    void updateTodo_NonExistent_ThrowsNotFound() {
        UpdateTodoRequest request = UpdateTodoRequest.newBuilder()
                .setId("non-existent-id")
                .setTitle("Title")
                .setDescription("Description")
                .setCompleted(false)
                .build();

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            stub.updateTodo(request);
        });

        assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
        assertEquals("Todo not found", exception.getStatus().getDescription());
    }

    @Test
    @DisplayName("deleteTodo should delete existing todo")
    void deleteTodo_DeletesExistingTodo() {
        // Create a todo first
        CreateTodoRequest createRequest = CreateTodoRequest.newBuilder()
                .setTitle("To Delete")
                .setDescription("This will be deleted")
                .build();
        CreateTodoResponse created = stub.createTodo(createRequest);
        String todoId = created.getTodo().getId();

        // Delete the todo
        DeleteTodoRequest deleteRequest = DeleteTodoRequest.newBuilder()
                .setId(todoId)
                .build();
        DeleteTodoResponse deleteResponse = stub.deleteTodo(deleteRequest);

        assertNotNull(deleteResponse);
        assertTrue(deleteResponse.getSuccess());

        // Verify the todo is deleted
        GetTodoRequest getRequest = GetTodoRequest.newBuilder()
                .setId(todoId)
                .build();
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            stub.getTodo(getRequest);
        });

        assertEquals(Status.NOT_FOUND.getCode(), exception.getStatus().getCode());
    }

    @Test
    @DisplayName("deleteTodo should return success false for non-existent todo")
    void deleteTodo_NonExistent_ReturnsSuccessFalse() {
        DeleteTodoRequest request = DeleteTodoRequest.newBuilder()
                .setId("non-existent-id")
                .build();
        DeleteTodoResponse response = stub.deleteTodo(request);

        assertNotNull(response);
        assertFalse(response.getSuccess());
    }

    @Test
    @DisplayName("listTodos should return all created todos")
    void listTodos_ReturnsAllCreatedTodos() {
        // Create multiple todos
        CreateTodoRequest request1 = CreateTodoRequest.newBuilder()
                .setTitle("Todo 1")
                .setDescription("Description 1")
                .build();
        CreateTodoRequest request2 = CreateTodoRequest.newBuilder()
                .setTitle("Todo 2")
                .setDescription("Description 2")
                .build();
        CreateTodoRequest request3 = CreateTodoRequest.newBuilder()
                .setTitle("Todo 3")
                .setDescription("Description 3")
                .build();

        stub.createTodo(request1);
        stub.createTodo(request2);
        stub.createTodo(request3);

        // List all todos
        ListTodosRequest listRequest = ListTodosRequest.newBuilder().build();
        ListTodosResponse response = stub.listTodos(listRequest);

        assertNotNull(response);
        assertTrue(response.getTodosCount() >= 3);
        assertTrue(response.getTodosList().stream()
                .anyMatch(t -> t.getTitle().equals("Todo 1")));
        assertTrue(response.getTodosList().stream()
                .anyMatch(t -> t.getTitle().equals("Todo 2")));
        assertTrue(response.getTodosList().stream()
                .anyMatch(t -> t.getTitle().equals("Todo 3")));
    }

    @Test
    @DisplayName("Full CRUD workflow should work correctly")
    void fullCrudWorkflow_WorksCorrectly() {
        // Create todos
        CreateTodoRequest create1 = CreateTodoRequest.newBuilder()
                .setTitle("Workflow Todo 1")
                .setDescription("Description 1")
                .build();
        CreateTodoRequest create2 = CreateTodoRequest.newBuilder()
                .setTitle("Workflow Todo 2")
                .setDescription("Description 2")
                .build();
        CreateTodoRequest create3 = CreateTodoRequest.newBuilder()
                .setTitle("Workflow Todo 3")
                .setDescription("Description 3")
                .build();

        CreateTodoResponse created1 = stub.createTodo(create1);
        CreateTodoResponse created2 = stub.createTodo(create2);
        CreateTodoResponse created3 = stub.createTodo(create3);

        assertNotNull(created1);
        assertNotNull(created2);
        assertNotNull(created3);

        // Verify all todos exist
        ListTodosResponse listResponse = stub.listTodos(ListTodosRequest.newBuilder().build());
        assertTrue(listResponse.getTodosCount() >= 3);

        // Update one todo
        UpdateTodoRequest updateRequest = UpdateTodoRequest.newBuilder()
                .setId(created2.getTodo().getId())
                .setTitle("Updated Workflow Todo 2")
                .setDescription("Updated Description 2")
                .setCompleted(true)
                .build();
        UpdateTodoResponse updateResponse = stub.updateTodo(updateRequest);
        assertEquals("Updated Workflow Todo 2", updateResponse.getTodo().getTitle());
        assertTrue(updateResponse.getTodo().getCompleted());

        // Delete one todo
        DeleteTodoRequest deleteRequest = DeleteTodoRequest.newBuilder()
                .setId(created3.getTodo().getId())
                .build();
        DeleteTodoResponse deleteResponse = stub.deleteTodo(deleteRequest);
        assertTrue(deleteResponse.getSuccess());

        // Verify deleted todo is gone
        GetTodoRequest getRequest = GetTodoRequest.newBuilder()
                .setId(created3.getTodo().getId())
                .build();
        assertThrows(StatusRuntimeException.class, () -> stub.getTodo(getRequest));

        // Verify remaining todos
        GetTodoResponse get1 = stub.getTodo(GetTodoRequest.newBuilder()
                .setId(created1.getTodo().getId())
                .build());
        GetTodoResponse get2 = stub.getTodo(GetTodoRequest.newBuilder()
                .setId(created2.getTodo().getId())
                .build());

        assertNotNull(get1);
        assertNotNull(get2);
        assertEquals("Workflow Todo 1", get1.getTodo().getTitle());
        assertEquals("Updated Workflow Todo 2", get2.getTodo().getTitle());
        assertTrue(get2.getTodo().getCompleted());
    }
}

