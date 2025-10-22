package org.springbozo.meditracker;

import org.springbozo.meditracker.model.Person;
import org.springbozo.meditracker.repository.PersonRepository;
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
    CommandLineRunner seed(PersonRepository repo) {
        return args -> {
            if (repo.count() == 0) { // only seed if empty
                repo.saveAll(List.of(
                        new Person("Alice", "alice@example.com"),
                        new Person("Bob", "bob@example.com")
                ));
            }
        };
    }
}
