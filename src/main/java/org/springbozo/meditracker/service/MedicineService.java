package org.springbozo.meditracker.service;

import org.springbozo.meditracker.model.Medicine;
import org.springbozo.meditracker.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MedicineService {

    private final MedicineRepository medicineRepository;

    @Autowired
    public MedicineService(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
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
     * Paginated list, optional search by name or generic name
     */
    public Page<Medicine> listMedicinesPaged(int page, int size, String search) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        if (search == null || search.trim().isEmpty()) {
            return medicineRepository.findByActiveTrue(pageable);
        }
        String q = search.trim();
        // Use repo method to search by name or generic name
        return medicineRepository.findByActiveTrueAndNameContainingIgnoreCaseOrActiveTrueAndGenericNameContainingIgnoreCase(q, pageable, q);
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

    /**
     * Check if medicine exists by name (case-insensitive) - used for deduplication
     */
    public boolean existsByName(String name) {
        return medicineRepository.existsByNameIgnoreCase(name);
    }

    /**
     * Check if medicine exists by name AND manufacturer - more precise deduplication
     * Allows same drug name with different manufacturers
     */
    public boolean existsByNameAndManufacturer(String name, String manufacturer) {
        if (manufacturer == null || manufacturer.isBlank()) {
            return medicineRepository.existsByNameIgnoreCaseAndManufacturerIsNull(name);
        }
        return medicineRepository.existsByNameIgnoreCaseAndManufacturerIgnoreCase(name, manufacturer);
    }
}