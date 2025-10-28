package org.springbozo.meditracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Entity
public class Medicine {

    @Id
    @Column(name = "medicine_id")
    private int id;

    private String name;
    private String dosage;
    private String description;

    @OneToMany(mappedBy = "medicine", cascade = CascadeType.ALL,
            orphanRemoval = true,targetEntity = Schedule.class, fetch =  FetchType.EAGER)
    private List<Schedule> schedules = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

}
