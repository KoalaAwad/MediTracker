package org.springbozo.meditracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medicine_id")
    private int id;

    @Column(name = "medicine_name")
    private String name;

    @Column(name = "generic_name")
    private String genericName;

    private String manufacturer;

    @Column(name = "dosage_form")
    private String dosageForm;

    private String strength;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "side_effects", columnDefinition = "text")
    private String sideEffects;

    @Column(columnDefinition = "text")
    private String contraindications;

}