package com.team19.musuimsa.exception.handler;

import com.team19.musuimsa.exception.auth.AuthenticationException;
import com.team19.musuimsa.exception.auth.InvalidPasswordException;
import com.team19.musuimsa.exception.conflict.DataConflictException;
import com.team19.musuimsa.exception.dto.ErrorResponseDto;
import com.team19.musuimsa.exception.notfound.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataConflictException.class)
    public ResponseEntity<ErrorResponseDto> handleDataConflictException(
            DataConflictException ex,
            HttpServletRequest request) {

        return handleException(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityNotFoundException(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        return handleException(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        return handleException(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidPasswordException(
            InvalidPasswordException ex,
            HttpServletRequest request) {

        return handleException(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return handleException(
                HttpStatus.BAD_REQUEST,
                errorMessage,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {

        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);

        return handleException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 내부 오류가 발생했습니다.",
                request.getRequestURI()
        );
    }

    private ResponseEntity<ErrorResponseDto> handleException(
            HttpStatus status,
            String message,
            String path) {

        return ResponseEntity.status(status)
                .body(ErrorResponseDto.from(status, message, path));
    }
}
