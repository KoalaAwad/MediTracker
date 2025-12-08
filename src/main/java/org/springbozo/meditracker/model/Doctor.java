package org.springbozo.meditracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doctor_id")
    private Integer id;

    // Make these nullable so doctor profile is optional
    @Column(name = "first_name", length = 120)
    private String firstName;

    @Column(name = "last_name", length = 120)
    private String lastName;

    @Column(length = 150)
    private String specialization;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(length = 30)
    private String phone;

    @Column(name = "clinic_address", length = 255)
    private String clinicAddress;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToMany(mappedBy = "doctors")
    private Set<Patient> patients = new HashSet<>();

    // Active flag so records are soft-disabled when role removed
    @Column(name = "active", nullable = false)
    private boolean active = true;
}