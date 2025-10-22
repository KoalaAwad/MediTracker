package org.springbozo.meditracker.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "person_id")
    private int Personid;
    private String name;
    private String email;

    public Person(){}

    public Person(String name, String email) {
        this.name = name;
        this.email = email;
    }
}


