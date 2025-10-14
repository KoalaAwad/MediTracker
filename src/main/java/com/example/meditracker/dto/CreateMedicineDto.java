package com.example.meditracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateMedicineDto {
    @NotBlank(message = "Medicine name is required")
    private String name;

    private String description;

    @NotNull(message = "Dosage amount is required")
    @Positive(message = "Dosage amount must be positive")
    private Double dosageAmount;

    @NotBlank(message = "Dosage unit is required")
    private String dosageUnit;

    public CreateMedicineDto(){}

    public CreateMedicineDto(String name, String description, Double dosageAmount, String dosageUnit) {
        this.name = name;
        this.description = description;
        this.dosageAmount = dosageAmount;
        this.dosageUnit = dosageUnit;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getDosageAmount() { return dosageAmount; }
    public void setDosageAmount(Double dosageAmount) { this.dosageAmount = dosageAmount; }

    public String getDosageUnit() { return dosageUnit; }
    public void setDosageUnit(String dosageUnit) { this.dosageUnit = dosageUnit; }

}
