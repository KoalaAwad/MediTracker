package com.example.meditracker.dto;

import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MedicineDto {
    private Long id;
    private String name;
    private String description;
    private Double dosageAmount;
    private String dosageUnit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MedicineScheduleDto> schedules;

    public MedicineDto(){}

    public MedicineDto(Long id,String name, String description,
                       Double dosageAmount, String dosageUnit,
                       LocalDateTime createdAt, LocalDateTime updatedAt){
        this.id = id;
        this.name = name;
        this.description = description;
        this.dosageAmount = dosageAmount;
        this.dosageUnit = dosageUnit;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


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

    public List<MedicineScheduleDto> getSchedules() { return schedules; }
    public void setSchedules(List<MedicineScheduleDto> schedules) { this.schedules = schedules; }

}
