package com.example.meditracker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="medicines")
public class Medicine{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Medicine name is required")
    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @NotNull(message = "Dosage amount is required")
    @Positive(message = "Dosage amount invalid")
    @Column(nullable = false)
    private Double dosageAmount;

    @NotBlank(message = "Dosage unit is required")
    @Column(nullable = false)
    private String dosageUnit;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "medicine", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MedicineSchedule> schedules = new ArrayList<>();

    public Medicine(){}

    public Medicine(String name, String description, Double dosageAmount, String dosageUnit){
        this.name = name;
        this.description = description;
        this.dosageAmount = dosageAmount;
        this.dosageUnit = dosageUnit;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    //Getters n Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getDosageAmount() { return dosageAmount; }
    public void setDosageAmount(Double dosageAmount) { this.dosageAmount = dosageAmount; }

    public String getDosageUnit() { return dosageUnit; }
    public void setDosageUnit(String dosageUnit) { this.dosageUnit = dosageUnit; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<MedicineSchedule> getSchedules() { return schedules; }
    public void setSchedules(List<MedicineSchedule> schedules) { this.schedules = schedules; }


    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }



}
