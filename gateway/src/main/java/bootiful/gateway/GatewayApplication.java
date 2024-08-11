package bootiful.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;


// https://github.com/auth0-samples/auth0-spring-security5-api-sample/commit/b6d4229977d0c0c88f92ffe799a0f0143b30e3fc
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    WebClient http(ReactiveClientRegistrationRepository clientRegistrations,
                   ServerOAuth2AuthorizedClientRepository authorizedClients,
                   WebClient.Builder builder) {
        var oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients);
        oauth.setDefaultOAuth2AuthorizedClient(true);
        return builder.filter(oauth).build();
    }
}

@Controller
@ResponseBody
class Client {

    private final WebClient http;

    Client(WebClient http) {
        this.http = http;
    }

    @GetMapping("/hello")
    Mono<String> hello() {
        return http
                .get()
                .uri("http://localhost:8080/hello")
                .retrieve()
                .bodyToMono(String.class);
    }

}


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