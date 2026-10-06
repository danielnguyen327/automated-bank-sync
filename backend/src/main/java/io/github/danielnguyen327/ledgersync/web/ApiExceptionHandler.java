package io.github.danielnguyen327.ledgersync.web;

import java.util.Comparator;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Errors come back as problem details (RFC 9457). The frontend shows the "detail" field as-is, so it
 * is always a sentence written for people.
 */
@RestControllerAdvice 
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String detail = ex.getBindingResult().getFieldErrors().stream()
        .sorted(Comparator.comparing(FieldError::getField))
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining(" "));
    ex.getBody().setDetail(detail);
    return handleExceptionInternal(ex, ex.getBody(), headers, status, request);
  }
}