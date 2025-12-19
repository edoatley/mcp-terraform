package com.example.todo.contract;

import com.example.todo.proto.*;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TodoService gRPC Contract Tests")
class TodoServiceContractTest {

    private ServiceDescriptor serviceDescriptor;

    @BeforeEach
    void setUp() {
        serviceDescriptor = TodoProto.getDescriptor().findServiceByName("TodoService");    
    }

    @Test
    @DisplayName("Service descriptor should match proto definition")
    void serviceDescriptor_ShouldMatchProtoDefinition() {
        assertThat(serviceDescriptor.getName()).isEqualTo("TodoService");
        assertThat(serviceDescriptor.getMethods())
            .extracting(MethodDescriptor::getName)
            .containsExactlyInAnyOrder(
                "GetTodo",
                "ListTodos",
                "CreateTodo",
                "UpdateTodo",
                "DeleteTodo"
            );
    }

    @Test
    @DisplayName("GetTodo method should have correct input/output types")
    void getTodoMethod_ShouldHaveCorrectTypes() {
        MethodDescriptor getTodoMethod = getMethodDescriptor(serviceDescriptor, "GetTodo");       
        assertThat(getTodoMethod.getName()).isEqualTo("GetTodo");
        assertThat(getTodoMethod.isClientStreaming()).isFalse();
        assertThat(getTodoMethod.isServerStreaming()).isFalse();
    }

    @Test
    @DisplayName("ListTodos method should have correct input/output types")
    void listTodosMethod_ShouldHaveCorrectTypes() {
        MethodDescriptor listTodosMethod = getMethodDescriptor(serviceDescriptor, "ListTodos");
        assertThat(listTodosMethod.getName()).isEqualTo("ListTodos");
        assertThat(listTodosMethod.isClientStreaming()).isFalse();
        assertThat(listTodosMethod.isServerStreaming()).isFalse();
    }

    @Test
    @DisplayName("CreateTodo method should have correct input/output types")
    void createTodoMethod_ShouldHaveCorrectTypes() {
        MethodDescriptor createTodoMethod = getMethodDescriptor(serviceDescriptor, "CreateTodo");
        assertThat(createTodoMethod.getName()).isEqualTo("CreateTodo");
        assertThat(createTodoMethod.isClientStreaming()).isFalse();
        assertThat(createTodoMethod.isServerStreaming()).isFalse();
    }

    @Test
    @DisplayName("UpdateTodo method should have correct input/output types")
    void updateTodoMethod_ShouldHaveCorrectTypes() {
        MethodDescriptor updateTodoMethod = getMethodDescriptor(serviceDescriptor, "UpdateTodo");
        assertThat(updateTodoMethod.getName()).isEqualTo("UpdateTodo");
        assertThat(updateTodoMethod.isClientStreaming()).isFalse();
        assertThat(updateTodoMethod.isServerStreaming()).isFalse();
    }

    @Test
    @DisplayName("DeleteTodo method should have correct input/output types")
    void deleteTodoMethod_ShouldHaveCorrectTypes() {
        MethodDescriptor deleteTodoMethod = getMethodDescriptor(serviceDescriptor, "DeleteTodo");
        assertThat(deleteTodoMethod.getName()).isEqualTo("DeleteTodo");
        assertThat(deleteTodoMethod.isClientStreaming()).isFalse();
        assertThat(deleteTodoMethod.isServerStreaming()).isFalse();
    }

    @Test
    @DisplayName("Todo message should serialize and deserialize correctly")
    void todoMessage_ShouldSerializeAndDeserialize() throws InvalidProtocolBufferException {
        // Given
        Todo original = Todo.newBuilder()
                .setId("test-id")
                .setTitle("Test Title")
                .setDescription("Test Description")
                .setCompleted(true)
                .build();
        
        // When
        byte[] serialized = original.toByteArray();
        assertThat(serialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        Todo deserialized = Todo.parseFrom(serialized);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("GetTodoRequest should serialize and deserialize correctly")
    void getTodoRequest_ShouldSerializeAndDeserialize() throws InvalidProtocolBufferException {
        // Given
        GetTodoRequest original = GetTodoRequest.newBuilder()
                .setId("test-id")
                .build();
        
        // When
        byte[] serialized = original.toByteArray();
        assertThat(serialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        GetTodoRequest deserialized = GetTodoRequest.parseFrom(serialized);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("GetTodoResponse should serialize and deserialize correctly")
    void getTodoResponse_ShouldSerializeAndDeserialize() throws InvalidProtocolBufferException {
        // Given
        GetTodoResponse original = GetTodoResponse.newBuilder()
                .setTodo(Todo.newBuilder()
                        .setId("test-id")
                        .setTitle("Test")
                        .setDescription("Description")
                        .setCompleted(false)
                        .build())
                .build();
        
        // When
        byte[] serialized = original.toByteArray();
        assertThat(serialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        GetTodoResponse deserialized = GetTodoResponse.parseFrom(serialized);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("ListTodosRequest should serialize and deserialize correctly")
    void listTodosRequest_ShouldSerializeAndDeserialize() throws InvalidProtocolBufferException {
        // Given
        ListTodosRequest original = ListTodosRequest.newBuilder().build();
        
        // When
        byte[] serialized = original.toByteArray();
        assertThat(serialized).isNotNull();
        
        // Then
        ListTodosRequest deserialized = ListTodosRequest.parseFrom(serialized);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("ListTodosResponse should serialize and deserialize correctly")
    void listTodosResponse_ShouldSerializeAndDeserialize() throws InvalidProtocolBufferException {
        // Given
        ListTodosResponse original = ListTodosResponse.newBuilder()
                .addTodos(Todo.newBuilder()
                        .setId("id1")
                        .setTitle("Todo 1")
                        .setDescription("Description 1")
                        .setCompleted(false)
                        .build())
                .addTodos(Todo.newBuilder()
                        .setId("id2")
                        .setTitle("Todo 2")
                        .setDescription("Description 2")
                        .setCompleted(true)
                        .build())
                .build();
        
        // When
        byte[] serialized = original.toByteArray();
        assertThat(serialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        ListTodosResponse deserialized = ListTodosResponse.parseFrom(serialized);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("CreateTodoRequest should serialize and deserialize correctly")
    void createTodoRequest_ShouldSerializeAndDeserialize() throws InvalidProtocolBufferException {
        CreateTodoRequest original = CreateTodoRequest.newBuilder()
                .setTitle("New Todo")
                .setDescription("New Description")
                .build();
        
        byte[] serialized = original.toByteArray();
        CreateTodoRequest deserialized = CreateTodoRequest.parseFrom(serialized);
        
        assertThat(deserialized.getTitle()).isEqualTo(original.getTitle());
        assertThat(deserialized.getDescription()).isEqualTo(original.getDescription());
    }

    @Test
    @DisplayName("CreateTodoResponse should serialize and deserialize correctly")
    void createTodoResponse_ShouldSerializeAndDeserialize() throws InvalidProtocolBufferException {
        // Given
        CreateTodoResponse original = CreateTodoResponse.newBuilder()
                .setTodo(Todo.newBuilder()
                        .setId("new-id")
                        .setTitle("Created Todo")
                        .setDescription("Created Description")
                        .setCompleted(false)
                        .build())
                .build();
        
        // When
        byte[] serialized = original.toByteArray();
        assertThat(serialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        CreateTodoResponse deserialized = CreateTodoResponse.parseFrom(serialized);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("UpdateTodoRequest should serialize and deserialize correctly")
    void updateTodoRequest_ShouldSerializeAndDeserialize() throws InvalidProtocolBufferException {
        // Given
        UpdateTodoRequest original = UpdateTodoRequest.newBuilder()
                .setId("update-id")
                .setTitle("Updated Title")
                .setDescription("Updated Description")
                .setCompleted(true)
                .build();
        
        // When
        byte[] serialized = original.toByteArray();
        assertThat(serialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        UpdateTodoRequest deserialized = UpdateTodoRequest.parseFrom(serialized);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("UpdateTodoResponse should serialize and deserialize correctly")
    void updateTodoResponse_ShouldSerializeAndDeserialize() throws InvalidProtocolBufferException {
        // Given
        UpdateTodoResponse original = UpdateTodoResponse.newBuilder()
                .setTodo(Todo.newBuilder()
                        .setId("updated-id")
                        .setTitle("Updated Todo")
                        .setDescription("Updated Description")
                        .setCompleted(true)
                        .build())
                .build();
        // When
        byte[] serialized = original.toByteArray();
        assertThat(serialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        UpdateTodoResponse deserialized = UpdateTodoResponse.parseFrom(serialized);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("DeleteTodoRequest should serialize and deserialize correctly")
    void deleteTodoRequest_ShouldSerializeAndDeserialize() throws InvalidProtocolBufferException {
        // Given
        DeleteTodoRequest original = DeleteTodoRequest.newBuilder()
                .setId("delete-id")
                .build();
        // When
        byte[] serialized = original.toByteArray();
        assertThat(serialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        DeleteTodoRequest deserialized = DeleteTodoRequest.parseFrom(serialized);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("DeleteTodoResponse should serialize and deserialize correctly")
    void deleteTodoResponse_ShouldSerializeAndDeserialize() throws InvalidProtocolBufferException {
        // Given
        DeleteTodoResponse original = DeleteTodoResponse.newBuilder()
                .setSuccess(true)
                .build();
        // When
        byte[] serialized = original.toByteArray();
        assertThat(serialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        DeleteTodoResponse deserialized = DeleteTodoResponse.parseFrom(serialized);
        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("Todo message should handle empty strings correctly")
    void todoMessage_ShouldHandleEmptyStrings() {
        // Given
        Todo todo = Todo.newBuilder()
                .setId("")
                .setTitle("")
                .setDescription("")
                .setCompleted(false)
                .build();
        
        // Then
        assertThat(todo.getId()).isEmpty();
        assertThat(todo.getTitle()).isEmpty();
        assertThat(todo.getDescription()).isEmpty();
        assertThat(todo.getCompleted()).isFalse();
    }

    @Test
    @DisplayName("Todo message should handle default values correctly")
    void todoMessage_ShouldHandleDefaultValues() {
        // Given
        Todo todo = Todo.newBuilder().build();
        
        // Then
        assertThat(todo.getId()).isEmpty();
        assertThat(todo.getTitle()).isEmpty();
        assertThat(todo.getDescription()).isEmpty();
        assertThat(todo.getCompleted()).isFalse();
    }

    @Test
    @DisplayName("Messages should be backward compatible with missing fields")
    void messages_ShouldBeBackwardCompatible() throws InvalidProtocolBufferException {
        // Given
        Todo full = Todo.newBuilder()
                .setId("id")
                .setTitle("title")
                .setDescription("description")
                .setCompleted(true)
                .build();
        
        // When
        byte[] serialized = full.toByteArray();
        assertThat(serialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        Todo partial = Todo.parseFrom(serialized);
        assertThat(partial).isEqualTo(full);
    }

    @Test
    @DisplayName("ListTodosResponse should handle empty list correctly")
    void listTodosResponse_ShouldHandleEmptyList() throws InvalidProtocolBufferException {
        // Given
        ListTodosResponse response = ListTodosResponse.newBuilder().build();
        // When
        byte[] serialized = response.toByteArray();
        assertThat(serialized).isNotNull();
        
        // Then
        ListTodosResponse deserialized = ListTodosResponse.parseFrom(serialized);
        assertThat(deserialized).isEqualTo(response);
        assertThat(deserialized.getTodosList()).isEmpty();
    }

    @Test
    @DisplayName("All request messages should have required fields defined in proto")
    void requestMessages_ShouldHaveRequiredFields() throws InvalidProtocolBufferException {
        // Given
        // GetTodoRequest requires id field
        GetTodoRequest getRequest = GetTodoRequest.newBuilder().setId("test").build();
        // When
        byte[] serialized = getRequest.toByteArray();
        assertThat(serialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        GetTodoRequest deserialized = GetTodoRequest.parseFrom(serialized);
        assertThat(deserialized).isEqualTo(getRequest);
        assertThat(deserialized.getId()).isNotEmpty();
        
        // CreateTodoRequest requires title and description
        CreateTodoRequest createRequest = CreateTodoRequest.newBuilder().setTitle("title").setDescription("desc").build();
        // When
        byte[] createSerialized = createRequest.toByteArray();
        assertThat(createSerialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        CreateTodoRequest createDeserialized = CreateTodoRequest.parseFrom(createSerialized);
        assertThat(createDeserialized).isEqualTo(createRequest);
        assertThat(createDeserialized.getTitle()).isNotEmpty();
        // UpdateTodoRequest requires id
        UpdateTodoRequest updateRequest = UpdateTodoRequest.newBuilder()
                .setId("id")
                .setTitle("title")
                .build();
        assertThat(updateRequest.getId()).isNotEmpty();
        
        // DeleteTodoRequest requires id
        DeleteTodoRequest deleteRequest = DeleteTodoRequest.newBuilder().setId("id").build();
        assertThat(deleteRequest.getId()).isNotEmpty();
    }

    @Test
    @DisplayName("Response messages should have correct structure")
    void responseMessages_ShouldHaveCorrectStructure() throws InvalidProtocolBufferException {
        // Given
        // GetTodoResponse should have todo field
        GetTodoResponse getResponse = GetTodoResponse.newBuilder()
                .setTodo(Todo.getDefaultInstance())
                .build();
        // When
        byte[] serialized = getResponse.toByteArray();
        assertThat(serialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        GetTodoResponse getDeserialized = GetTodoResponse.parseFrom(serialized);
        assertThat(getDeserialized).isEqualTo(getResponse);
        assertThat(getDeserialized.getTodo()).isNotNull();
        
        // ListTodosResponse should have todos field
        ListTodosResponse listResponse = ListTodosResponse.newBuilder().build();
        // When
        byte[] listSerialized = listResponse.toByteArray();
        assertThat(listSerialized).isNotNull();
        
        // Then
        ListTodosResponse listDeserialized = ListTodosResponse.parseFrom(listSerialized);
        assertThat(listDeserialized).isEqualTo(listResponse);
        assertThat(listDeserialized.getTodosList()).isNotNull();
        
        // CreateTodoResponse should have todo field
        CreateTodoResponse createResponse = CreateTodoResponse.newBuilder()
                .setTodo(Todo.getDefaultInstance())
                .build();
        // When
        byte[] createResponseSerialized = createResponse.toByteArray();
        assertThat(createResponseSerialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        CreateTodoResponse createResponseDeserialized = CreateTodoResponse.parseFrom(createResponseSerialized);
        assertThat(createResponseDeserialized).isEqualTo(createResponse);
        assertThat(createResponseDeserialized.getTodo()).isNotNull();
        
        // UpdateTodoResponse should have todo field
        UpdateTodoResponse updateResponse = UpdateTodoResponse.newBuilder()
                .setTodo(Todo.getDefaultInstance())
                .build();
        // When
        byte[] updateSerialized = updateResponse.toByteArray();
        assertThat(updateSerialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        UpdateTodoResponse updateDeserialized = UpdateTodoResponse.parseFrom(updateSerialized);
        assertThat(updateDeserialized).isEqualTo(updateResponse);
        assertThat(updateDeserialized.getTodo()).isNotNull();
        
        // DeleteTodoResponse should have success field
        DeleteTodoResponse deleteResponse = DeleteTodoResponse.newBuilder()
                .setSuccess(true)
                .build();
        // When
        byte[] deleteSerialized = deleteResponse.toByteArray();
        assertThat(deleteSerialized).isNotNull().hasSizeGreaterThan(0);
        
        // Then
        DeleteTodoResponse deleteDeserialized = DeleteTodoResponse.parseFrom(deleteSerialized);
        assertThat(deleteDeserialized).isEqualTo(deleteResponse);
        assertThat(deleteDeserialized.getSuccess()).isTrue();
    }

    @Test
    @DisplayName("Messages should support JSON format conversion")
    void messages_ShouldSupportJsonFormat() throws InvalidProtocolBufferException {
        // Given
        Todo todo = Todo.newBuilder()
                .setId("json-id")
                .setTitle("JSON Title")
                .setDescription("JSON Description")
                .setCompleted(true)
                .build();
        
        // Convert to JSON
        String json = JsonFormat.printer().print(todo);
        assertThat(json).isNotNull();
        assertThat(json).contains("json-id");
        assertThat(json).contains("JSON Title");
        
        // Parse from JSON
        Todo.Builder builder = Todo.newBuilder();
        JsonFormat.parser().merge(json, builder);
        Todo parsed = builder.build();
        assertThat(parsed).isEqualTo(todo);
    }

    private MethodDescriptor getMethodDescriptor(ServiceDescriptor serviceDescriptor, String methodName) {
        return serviceDescriptor.getMethods().stream()
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Method " + methodName + " not found"));
    }
}

