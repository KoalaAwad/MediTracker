package org.springbozo.meditracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prescription_id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @Column(name = "week_days", length = 100)
    private String weekDays; // e.g. "Mon,Tue,Thu"

    // Times of day when the medicine should be taken
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "prescription_day_time", joinColumns = @JoinColumn(name = "prescription_id"))
    @Column(name = "time_of_day")
    private List<LocalTime> dayTimes = new ArrayList<>();

    @Column(name = "dosage_amount")
    private int dosageAmount;

    @Column(name = "dosage_unit", length = 50)
    private String dosageUnit;

    @Column(name = "start_date")
    private LocalDate startDate; // date-only; optional (defaults to today)

    @Column(name = "end_date")
    private LocalDate endDate; // date-only; optional

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // dosages relationship removed intentionally — handled separately in PrescriptionDosage if needed

    @PrePersist
    protected void onCreate() {
        var now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}