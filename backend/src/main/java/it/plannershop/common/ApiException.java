package it.plannershop.common;

import org.springframework.http.HttpStatus;

/**
 * Errore applicativo con stato HTTP: viene trasformato in una risposta
 * ProblemDetail da {@link ApiExceptionHandler}. Il messaggio è in italiano
 * ed è pensato per essere mostrato all'utente.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String messaggio) {
        super(messaggio);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException nonTrovato(String messaggio) {
        return new ApiException(HttpStatus.NOT_FOUND, messaggio);
    }

    public static ApiException conflitto(String messaggio) {
        return new ApiException(HttpStatus.CONFLICT, messaggio);
    }

    public static ApiException richiestaNonValida(String messaggio) {
        return new ApiException(HttpStatus.BAD_REQUEST, messaggio);
    }

    public static ApiException nonAutenticato(String messaggio) {
        return new ApiException(HttpStatus.UNAUTHORIZED, messaggio);
    }

    public static ApiException troppiTentativi(String messaggio) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, messaggio);
    }
}
