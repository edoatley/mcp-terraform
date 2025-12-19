package com.example.todo.controller;

import com.example.todo.model.Todo;
import com.example.todo.proto.*;
import com.example.todo.service.TodoService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TodoGrpcController Unit Tests")
class TodoGrpcControllerTest {

    @Mock
    private TodoService todoService;

    @Mock
    private StreamObserver<GetTodoResponse> getTodoResponseObserver;

    @Mock
    private StreamObserver<ListTodosResponse> listTodosResponseObserver;

    @Mock
    private StreamObserver<CreateTodoResponse> createTodoResponseObserver;

    @Mock
    private StreamObserver<UpdateTodoResponse> updateTodoResponseObserver;

    @Mock
    private StreamObserver<DeleteTodoResponse> deleteTodoResponseObserver;

    @InjectMocks
    private TodoGrpcController todoGrpcController;

    private Todo testTodo;

    @BeforeEach
    void setUp() {
        testTodo = new Todo("test-id", "Test Title", "Test Description", false);
    }

    @Test
    @DisplayName("getTodo should return todo when it exists")
    void getTodo_WhenExists_ReturnsTodo() {
        // Given
        GetTodoRequest request = GetTodoRequest.newBuilder()
                .setId("test-id")
                .build();
        when(todoService.getTodoById("test-id")).thenReturn(Optional.of(testTodo));

        // When
        todoGrpcController.getTodo(request, getTodoResponseObserver);

        // Then
        ArgumentCaptor<GetTodoResponse> responseCaptor = ArgumentCaptor.forClass(GetTodoResponse.class);
        verify(getTodoResponseObserver, times(1)).onNext(responseCaptor.capture());
        verify(getTodoResponseObserver, times(1)).onCompleted();
        verify(getTodoResponseObserver, never()).onError(any());

        GetTodoResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertEquals("test-id", response.getTodo().getId());
        assertEquals("Test Title", response.getTodo().getTitle());
        assertEquals("Test Description", response.getTodo().getDescription());
        assertFalse(response.getTodo().getCompleted());
    }

    @Test
    @DisplayName("getTodo should return NOT_FOUND error when todo does not exist")
    void getTodo_WhenNotExists_ReturnsNotFoundError() {
        // Given
        GetTodoRequest request = GetTodoRequest.newBuilder()
                .setId("non-existent")
                .build();
        when(todoService.getTodoById("non-existent")).thenReturn(Optional.empty());

        // When
        todoGrpcController.getTodo(request, getTodoResponseObserver);

        // Then
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(getTodoResponseObserver, never()).onNext(any());
        verify(getTodoResponseObserver, never()).onCompleted();
        verify(getTodoResponseObserver, times(1)).onError(errorCaptor.capture());

        Throwable error = errorCaptor.getValue();
        assertNotNull(error);
        assertTrue(error instanceof io.grpc.StatusRuntimeException);
        io.grpc.StatusRuntimeException statusException = (io.grpc.StatusRuntimeException) error;
        assertEquals(io.grpc.Status.NOT_FOUND.getCode(), statusException.getStatus().getCode());
        assertEquals("Todo not found", statusException.getStatus().getDescription());
    }

    @Test
    @DisplayName("listTodos should return all todos")
    void listTodos_ReturnsAllTodos() {
        // Given
        ListTodosRequest request = ListTodosRequest.newBuilder().build();
        List<Todo> todos = Arrays.asList(
                new Todo("id1", "Title 1", "Description 1", false),
                new Todo("id2", "Title 2", "Description 2", true)
        );
        when(todoService.getAllTodos()).thenReturn(todos);

        // When
        todoGrpcController.listTodos(request, listTodosResponseObserver);

        // Then
        ArgumentCaptor<ListTodosResponse> responseCaptor = ArgumentCaptor.forClass(ListTodosResponse.class);
        verify(listTodosResponseObserver, times(1)).onNext(responseCaptor.capture());
        verify(listTodosResponseObserver, times(1)).onCompleted();
        verify(listTodosResponseObserver, never()).onError(any());

        ListTodosResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertEquals(2, response.getTodosCount());
        assertEquals("id1", response.getTodos(0).getId());
        assertEquals("Title 1", response.getTodos(0).getTitle());
        assertEquals("id2", response.getTodos(1).getId());
        assertEquals("Title 2", response.getTodos(1).getTitle());
        assertTrue(response.getTodos(1).getCompleted());
    }

    @Test
    @DisplayName("listTodos should return empty list when no todos exist")
    void listTodos_WhenNoTodos_ReturnsEmptyList() {
        // Given
        ListTodosRequest request = ListTodosRequest.newBuilder().build();
        when(todoService.getAllTodos()).thenReturn(List.of());

        // When
        todoGrpcController.listTodos(request, listTodosResponseObserver);

        // Then
        ArgumentCaptor<ListTodosResponse> responseCaptor = ArgumentCaptor.forClass(ListTodosResponse.class);
        verify(listTodosResponseObserver, times(1)).onNext(responseCaptor.capture());
        verify(listTodosResponseObserver, times(1)).onCompleted();

        ListTodosResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertEquals(0, response.getTodosCount());
    }

    @Test
    @DisplayName("createTodo should create and return todo")
    void createTodo_CreatesAndReturnsTodo() {
        // Given
        CreateTodoRequest request = CreateTodoRequest.newBuilder()
                .setTitle("New Title")
                .setDescription("New Description")
                .build();
        Todo createdTodo = new Todo("new-id", "New Title", "New Description", false);
        when(todoService.createTodo(any(Todo.class))).thenReturn(createdTodo);

        // When
        todoGrpcController.createTodo(request, createTodoResponseObserver);

        // Then
        ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
        verify(todoService, times(1)).createTodo(todoCaptor.capture());
        
        Todo capturedTodo = todoCaptor.getValue();
        assertEquals("New Title", capturedTodo.getTitle());
        assertEquals("New Description", capturedTodo.getDescription());

        ArgumentCaptor<CreateTodoResponse> responseCaptor = ArgumentCaptor.forClass(CreateTodoResponse.class);
        verify(createTodoResponseObserver, times(1)).onNext(responseCaptor.capture());
        verify(createTodoResponseObserver, times(1)).onCompleted();
        verify(createTodoResponseObserver, never()).onError(any());

        CreateTodoResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertEquals("new-id", response.getTodo().getId());
        assertEquals("New Title", response.getTodo().getTitle());
        assertEquals("New Description", response.getTodo().getDescription());
    }

    @Test
    @DisplayName("updateTodo should update and return todo when it exists")
    void updateTodo_WhenExists_UpdatesAndReturnsTodo() {
        // Given
        UpdateTodoRequest request = UpdateTodoRequest.newBuilder()
                .setId("test-id")
                .setTitle("Updated Title")
                .setDescription("Updated Description")
                .setCompleted(true)
                .build();
        Todo updatedTodo = new Todo("test-id", "Updated Title", "Updated Description", true);
        when(todoService.updateTodo(eq("test-id"), any(Todo.class)))
                .thenReturn(Optional.of(updatedTodo));

        // When
        todoGrpcController.updateTodo(request, updateTodoResponseObserver);

        // Then
        ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
        verify(todoService, times(1)).updateTodo(eq("test-id"), todoCaptor.capture());

        Todo capturedTodo = todoCaptor.getValue();
        assertEquals("test-id", capturedTodo.getId());
        assertEquals("Updated Title", capturedTodo.getTitle());
        assertEquals("Updated Description", capturedTodo.getDescription());
        assertTrue(capturedTodo.isCompleted());

        ArgumentCaptor<UpdateTodoResponse> responseCaptor = ArgumentCaptor.forClass(UpdateTodoResponse.class);
        verify(updateTodoResponseObserver, times(1)).onNext(responseCaptor.capture());
        verify(updateTodoResponseObserver, times(1)).onCompleted();
        verify(updateTodoResponseObserver, never()).onError(any());

        UpdateTodoResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertEquals("test-id", response.getTodo().getId());
        assertEquals("Updated Title", response.getTodo().getTitle());
        assertTrue(response.getTodo().getCompleted());
    }

    @Test
    @DisplayName("updateTodo should return NOT_FOUND error when todo does not exist")
    void updateTodo_WhenNotExists_ReturnsNotFoundError() {
        // Given
        UpdateTodoRequest request = UpdateTodoRequest.newBuilder()
                .setId("non-existent")
                .setTitle("Updated Title")
                .setDescription("Updated Description")
                .setCompleted(true)
                .build();
        when(todoService.updateTodo(eq("non-existent"), any(Todo.class)))
                .thenReturn(Optional.empty());

        // When
        todoGrpcController.updateTodo(request, updateTodoResponseObserver);

        // Then
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(updateTodoResponseObserver, never()).onNext(any());
        verify(updateTodoResponseObserver, never()).onCompleted();
        verify(updateTodoResponseObserver, times(1)).onError(errorCaptor.capture());

        Throwable error = errorCaptor.getValue();
        assertNotNull(error);
        assertTrue(error instanceof io.grpc.StatusRuntimeException);
        io.grpc.StatusRuntimeException statusException = (io.grpc.StatusRuntimeException) error;
        assertEquals(io.grpc.Status.NOT_FOUND.getCode(), statusException.getStatus().getCode());
        assertEquals("Todo not found", statusException.getStatus().getDescription());
    }

    @Test
    @DisplayName("deleteTodo should return success true when todo exists")
    void deleteTodo_WhenExists_ReturnsSuccessTrue() {
        // Given
        DeleteTodoRequest request = DeleteTodoRequest.newBuilder()
                .setId("test-id")
                .build();
        when(todoService.deleteTodo("test-id")).thenReturn(true);

        // When
        todoGrpcController.deleteTodo(request, deleteTodoResponseObserver);

        // Then
        verify(todoService, times(1)).deleteTodo("test-id");

        ArgumentCaptor<DeleteTodoResponse> responseCaptor = ArgumentCaptor.forClass(DeleteTodoResponse.class);
        verify(deleteTodoResponseObserver, times(1)).onNext(responseCaptor.capture());
        verify(deleteTodoResponseObserver, times(1)).onCompleted();
        verify(deleteTodoResponseObserver, never()).onError(any());

        DeleteTodoResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertTrue(response.getSuccess());
    }

    @Test
    @DisplayName("deleteTodo should return success false when todo does not exist")
    void deleteTodo_WhenNotExists_ReturnsSuccessFalse() {
        // Given
        DeleteTodoRequest request = DeleteTodoRequest.newBuilder()
                .setId("non-existent")
                .build();
        when(todoService.deleteTodo("non-existent")).thenReturn(false);

        // When
        todoGrpcController.deleteTodo(request, deleteTodoResponseObserver);

        // Then
        verify(todoService, times(1)).deleteTodo("non-existent");

        ArgumentCaptor<DeleteTodoResponse> responseCaptor = ArgumentCaptor.forClass(DeleteTodoResponse.class);
        verify(deleteTodoResponseObserver, times(1)).onNext(responseCaptor.capture());
        verify(deleteTodoResponseObserver, times(1)).onCompleted();
        verify(deleteTodoResponseObserver, never()).onError(any());

        DeleteTodoResponse response = responseCaptor.getValue();
        assertNotNull(response);
        assertFalse(response.getSuccess());
    }

    @Test
    @DisplayName("convertToProto should correctly convert Todo to proto")
    void convertToProto_ConvertsCorrectly() {
        // Given
        Todo todo = new Todo("id", "Title", "Description", true);
        GetTodoRequest request = GetTodoRequest.newBuilder()
                .setId("id")
                .build();
        when(todoService.getTodoById("id")).thenReturn(Optional.of(todo));

        // When
        todoGrpcController.getTodo(request, getTodoResponseObserver);

        // Then
        ArgumentCaptor<GetTodoResponse> responseCaptor = ArgumentCaptor.forClass(GetTodoResponse.class);
        verify(getTodoResponseObserver).onNext(responseCaptor.capture());

        com.example.todo.proto.Todo protoTodo = responseCaptor.getValue().getTodo();
        assertEquals("id", protoTodo.getId());
        assertEquals("Title", protoTodo.getTitle());
        assertEquals("Description", protoTodo.getDescription());
        assertTrue(protoTodo.getCompleted());
    }
}

