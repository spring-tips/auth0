package bootiful.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Map;

// https://developer.auth0.com/resources/labs/authorization/spring-boot-microservices-security#overview
@SpringBootApplication
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}

@Controller
@ResponseBody
class GreetingsController {

    @GetMapping("/hello")
    Map<String, String> hello(Principal principal) {
        return Map.of("messages", "Hello, " + principal.getName() + ", from the backend API!");
    }
}
