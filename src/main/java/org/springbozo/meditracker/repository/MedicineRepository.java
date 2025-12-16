package org.springbozo.meditracker.repository;


import org.springbozo.meditracker.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Integer> {

    // Find only active medicines
    List<Medicine> findByActiveTrue();

    // Find by ID only if active
    Optional<Medicine> findByIdAndActiveTrue(int id);
}
