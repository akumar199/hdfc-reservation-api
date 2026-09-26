package com.example.idempotency.exception;

import java.util.Map;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @SuppressWarnings("deprecation")
	@ExceptionHandler(IdempotencyKeyReusedException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleIdempotencyKeyReused(
            IdempotencyKeyReusedException exception) {

        return Map.of(
                "error", "idempotency_key_reused"
        );
    }
}
	
