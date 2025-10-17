package org.springbozo.meditracker;

import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class MediTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediTrackerApplication.class, args);
    }

    @Bean
    CommandLineRunner seed(UserRepository repo) {
        return args -> {
            if (repo.count() == 0) { // only seed if empty
                repo.saveAll(List.of(
                        new User("Alice", "alice@example.com"),
                        new User("Bob", "bob@example.com")
                ));
            }
        };
    }
}
