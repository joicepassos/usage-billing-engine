package io.github.joicepassos.billing.shared.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every HTTP error leaves as RFC 9457 Problem Details with a {@code requestId}. Validation failures are 422 with the
 * invalid fields; unexpected failures are 500 with a generic message and the details only in the log. See ADR 0008.
 */
@RestControllerAdvice
public class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {

    static final String TYPE_BASE = "https://usage-billing-engine/errors/";
    private static final Logger log = LoggerFactory.getLogger(ProblemDetailsAdvice.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, List<String>> errors = new TreeMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors
                .computeIfAbsent(error.getField(), field -> new ArrayList<>())
                .add(error.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT,
                errors.size() + (errors.size() == 1 ? " field is invalid" : " fields are invalid"));
        problem.setType(URI.create(TYPE_BASE + "validation"));
        problem.setTitle("Invalid request");
        problem.setProperty("errors", errors);
        return createResponseEntity(problem, headers, HttpStatus.UNPROCESSABLE_CONTENT, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Not found", ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<Object> handleConflict(ConflictException ex, WebRequest request) {
        return problem(HttpStatus.CONFLICT, "conflict", "Conflict", ex.getMessage(), request);
    }

    private ResponseEntity<Object> problem(HttpStatus status, String type, String title, String detail,
            WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_BASE + type));
        problem.setTitle(title);
        return createResponseEntity(problem, new HttpHeaders(), status, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Quote the request id when reporting it.");
        problem.setType(URI.create(TYPE_BASE + "internal"));
        problem.setTitle("Internal error");
        return createResponseEntity(problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode,
            WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            Object requestId = request.getAttribute(RequestIdFilter.ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (requestId != null) {
                Map<String, Object> properties = problem.getProperties() == null
                        ? new LinkedHashMap<>() : new LinkedHashMap<>(problem.getProperties());
                properties.put("requestId", requestId);
                problem.setProperties(properties);
            }
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }
}
