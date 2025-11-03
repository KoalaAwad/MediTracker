package org.springbozo.meditracker.service;

import org.springbozo.meditracker.model.Medicine;
import org.springbozo.meditracker.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public Optional<Medicine> getMedicineById(int id) {
        // Use a fetch join to load schedules eagerly
        return medicineRepository.findById(id);
    }

    public List<Medicine> listMedicines() {
        // Use a fetch join to load schedules eagerly
        return medicineRepository.findAll();
    }
}