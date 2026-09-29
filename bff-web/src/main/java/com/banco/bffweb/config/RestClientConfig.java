package com.banco.bffweb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

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

    @Bean
    public RestClient coreRestClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
                                     @Value("${core.api.base-url}") String baseUrl) {
        return loadBalancedRestClientBuilder.baseUrl(baseUrl).build();
    }
}
