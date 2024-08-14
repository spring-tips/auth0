package com.example.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.function.Consumer;

@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Bean
    RouteLocator gateway(RouteLocatorBuilder builder) {
        return builder
                .routes()
                .route(rs -> rs
                        .path("/*")
                        .filters(GatewayFilterSpec::tokenRelay)
                        .uri("http://localhost:9090")
                )
                .build();
    }

}


/**
 * a useful fix from <a href="https://github.com/okta/okta-spring-boot/issues/596">Matt Raible</a>
 */
@Configuration
class SecurityConfiguration {

    private final String audience;

    private final ReactiveClientRegistrationRepository clientRegistrationRepository;

    SecurityConfiguration(ReactiveClientRegistrationRepository clientRegistrationRepository,
                          @Value("${auth0.audience}") String audience) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.audience = audience;
    }

    @Bean
    SecurityWebFilterChain filterChain(ServerHttpSecurity http) throws Exception {
        http
                .authorizeExchange(authz -> authz
                        .anyExchange().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationRequestResolver(
                                authorizationRequestResolver(this.clientRegistrationRepository)
                        )
                );
        return http.build();
    }

    private ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ReactiveClientRegistrationRepository clientRegistrationRepository) {

        var authorizationRequestResolver = new DefaultServerOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository);
        authorizationRequestResolver.setAuthorizationRequestCustomizer(
                authorizationRequestCustomizer());

        return authorizationRequestResolver;
    }

    private Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer() {
        return customizer -> customizer
                .additionalParameters(params -> params.put("audience", audience));
    }
}