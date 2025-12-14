package com.ecommerce.common.advice;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle custom business exceptions (AppException)
     */
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("Business exception: {} - {}", errorCode.name(), errorCode.getMessage());

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    /**
     * Handle validation exceptions (@Valid)
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Map<String, String>>> handlingValidation(MethodArgumentNotValidException exception) {
        // Get the first error's message key
        String enumKey = exception.getFieldError() != null 
                ? exception.getFieldError().getDefaultMessage() 
                : "VALIDATION_ERROR";

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        try {
            errorCode = ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown validation error key: {}", enumKey);
        }

        // Collect all field errors
        Map<String, String> fieldErrors = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            
            // Try to get the message from ErrorCode if it exists
            try {
                ErrorCode fieldErrorCode = ErrorCode.valueOf(errorMessage);
                fieldErrors.put(fieldName, fieldErrorCode.getMessage());
            } catch (IllegalArgumentException e) {
                fieldErrors.put(fieldName, errorMessage);
            }
        });

        log.warn("Validation failed: {}", fieldErrors);

        ApiResponse<Map<String, String>> apiResponse = ApiResponse.<Map<String, String>>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .result(fieldErrors)
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    /**
     * Handle all uncaught exceptions
     */
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<Void>> handlingException(Exception exception) {
        log.error("Uncategorized exception: ", exception);

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .build();

        return ResponseEntity.status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode()).body(apiResponse);
    }
}
