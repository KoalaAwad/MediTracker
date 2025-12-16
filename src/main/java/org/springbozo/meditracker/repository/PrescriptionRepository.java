package org.springbozo.meditracker.repository;

import org.springbozo.meditracker.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Integer> {
    List<Prescription> findByPatientId(Integer patientId);

    // Check if any prescriptions exist for a medicine (using medicine.id)
    boolean existsByMedicine_Id(int medicineId);

    // Count prescriptions using a medicine
    long countByMedicine_Id(int medicineId);
}

