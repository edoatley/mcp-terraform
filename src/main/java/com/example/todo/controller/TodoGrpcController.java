package com.example.todo.controller;

import com.example.todo.model.Todo;
import com.example.todo.proto.*;
import com.example.todo.service.TodoService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class TodoGrpcController extends TodoServiceGrpc.TodoServiceImplBase {
    
    private final TodoService todoService;
    
    public TodoGrpcController(TodoService todoService) {
        this.todoService = todoService;
    }
    
    @Override
    public void getTodo(GetTodoRequest request, StreamObserver<GetTodoResponse> responseObserver) {
        todoService.getTodoById(request.getId())
                .ifPresentOrElse(
                        todo -> {
                            GetTodoResponse response = GetTodoResponse.newBuilder()
                                    .setTodo(convertToProto(todo))
                                    .build();
                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        },
                        () -> {
                            responseObserver.onError(
                                    io.grpc.Status.NOT_FOUND
                                            .withDescription("Todo not found")
                                            .asRuntimeException()
                            );
                        }
                );
    }
    
    @Override
    public void listTodos(ListTodosRequest request, StreamObserver<ListTodosResponse> responseObserver) {
        ListTodosResponse response = ListTodosResponse.newBuilder()
                .addAllTodos(todoService.getAllTodos().stream()
                        .map(this::convertToProto)
                        .toList())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    @Override
    public void createTodo(CreateTodoRequest request, StreamObserver<CreateTodoResponse> responseObserver) {
        Todo todo = new Todo(request.getTitle(), request.getDescription());
        Todo created = todoService.createTodo(todo);
        
        CreateTodoResponse response = CreateTodoResponse.newBuilder()
                .setTodo(convertToProto(created))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    @Override
    public void updateTodo(UpdateTodoRequest request, StreamObserver<UpdateTodoResponse> responseObserver) {
        Todo todo = new Todo(request.getId(), request.getTitle(), request.getDescription(), request.getCompleted());
        todoService.updateTodo(request.getId(), todo)
                .ifPresentOrElse(
                        updated -> {
                            UpdateTodoResponse response = UpdateTodoResponse.newBuilder()
                                    .setTodo(convertToProto(updated))
                                    .build();
                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        },
                        () -> {
                            responseObserver.onError(
                                    io.grpc.Status.NOT_FOUND
                                            .withDescription("Todo not found")
                                            .asRuntimeException()
                            );
                        }
                );
    }
    
    @Override
    public void deleteTodo(DeleteTodoRequest request, StreamObserver<DeleteTodoResponse> responseObserver) {
        boolean success = todoService.deleteTodo(request.getId());
        DeleteTodoResponse response = DeleteTodoResponse.newBuilder()
                .setSuccess(success)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    private com.example.todo.proto.Todo convertToProto(Todo todo) {
        return com.example.todo.proto.Todo.newBuilder()
                .setId(todo.getId())
                .setTitle(todo.getTitle())
                .setDescription(todo.getDescription())
                .setCompleted(todo.isCompleted())
                .build();
    }
}

