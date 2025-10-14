package com.example.meditracker.aspect;

import com.example.meditracker.aspect.annotation.ValidateMedicine;
import com.example.meditracker.entity.Medicine;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ValidationAspect {
    @Before("@annotation(validateMedicine)")
    public void validateMedicineData(JoinPoint joinPoint, ValidateMedicine validateMedicine) {
        Object[] args = joinPoint.getArgs();

        Medicine medicine = null;
        for (Object arg : args){
            if (arg instanceof Medicine){
                medicine = (Medicine) arg;
                break;
            }
        }

        if (medicine != null){
            validateMedicine(medicine);
        }
    }
    private void validateMedicine(Medicine medicine) {
        if (medicine.getName() == null || medicine.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Medicine name cannot be empty");
        }
        if (medicine.getDosageAmount() == null || medicine.getDosageAmount() <= 0) {
            throw new IllegalArgumentException("Dosage amount must be positive");
        }
        if (medicine.getDosageUnit() == null || medicine.getDosageUnit().trim().isEmpty()) {
            throw new IllegalArgumentException("Dosage unit cannot be empty");
        }
    }
}
