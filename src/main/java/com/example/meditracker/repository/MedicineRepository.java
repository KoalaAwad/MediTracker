package com.example.meditracker.repository;

import com.example.meditracker.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
 public interface MedicineRepository extends JpaRepository<Medicine, Long>{

    List<Medicine> findByNameContainingIgnoreCase(String name);

    // Find medicines by dosage unit
    List<Medicine> findByDosageUnit(String dosageUnit);

    // Custom query to find medicines ordered by name
    // @Query("SELECT m FROM Medicine m ORDER BY m.name ASC")
    List<Medicine> findAllByOrderByNameAsc();
}
