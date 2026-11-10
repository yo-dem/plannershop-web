package it.plannershop.security;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import it.plannershop.common.ApiException;

/**
 * Limita i tentativi di login falliti per indirizzo IP e per email, per
 * rallentare chi prova a indovinare le password.
 *
 * <p>In memoria: si azzera al riavvio e vale per una sola istanza del backend,
 * sufficiente per l'uso previsto.
 */
@Component
public class LoginAttemptLimiter {

    private final SecurityProperties props;
    private final Clock clock = Clock.systemUTC();
    private final Map<String, Deque<Instant>> fallimenti = new ConcurrentHashMap<>();

    public LoginAttemptLimiter(SecurityProperties props) {
        this.props = props;
    }

    public void verifica(String ip, String email) {
        if (bloccato(chiaveIp(ip)) || bloccato(chiaveEmail(email))) {
            throw ApiException.troppiTentativi("Troppi tentativi di accesso. Riprova tra qualche minuto.");
        }
    }

    public void registraFallimento(String ip, String email) {
        aggiungi(chiaveIp(ip));
        aggiungi(chiaveEmail(email));
    }

    public void registraSuccesso(String email) {
        fallimenti.remove(chiaveEmail(email));
    }

    private boolean bloccato(String chiave) {
        Deque<Instant> lista = fallimenti.get(chiave);
        if (lista == null) {
            return false;
        }
        synchronized (lista) {
            pulisci(lista);
            return lista.size() >= props.loginMaxTentativi();
        }
    }

    private void aggiungi(String chiave) {
        Deque<Instant> lista = fallimenti.computeIfAbsent(chiave, k -> new ArrayDeque<>());
        synchronized (lista) {
            pulisci(lista);
            lista.addLast(clock.instant());
        }
    }

    private void pulisci(Deque<Instant> lista) {
        Instant limite = clock.instant().minus(props.loginFinestra());
        while (!lista.isEmpty() && lista.peekFirst().isBefore(limite)) {
            lista.removeFirst();
        }
    }

    private static String chiaveIp(String ip) {
        return "ip:" + ip;
    }

    private static String chiaveEmail(String email) {
        return "email:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }
}
