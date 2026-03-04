package com.kmoroz.stockalert.alert.exception;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import com.kmoroz.stockalert.common.dto.ErrorResponse;

import java.time.Instant;

@Controller
@Slf4j
public class GlobalExceptionHandler {

    @io.micronaut.http.annotation.Error(global = true, exception = PersistenceException.class)
    public HttpResponse<ErrorResponse> handleIConstraintViolationException(HttpRequest<?> request,
                                                                           PersistenceException exception) {
        logException(request, exception);
        return HttpResponse.serverError(buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage()));
    }

    @io.micronaut.http.annotation.Error(global = true, exception = ConstraintViolationException.class)
    public HttpResponse<ErrorResponse> handleIPersistenceViolationException(HttpRequest<?> request,
                                                                           ConstraintViolationException exception) {
        logException(request, exception);
        return HttpResponse.badRequest(buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage()));
    }

    @io.micronaut.http.annotation.Error(global = true, exception = RuntimeException.class)
    public HttpResponse<ErrorResponse> handleInternalError(HttpRequest<?> request, RuntimeException exception) {
        logException(request, exception);
        return HttpResponse.serverError(buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage()));
    }

    private void logException(HttpRequest<?> request, Throwable exception) {
        log.error("{} occurred for {} {}: {}",
                exception.getClass().getSimpleName(), request.getMethod(),
                request.getUri(), exception.getMessage(), exception);
    }

    private ErrorResponse buildErrorResponse(HttpStatus httpStatus, String errorMessage) {
        return ErrorResponse.builder()
                .code(httpStatus.getCode())
                .reason(httpStatus.getReason())
                .message(errorMessage)
                .timestamp(Instant.now())
                .build();
    }
}
