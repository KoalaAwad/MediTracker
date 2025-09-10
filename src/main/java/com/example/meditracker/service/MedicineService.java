package com.example.meditracker.service;

import com.example.meditracker.aspect.annotation.ValidateMedicine;
import com.example.meditracker.entity.Medicine;
import com.example.meditracker.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MedicineService {

    @Autowired
    private MedicineRepository medicineRepository;

    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    public Optional<Medicine> getMedicineById(Long id) {
        return medicineRepository.findById(id);
    }

    @ValidateMedicine
    public Medicine createMedicine(Medicine medicine) {
//        validateMedicine(medicine);
        return medicineRepository.save(medicine);
    }

    @ValidateMedicine
    public Medicine updateMedicine(Long id, Medicine updatedMedicine) {
        return medicineRepository.findById(id)
                .map(medicine -> {
                    medicine.setName(updatedMedicine.getName());
                    medicine.setDescription(updatedMedicine.getDescription());
                    medicine.setDosageAmount(updatedMedicine.getDosageAmount());
                    medicine.setDosageUnit(updatedMedicine.getDosageUnit());
                    medicine.setUpdatedAt(LocalDateTime.now());
                    return medicineRepository.save(medicine);
                })
                .orElseThrow(() -> new RuntimeException("Medicine not found with id: " + id));
    }

    public void deleteMedicine(Long id) {
        if (!medicineRepository.existsById(id)) {
            throw new RuntimeException("Medicine not found with id: " + id);
        }
        medicineRepository.deleteById(id);
    }

    public List<Medicine> searchMedicinesByName(String name) {
        return medicineRepository.findByNameContainingIgnoreCase(name);
    }

//    private void validateMedicine(Medicine medicine) {
//        if (medicine.getName() == null || medicine.getName().trim().isEmpty()) {
//            throw new IllegalArgumentException("Medicine name cannot be empty");
//        }
//        if (medicine.getDosageAmount() <= 0) {
//            throw new IllegalArgumentException("Dosage amount must be positive");
//        }
//        if (medicine.getDosageUnit() == null || medicine.getDosageUnit().trim().isEmpty()) {
//            throw new IllegalArgumentException("Dosage unit cannot be empty");
//
//        }
//    }
}
