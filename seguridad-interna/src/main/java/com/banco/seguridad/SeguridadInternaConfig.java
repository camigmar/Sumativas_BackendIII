package com.banco.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

// Se importa con @Import(SeguridadInternaConfig.class) en cada servicio que llama a otros.
// Propiedades (config-repo): servicios-internos.token-uri, client-id, client-secret ({cipher}) y scope.
@Configuration
public class SeguridadInternaConfig {

    @Bean
    public TokenServiciosProvider tokenServiciosProvider(
            @Value("${servicios-internos.token-uri}") String tokenUri,
            @Value("${servicios-internos.client-id:servicios-internos}") String clientId,
            @Value("${servicios-internos.client-secret}") String clientSecret,
            @Value("${servicios-internos.scope:interno}") String scope,
            @Value("${servicios-internos.timeout:3s}") Duration timeout) {
        return new TokenServiciosProvider(
                new AuthServerTokenEmisor(tokenUri, clientId, clientSecret, scope, timeout), Clock.systemUTC());
    }

    @Bean
    public TokenServiciosInterceptor tokenServiciosInterceptor(TokenServiciosProvider tokenServiciosProvider) {
        return new TokenServiciosInterceptor(tokenServiciosProvider);
    }
}
