package org.springbozo.meditracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Integer id;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // Extra fields (not in ER) retained but clearly mapped
    @Column(name = "blood_type")
    private String bloodType;

    // Fix: 'name' not 'columnDefinition'
    @Column(name = "allergies")
    private String allergies;

    // New fields per ER
    @Column(length = 20)
    private String gender;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(name = "medical_history")
    private String medicalHistory;

    @Column(name = "name", nullable = false)
    private String name;


    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToMany
    @JoinTable(
            name = "patient_doctor",
            joinColumns = @JoinColumn(name = "patient_id"),
            inverseJoinColumns = @JoinColumn(name = "doctor_id")
    )
    private Set<Doctor> doctors = new HashSet<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prescription> PrescribedMedicine = new ArrayList<>();

    // Active flag: when role removed, mark inactive instead of deleting record
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Transient
    public String getEmail() {
        return this.user == null ? null : this.user.getEmail();
    }

    @Transient
    public String getName() {
        return this.user == null ? null : this.user.getName();
    }

}