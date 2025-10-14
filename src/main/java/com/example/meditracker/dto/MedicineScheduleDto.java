package com.example.meditracker.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class MedicineScheduleDto {
    private Long id;
    private Long medicineId;
    private String medicineName; // Include medicine name for convenience
    private LocalTime timeOfDay;
    private String frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private String instructions;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MedicineScheduleDto(){}

    public MedicineScheduleDto(Long id, Long medicineId, String medicineName,
                               LocalTime timeOfDay, String frequency, LocalDate startDate,
                               LocalDate endDate, String instructions, Boolean isActive,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.timeOfDay = timeOfDay;
        this.frequency = frequency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.instructions = instructions;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMedicineId() { return medicineId; }
    public void setMedicineId(Long medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public LocalTime getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(LocalTime timeOfDay) { this.timeOfDay = timeOfDay; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }


}
