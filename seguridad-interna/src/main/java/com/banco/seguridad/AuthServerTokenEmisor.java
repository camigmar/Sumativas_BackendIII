package com.banco.seguridad;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

// Pide un token a auth-server con client_credentials (client_secret_basic). Usa un RestClient propio,
// sin balanceo: auth-server se llama por su URL directa, no por Eureka.
public class AuthServerTokenEmisor implements Supplier<TokenEmitido> {

    private final RestClient restClient;
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final String scope;

    public AuthServerTokenEmisor(String tokenUri, String clientId, String clientSecret, String scope, Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
    }

    @Override
    @SuppressWarnings("unchecked")
    public TokenEmitido get() {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("grant_type", "client_credentials");
        formulario.add("scope", scope);
        try {
            Map<String, Object> respuesta = restClient.post()
                    .uri(tokenUri)
                    .headers(h -> h.setBasicAuth(clientId, clientSecret))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .body(Map.class);
            if (respuesta == null || !(respuesta.get("access_token") instanceof String token)) {
                throw new IllegalStateException("auth-server no devolvio access_token");
            }
            long segundos = respuesta.get("expires_in") instanceof Number n ? n.longValue() : 300L;
            return new TokenEmitido(token, Duration.ofSeconds(segundos));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("No se pudo obtener el token de " + clientId + " en " + tokenUri, ex);
        }
    }
}
