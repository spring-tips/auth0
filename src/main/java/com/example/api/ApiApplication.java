package com.example.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Map;

@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}
}

@Controller
@ResponseBody
class GreetingController {

    @GetMapping("/hello")
	Map<String, String> hello(Principal principal) {
		System.out.println("hello, from the API!");
        return Map.of("message", "Hello, " + principal.getName() + "!");
    }
}
