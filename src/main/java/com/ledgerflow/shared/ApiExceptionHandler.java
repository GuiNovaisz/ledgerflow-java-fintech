package com.ledgerflow.shared;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ProblemDetail business(BusinessException e) {
        return problem(e.getStatus(), e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    ProblemDetail invalid(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Check the request fields, formats and limits.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(DataIntegrityViolationException e) {
        return problem(HttpStatus.CONFLICT, "DATA_CONFLICT", "The operation conflicts with an existing record or financial constraint.");
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    ProblemDetail busy(PessimisticLockingFailureException e) {
        return problem(HttpStatus.CONFLICT, "RETRY_OPERATION", "Concurrent operation; retry with the same idempotency key.");
    }

    @ExceptionHandler(Exception.class)
    org.springframework.http.ResponseEntity<ProblemDetail> unexpected(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse response) {
            ProblemDetail detail = ProblemDetail.forStatusAndDetail(response.getStatusCode(), "The HTTP request could not be processed.");
            detail.setProperty("code", "HTTP_REQUEST_ERROR");
            return org.springframework.http.ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders()).body(detail);
        }
        String reference = UUID.randomUUID().toString();
        // Log class and reference only: SQL parameters, passwords and bearer tokens never belong in logs.
        LOG.error("Request failure reference={} type={}", reference, e.getClass().getSimpleName());
        ProblemDetail detail = problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected failure. Contact support with the reference.");
        detail.setProperty("reference", reference);
        return org.springframework.http.ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(detail);
    }

    private ProblemDetail problem(HttpStatus status, String code, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setProperty("code", code);
        return detail;
    }
}
