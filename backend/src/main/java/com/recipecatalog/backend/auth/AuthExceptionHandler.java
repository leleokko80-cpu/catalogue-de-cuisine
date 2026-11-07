package com.recipecatalog.backend.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.recipecatalog.backend.auth")
public class AuthExceptionHandler {

    @ExceptionHandler(AuthService.EmailAlreadyUsedException.class)
    public ResponseEntity<AuthError> conflict(AuthService.EmailAlreadyUsedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthError(e.getMessage(), null));
    }

    @ExceptionHandler(AuthService.InvalidCredentialsException.class)
    public ResponseEntity<AuthError> unauthorized(AuthService.InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthError(e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthError> validation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new AuthError("Validation failed", errors));
    }
}
