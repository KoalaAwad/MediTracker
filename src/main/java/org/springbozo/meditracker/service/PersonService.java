package org.springbozo.meditracker.service;

import org.springbozo.meditracker.model.Person;
import org.springbozo.meditracker.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PersonService {

    private PersonRepository personRepository;

    @Autowired
    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public boolean savePerson(Person person){
        personRepository.save(person);
        return true;
    }
}



