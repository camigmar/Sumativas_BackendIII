package com.banco.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    // Builder sin balanceo: lo usa el propio cliente de Eureka para hablar con
    // localhost:8761. Sin este @Primary, Eureka tomaria el builder @LoadBalanced
    // e intentaria resolver "localhost" como nombre de servicio (dependencia circular).
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    // Timeouts cortos: si servicio-clientes no responde, la apertura falla rapido.
    @Bean
    public RestClient clientesRestClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
                                         @Value("${clientes.api.base-url}") String baseUrl,
                                         @Value("${clientes.api.connect-timeout}") Duration connectTimeout,
                                         @Value("${clientes.api.read-timeout}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return loadBalancedRestClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
