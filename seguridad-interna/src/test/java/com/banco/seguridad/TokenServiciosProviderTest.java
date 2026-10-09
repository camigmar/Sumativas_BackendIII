package com.banco.seguridad;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiciosProviderTest {

    // Reloj que el test puede adelantar.
    static class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-10-10T10:00:00Z");
        void avanzar(Duration d) { ahora = ahora.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private final AtomicInteger emitidos = new AtomicInteger();
    private final Supplier<TokenEmitido> emisor =
            () -> new TokenEmitido("token-" + emitidos.incrementAndGet(), Duration.ofMinutes(10));

    @Test
    void reutilizaElTokenMientrasEsteVigente() {
        RelojManual reloj = new RelojManual();
        TokenServiciosProvider provider = new TokenServiciosProvider(emisor, reloj);

        String primero = provider.obtenerToken();
        reloj.avanzar(Duration.ofMinutes(5));
        String segundo = provider.obtenerToken();

        assertThat(primero).isEqualTo("token-1");
        assertThat(segundo).isEqualTo("token-1");
        assertThat(emitidos).hasValue(1);
    }

    @Test
    void loRenuevaAntesDeQueExpire() {
        RelojManual reloj = new RelojManual();
        TokenServiciosProvider provider = new TokenServiciosProvider(emisor, reloj);
        provider.obtenerToken();

        // Faltan 59 s para expirar: menos que el margen de renovacion (60 s).
        reloj.avanzar(Duration.ofMinutes(10).minusSeconds(59));

        assertThat(provider.obtenerToken()).isEqualTo("token-2");
    }

    @Test
    void invalidarDescartaSoloElTokenRechazado() {
        TokenServiciosProvider provider = new TokenServiciosProvider(emisor, new RelojManual());
        provider.obtenerToken();

        provider.invalidar("otro-token");
        assertThat(provider.obtenerToken()).isEqualTo("token-1");

        provider.invalidar("token-1");
        assertThat(provider.obtenerToken()).isEqualTo("token-2");
    }

    @Test
    void peticionesConcurrentesPidenUnSoloToken() throws Exception {
        CountDownLatch partida = new CountDownLatch(1);
        Supplier<TokenEmitido> emisorLento = () -> {
            emitidos.incrementAndGet();
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return new TokenEmitido("token-unico", Duration.ofMinutes(10));
        };
        TokenServiciosProvider provider = new TokenServiciosProvider(emisorLento, new RelojManual());
        ExecutorService hilos = Executors.newFixedThreadPool(8);
        List<Future<String>> resultados = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            resultados.add(hilos.submit(() -> { partida.await(); return provider.obtenerToken(); }));
        }

        partida.countDown();
        for (Future<String> resultado : resultados) {
            assertThat(resultado.get(5, TimeUnit.SECONDS)).isEqualTo("token-unico");
        }
        hilos.shutdown();
        assertThat(emitidos).hasValue(1);
    }
}
