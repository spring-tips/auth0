package bootiful.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.function.Consumer;


// https://github.com/auth0-samples/auth0-spring-security5-api-sample/commit/b6d4229977d0c0c88f92ffe799a0f0143b30e3fc
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb,
                         @Value("${mogul.gateway.api:http://localhost:8080}") String api) {
        var apiPrefix = "/api/";
        return rlb//
                .routes()
                .route(rs -> rs.path(apiPrefix + "**")
                        .filters(f -> f.tokenRelay().rewritePath(apiPrefix + "(?<segment>.*)", "/$\\{segment}"))
                        .uri(api))
                .build();
    }

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http//
                .authorizeExchange((authorize) -> authorize//
                        .anyExchange()
                        .authenticated()//
                )//
                .csrf(ServerHttpSecurity.CsrfSpec::disable)//
                .oauth2Login(Customizer.withDefaults())//
                .oauth2Client(Customizer.withDefaults())//
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
                          @Value("${okta.oauth2.audience}") String audience) {
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

        DefaultServerOAuth2AuthorizationRequestResolver authorizationRequestResolver =
                new DefaultServerOAuth2AuthorizationRequestResolver(
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