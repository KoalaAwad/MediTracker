package org.springbozo.meditracker.service;

import org.springbozo.meditracker.model.Medicine;
import org.springbozo.meditracker.model.Patient;
import org.springbozo.meditracker.model.Prescription;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.model.embedded.Dosage;
import org.springbozo.meditracker.model.embedded.ScheduleEntry;
import org.springbozo.meditracker.model.enums.DosageUnit;
import org.springbozo.meditracker.repository.MedicineRepository;
import org.springbozo.meditracker.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository;
    private final UserService userService;

    @Autowired
    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               MedicineRepository medicineRepository,
                               UserService userService) {
        this.prescriptionRepository = prescriptionRepository;
        this.medicineRepository = medicineRepository;
        this.userService = userService;
    }

    /**
     * Create prescription for the authenticated user (must be a patient)
     */
    public Prescription createPrescriptionForUser(String email, PrescriptionCreateDto dto) {
        // Get user and their patient profile
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Patient patient = user.getPatient();
        if (patient == null) {
            throw new IllegalStateException("User does not have a patient profile");
        }

        // Validate medicine exists and is active
        Medicine medicine = medicineRepository.findByIdAndActiveTrue(dto.getMedicineId())
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found or inactive"));

        // Build prescription
        Prescription prescription = new Prescription();
        prescription.setPatient(patient);
        prescription.setMedicine(medicine);

        // Set dosage
        Dosage dosage = new Dosage();
        dosage.setAmount(dto.getDosage().getAmount());
        dosage.setUnit(DosageUnit.valueOf(dto.getDosage().getUnit()));
        prescription.setDosage(dosage);

        // Set dates
        prescription.setStartDate(dto.getStartDate());
        prescription.setEndDate(dto.getEndDate()); // Can be null for ongoing

        // Set timezone
        prescription.setTimeZone(dto.getTimeZone());

        // Set schedule
        Set<ScheduleEntry> scheduleSet = new HashSet<>();
        for (ScheduleEntryDto entryDto : dto.getSchedule()) {
            ScheduleEntry entry = new ScheduleEntry();
            entry.setDayOfWeek(DayOfWeek.valueOf(entryDto.getDayOfWeek()));
            entry.setTimeOfDay(LocalTime.parse(entryDto.getTimeOfDay()));
            scheduleSet.add(entry);
        }
        prescription.setSchedule(scheduleSet);

        return prescriptionRepository.save(prescription);
    }

    /**
     * Get all prescriptions for a user (patient)
     */
    public List<Prescription> getPrescriptionsForUser(String email) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Patient patient = user.getPatient();
        if (patient == null) {
            throw new IllegalStateException("User does not have a patient profile");
        }

        return prescriptionRepository.findByPatientId(patient.getId());
    }

    // DTOs for request/response
    public static class PrescriptionCreateDto {
        private int medicineId;
        private DosageDto dosage;
        private LocalDate startDate;
        private LocalDate endDate; // null = ongoing
        private String timeZone;
        private List<ScheduleEntryDto> schedule;

        // Getters and setters
        public int getMedicineId() { return medicineId; }
        public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

        public DosageDto getDosage() { return dosage; }
        public void setDosage(DosageDto dosage) { this.dosage = dosage; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

        public String getTimeZone() { return timeZone; }
        public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

        public List<ScheduleEntryDto> getSchedule() { return schedule; }
        public void setSchedule(List<ScheduleEntryDto> schedule) { this.schedule = schedule; }
    }

    public static class DosageDto {
        private java.math.BigDecimal amount;
        private String unit;

        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }

    public static class ScheduleEntryDto {
        private String dayOfWeek;
        private String timeOfDay;

        public String getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

        public String getTimeOfDay() { return timeOfDay; }
        public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }
    }

    public static class PrescriptionResponseDto {
        private int id;
        private int medicineId;
        private String medicineName;
        private java.math.BigDecimal dosageAmount;
        private String dosageUnit;
        private String startDate;
        private String endDate; // null if ongoing
        private String timeZone;
        private List<ScheduleEntryDto> schedule;

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getMedicineId() { return medicineId; }
        public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

        public String getMedicineName() { return medicineName; }
        public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

        public java.math.BigDecimal getDosageAmount() { return dosageAmount; }
        public void setDosageAmount(java.math.BigDecimal dosageAmount) { this.dosageAmount = dosageAmount; }

        public String getDosageUnit() { return dosageUnit; }
        public void setDosageUnit(String dosageUnit) { this.dosageUnit = dosageUnit; }

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }

        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }

        public String getTimeZone() { return timeZone; }
        public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

        public List<ScheduleEntryDto> getSchedule() { return schedule; }
        public void setSchedule(List<ScheduleEntryDto> schedule) { this.schedule = schedule; }
    }
}

