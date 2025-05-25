package com.example.onlinebankingapp.exceptions;

import com.example.onlinebankingapp.dtos.responses.ResponseObject;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Optional;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ResponseObject> handlingAppException(AppException Exception) {
        return ResponseEntity.status(Exception.getErrorCode().getStatus())
                .body(ResponseObject.builder()
                        .code(Exception.getErrorCode().getCode())
                        .message(Exception.getMessage())
                        .status(Exception.getErrorCode().getStatus())
                        .build());
    }

    @ExceptionHandler(value = RuntimeException.class)
    ResponseEntity<ResponseObject> handlingRuntimeException(RuntimeException Exception) {
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ResponseObject.builder()
                        .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                        .message(Exception.getMessage())
                        .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                        .build());
    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ResponseObject> handlingException(Exception Exception) {
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ResponseObject.builder()
                        .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                        .message(Exception.getMessage())
                        .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                        .build());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ResponseObject> handlingValidation(MethodArgumentNotValidException Exception) {
        String message = Optional.ofNullable(Exception.getFieldError())
                .map(FieldError::getDefaultMessage)
                .orElse(null);

        return ResponseEntity.status(ErrorCode.BAD_REQUEST.getStatus())
                .body(ResponseObject.builder()
                        .code(ErrorCode.BAD_REQUEST.getCode())
                        .message(message)
                        .status(ErrorCode.BAD_REQUEST.getStatus())
                        .build());
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseObject> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String customMessage = "Invalid input format";


        return ResponseEntity.status(ErrorCode.BAD_REQUEST.getStatus())
                .body(ResponseObject.builder()
                        .code(ErrorCode.BAD_REQUEST.getCode())
                        .message(customMessage)
                        .status(ErrorCode.BAD_REQUEST.getStatus())
                        .build());
    }

    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    ResponseEntity<ResponseObject> handlingMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException Exception) {
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ResponseObject.builder()
                        .code(ErrorCode.INVALID_INPUT.getCode())
                        .message(ErrorCode.INVALID_INPUT.getMessage())
                        .status(ErrorCode.BAD_REQUEST.getStatus())
                        .build());
    }
}
