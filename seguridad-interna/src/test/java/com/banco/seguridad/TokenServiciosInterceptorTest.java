package com.banco.seguridad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Usa la cadena real de interceptores de RestClient contra un servidor simulado.
class TokenServiciosInterceptorTest {

    private final AtomicInteger emitidos = new AtomicInteger();
    private MockRestServiceServer servidor;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        TokenServiciosProvider provider = new TokenServiciosProvider(
                () -> new TokenEmitido("token-" + emitidos.incrementAndGet(), Duration.ofHours(1)), Clock.systemUTC());
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://servicio-cuentas")
                .requestInterceptor(new TokenServiciosInterceptor(provider));
        servidor = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    @Test
    void enviaElTokenComoBearer() {
        servidor.expect(requestTo("http://servicio-cuentas/api/core/cuentas/101"))
                .andExpect(header("Authorization", "Bearer token-1"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        restClient.get().uri("/api/core/cuentas/101").retrieve().toBodilessEntity();

        servidor.verify();
    }

    @Test
    void ante401RenuevaElTokenYReintentaUnaVez() {
        servidor.expect(requestTo("http://servicio-cuentas/api/core/cuentas/101/debito"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-1"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        servidor.expect(requestTo("http://servicio-cuentas/api/core/cuentas/101/debito"))
                .andExpect(header("Authorization", "Bearer token-2"))
                .andRespond(withSuccess("{\"nuevoSaldo\":10.0}", MediaType.APPLICATION_JSON));

        String cuerpo = restClient.post().uri("/api/core/cuentas/101/debito")
                .contentType(MediaType.APPLICATION_JSON).body("{\"monto\":5}")
                .retrieve().body(String.class);

        assertThat(cuerpo).contains("nuevoSaldo");
        assertThat(emitidos).hasValue(2);
        servidor.verify();
    }

    @Test
    void siElReintentoTambienDa401NoReintentaMas() {
        servidor.expect(requestTo("http://servicio-cuentas/api/core/cuentas/101"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        servidor.expect(requestTo("http://servicio-cuentas/api/core/cuentas/101"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(HttpClientErrorException.Unauthorized.class,
                () -> restClient.get().uri("/api/core/cuentas/101").retrieve().toBodilessEntity());
        servidor.verify();
    }

    @Test
    void un403NoSeReintenta() {
        servidor.expect(requestTo("http://servicio-cuentas/api/core/cuentas/101"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(HttpClientErrorException.Forbidden.class,
                () -> restClient.get().uri("/api/core/cuentas/101").retrieve().toBodilessEntity());
        assertThat(emitidos).hasValue(1);
        servidor.verify();
    }
}
