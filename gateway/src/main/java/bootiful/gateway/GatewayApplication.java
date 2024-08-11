package bootiful.gateway;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


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
