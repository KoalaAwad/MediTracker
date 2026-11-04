package org.springbozo.meditracker.repository;

import org.springbozo.meditracker.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Integer> {
    List<Prescription> findByPatientId(Integer patientId);

    // Find only active prescriptions for a patient
    List<Prescription> findByPatientIdAndActiveTrue(Integer patientId);

    // Find all prescriptions (including inactive) for a patient
    List<Prescription> findByPatientIdOrderByCreatedAtDesc(Integer patientId);

    // Check if any prescriptions exist for a medicine (using medicine.id)
    boolean existsByMedicine_Id(int medicineId);

    // Count prescriptions using a medicine
    long countByMedicine_Id(int medicineId);

    // everything the reminder job needs, in one query
    @Query("select distinct p from Prescription p join fetch p.schedule join fetch p.medicine join fetch p.patient pt join fetch pt.user where p.active = true")
    List<Prescription> findActiveWithSchedule();
}

