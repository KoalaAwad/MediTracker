package org.springbozo.meditracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "prescription_dosage")
public class PrescriptionDosage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dosage_id")
    private Integer id;

    @Column(name = "dosage_sequence")
    private Integer dosageSequence;

    @Column(name = "dosage_amount")
    private String dosageAmount;

    @Column(name = "time_of_day")
    private LocalTime timeOfDay;

    @Column(name = "condition_description")
    private String condition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

}
