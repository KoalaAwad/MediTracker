package org.springbozo.meditracker.service;

import org.springbozo.meditracker.model.Medicine;
import org.springbozo.meditracker.repository.MedicineRepository;
import org.springbozo.meditracker.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;

    @Autowired
    public MedicineService(MedicineRepository medicineRepository, PrescriptionRepository prescriptionRepository) {
        this.medicineRepository = medicineRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    /**
     * Get medicine by ID - only returns if active
     */
    public Optional<Medicine> getMedicineById(int id) {
        return medicineRepository.findByIdAndActiveTrue(id);
    }

    /**
     * List all active medicines only
     */
    public List<Medicine> listMedicines() {
        return medicineRepository.findByActiveTrue();
    }

    /**
     * Save/create medicine - always set active = true for new medicines
     */
    public Medicine save(Medicine medicine) {
        // Ensure new medicines are active by default
        if (medicine.getId() == 0) {
            medicine.setActive(true);
        }
        return medicineRepository.save(medicine);
    }

    /**
     * Soft delete - mark medicine as inactive instead of actually deleting
     * This preserves prescription history while hiding medicine from normal listings
     */
    public boolean deleteMedicine(int id) {
        Optional<Medicine> medicineOpt = medicineRepository.findById(id);
        if (medicineOpt.isEmpty()) {
            return false; // Medicine doesn't exist
        }

        Medicine medicine = medicineOpt.get();

        // Soft delete: just mark as inactive
        medicine.setActive(false);
        medicineRepository.save(medicine);

        return true;
    }

    /**
     * Get medicine by ID regardless of active status
     * Used for prescription display so patients can see their prescriptions even with discontinued medicines
     */
    public Optional<Medicine> getMedicineByIdIncludingInactive(int id) {
        return medicineRepository.findById(id);
    }
}