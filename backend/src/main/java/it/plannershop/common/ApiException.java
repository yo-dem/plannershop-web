package it.plannershop.common;

import org.springframework.http.HttpStatus;

/**
 * Application error with an HTTP status, turned into a ProblemDetail response
 * by {@link ApiExceptionHandler}. The message is a key in messages*.properties,
 * translated into the language of the request (Accept-Language).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String messageKey;
    private final transient Object[] args;

    public ApiException(HttpStatus status, String messageKey, Object... args) {
        super(messageKey);
        this.status = status;
        this.messageKey = messageKey;
        this.args = args;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }

    public static ApiException notFound(String messageKey, Object... args) {
        return new ApiException(HttpStatus.NOT_FOUND, messageKey, args);
    }

    public static ApiException conflict(String messageKey, Object... args) {
        return new ApiException(HttpStatus.CONFLICT, messageKey, args);
    }

    public static ApiException badRequest(String messageKey, Object... args) {
        return new ApiException(HttpStatus.BAD_REQUEST, messageKey, args);
    }

    public static ApiException unauthorized(String messageKey, Object... args) {
        return new ApiException(HttpStatus.UNAUTHORIZED, messageKey, args);
    }

    public static ApiException forbidden(String messageKey, Object... args) {
        return new ApiException(HttpStatus.FORBIDDEN, messageKey, args);
    }

    public static ApiException tooManyRequests(String messageKey, Object... args) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, messageKey, args);
    }
}
