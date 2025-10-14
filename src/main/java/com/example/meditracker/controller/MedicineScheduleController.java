package com.example.meditracker.controller;

import com.example.meditracker.entity.MedicineSchedule;
import com.example.meditracker.repository.MedicineScheduleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines-schedule")
public class MedicineScheduleController {

    private final MedicineScheduleRepository repository;

    public MedicineScheduleController(MedicineScheduleRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<MedicineSchedule> getAllSchedules() {
        return repository.findAll();
    }

    @PostMapping
    public MedicineSchedule createSchedule(@RequestBody MedicineSchedule schedule) {
        return repository.save(schedule);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicineSchedule> getScheduleById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicineSchedule> updateSchedule(@PathVariable Long id,
                                                           @RequestBody MedicineSchedule schedule) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setMedicine(schedule.getMedicine());
                    existing.setTimeOfDay(schedule.getTimeOfDay());
                    existing.setStartDate(schedule.getStartDate());
                    existing.setEndDate(schedule.getEndDate());
                    existing.setIsActive(schedule.getIsActive());
                    repository.save(existing);
                    return ResponseEntity.ok(existing);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}
