package com.banco.seguridad;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

// Cachea el token de "servicios-internos" y lo renueva antes de que expire. Es thread-safe:
// varias peticiones concurrentes comparten un solo token y una sola renovacion.
public class TokenServiciosProvider {

    // Se renueva cuando faltan menos de 60 s, para no enviar un token que expire en el camino.
    static final Duration MARGEN_RENOVACION = Duration.ofSeconds(60);

    private final Supplier<TokenEmitido> emisor;
    private final Clock clock;

    private String token;
    private Instant expiracion;

    public TokenServiciosProvider(Supplier<TokenEmitido> emisor, Clock clock) {
        this.emisor = emisor;
        this.clock = clock;
    }

    public synchronized String obtenerToken() {
        if (token == null || !clock.instant().isBefore(expiracion.minus(MARGEN_RENOVACION))) {
            TokenEmitido nuevo = emisor.get();
            token = nuevo.valor();
            expiracion = clock.instant().plus(nuevo.vigencia());
        }
        return token;
    }

    // Descarta el token si es el que se rechazo (401), por ejemplo porque auth-server se reinicio
    // y cambio su clave de firma. Si otro hilo ya lo renovo, no hace nada.
    public synchronized void invalidar(String tokenRechazado) {
        if (tokenRechazado != null && tokenRechazado.equals(token)) {
            token = null;
            expiracion = null;
        }
    }
}
