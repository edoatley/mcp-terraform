package com.example.todo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@RestControllerAdvice
public class DynamoDbExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DynamoDbExceptionHandler.class);

    @ExceptionHandler(DynamoDbException.class)
    public ResponseEntity<ErrorResponse> handleDynamoDbException(DynamoDbException e) {
        logger.error("DynamoDB error occurred", e);
        
        ErrorResponse error = new ErrorResponse(
                "Database error",
                "An error occurred while accessing the database. Please try again later."
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        // Check if it's a DynamoDB-related runtime exception
        if (e.getMessage() != null && e.getMessage().contains("DynamoDB")) {
            logger.error("DynamoDB-related error occurred", e);
            ErrorResponse error = new ErrorResponse(
                    "Database error",
                    "An error occurred while accessing the database. Please try again later."
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
        
        // Re-throw if not DynamoDB related
        throw e;
    }

    public record ErrorResponse(String error, String message) {
    }
}



