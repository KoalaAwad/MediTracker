package com.example.meditracker.repository;

import com.example.meditracker.entity.MedicineSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


@Repository
public interface MedicineScheduleRepository extends JpaRepository<MedicineSchedule, Long>{

    // this will be a list of all schedules
    List<MedicineSchedule> findByMedicineIdAndIsActiveTrue(Long medicineId);

    @Query("SELECT ms FROM MedicineSchedule ms WHERE ms.isActive = true AND " +
            "ms.startDate <= :date AND (ms.endDate IS NULL OR ms.endDate >= :date)")
    List<MedicineSchedule> findSchedulesForDate(@Param("date") LocalDate date);


    @Query("SELECT ms FROM MedicineSchedule ms WHERE ms.isActive = true AND " +
            "ms.timeOfDay BETWEEN :startTime AND :endTime AND " +
            "ms.startDate <= :date AND (ms.endDate IS NULL OR ms.endDate >= :date)")
    List<MedicineSchedule> findSchedulesForTimeRange(@Param("startTime") LocalTime startTime,
                                                     @Param("endTime") LocalTime endTime,
                                                     @Param("date") LocalDate date);

    List<MedicineSchedule> findByIsActiveTrueOrderByTimeOfDay();
}

