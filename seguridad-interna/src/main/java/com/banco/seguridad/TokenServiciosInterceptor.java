package com.banco.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

// Agrega "Authorization: Bearer <token servicios-internos>" a cada llamada. Ante un 401 descarta el
// token, pide uno nuevo y reintenta una sola vez (cubre el reinicio de auth-server, que regenera su clave).
public class TokenServiciosInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TokenServiciosInterceptor.class);

    private final TokenServiciosProvider tokenProvider;

    public TokenServiciosInterceptor(TokenServiciosProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String token = tokenProvider.obtenerToken();
        request.getHeaders().setBearerAuth(token);
        ClientHttpResponse respuesta = execution.execute(request, body);
        if (respuesta.getStatusCode().value() != 401) {
            return respuesta;
        }

        respuesta.close();
        logger.warn("{} {} respondio 401: se renueva el token de servicios-internos y se reintenta una vez",
                request.getMethod(), request.getURI());
        tokenProvider.invalidar(token);
        request.getHeaders().setBearerAuth(tokenProvider.obtenerToken());
        return execution.execute(request, body);
    }
}
