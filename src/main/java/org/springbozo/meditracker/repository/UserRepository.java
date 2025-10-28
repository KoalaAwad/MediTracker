package org.springbozo.meditracker.repository;

import org.springbozo.meditracker.model.Person;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository extends CrudRepository<Person,Integer> {
    public Optional<Person> findByEmail(String email);
}
