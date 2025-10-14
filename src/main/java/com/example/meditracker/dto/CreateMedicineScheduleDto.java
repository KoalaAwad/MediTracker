package com.example.meditracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class CreateMedicineScheduleDto {
    @NotNull(message = "Medicine is required")
    private Long medicineId;

    @NotNull(message = "Time of day is required")
    private LocalTime timeOfDay;

    @NotBlank(message = "Frequency is required")
    private String frequency;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private String instructions;

    private Boolean isActive = true;

    public CreateMedicineScheduleDto() {}

    public CreateMedicineScheduleDto(Long medicineId, LocalTime timeOfDay, String frequency,
                                     LocalDate startDate, LocalDate endDate, String instructions, Boolean isActive) {
        this.medicineId = medicineId;
        this.timeOfDay = timeOfDay;
        this.frequency = frequency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.instructions = instructions;
        this.isActive = isActive != null ? isActive : true;
    }

    public Long getMedicineId() { return medicineId; }
    public void setMedicineId(Long medicineId) { this.medicineId = medicineId; }

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



}
