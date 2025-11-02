package com.hoaiphong.quanlysanpham.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleValidationErrors(Exception ex, WebRequest request) {
        // Extract field errors thành Map (như cũ)
        Map<String, String> errors = new LinkedHashMap<>();


        if (ex instanceof MethodArgumentNotValidException validEx) {
            // Case MethodArgumentNotValidException (từ @Valid trên DTO)
            validEx.getBindingResult().getFieldErrors().forEach(err ->
                    errors.put(err.getField(), err.getDefaultMessage())
            );
        } else if (ex instanceof ConstraintViolationException constraintEx) {
            // Case ConstraintViolationException (từ @Validated trên method params hoặc manual validate)
            constraintEx.getConstraintViolations().forEach(cv ->
                    errors.put(cv.getPropertyPath().toString(), cv.getMessage())
            );
        } else {
            errors.put("unknown", "Validation error occurred");
        }

        // Tạo ErrorResponse
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(new Date());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));  // Lấy path từ request


        if (ex instanceof MethodArgumentNotValidException) {
            errorResponse.setError("Request Body Invalid");
        } else if (ex instanceof ConstraintViolationException) {
            errorResponse.setError("Request Parameter Invalid");
        } else {
            errorResponse.setError("Something Went Wrong");
        }

        // Set message: Có thể concatenate hoặc JSON
        String message = errors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("; "));
        errorResponse.setMessage(message);  // Ví dụ: "name: must not be blank; age: must be positive"

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        // Tạo ErrorResponse
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(new Date());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

        // Set error phù hợp (param type error)
        errorResponse.setError("Request Parameter Invalid");

        // Extract message chi tiết từ exception
        String paramName = ex.getName();
        String inputValue = ex.getValue() != null ? ex.getValue().toString() : "unknown";
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("Failed to convert '%s' to %s: invalid input '%s'", paramName, requiredType, inputValue);
        errorResponse.setMessage(message);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(SomeThingWrongException.class)
    public ResponseEntity<ErrorResponse> handleSomeThingWrongException(SomeThingWrongException ex,WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(new Date());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError("Something went wrong");
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        return ResponseEntity.badRequest().body(errorResponse);
    }

//    @ExceptionHandler(ResourceNotFoundException.class)
//    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
//        ErrorResponse errorResponse = new ErrorResponse();
//        errorResponse.setTimestamp(new Date());
//        errorResponse.setStatus(HttpStatus.NOT_FOUND.value());
//        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
//        errorResponse.setError("Resource Not Found");
//        errorResponse.setMessage(ex.getMessage());
//
//        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
//    }
}
