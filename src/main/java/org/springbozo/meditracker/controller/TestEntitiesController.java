//package org.springbozo.meditracker.controller;
//
//import org.springbozo.meditracker.model.*;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//
//import java.time.DayOfWeek;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.List;
//import java.util.Set;
//
//@Controller
//@RequestMapping
//public class TestEntitiesController {
//
//    @GetMapping("/profile/test/user")
//    public String testUser(Model model) {
//        User user = new User();
//        user.setId(1);
//        user.setName("Display Name");
//        user.setUsername("jdoe");
//        user.setEmail("jdoe@example.com");
//        user.setPassword("$2a$hash");
//        user.setCreatedAt(LocalDateTime.now().minusDays(10));
//        user.setUpdatedAt(LocalDateTime.now());
//
//        Role role = new Role();
//        role.setId(2);
//        role.setRoleName("PATIENT");
//        role.setDescription("Standard patient role");
//        user.setRoles(Set.of(role));
//
//        model.addAttribute("user", user);
//        return "profile/user-entity-test";
//    }
//
//    @GetMapping("/patient/test")
//    public String testPatient(Model model) {
//        // User
//        User user = new User();
//        user.setId(100);
//        user.setName("Patient Display");
//        user.setUsername("patient.one");
//        user.setEmail("patient.one@example.com");
//        user.setPassword("$2a$hash");
//
//        // Doctor
//        Doctor doc = new Doctor();
//        doc.setId(200);
//        doc.setFirstName("Gregory");
//        doc.setLastName("House");
//        doc.setSpecialization("Diagnostics");
//        doc.setLicenseNumber("LIC-555");
//        doc.setPhone("+1 555-0100");
//        doc.setClinicAddress("221B Clinic St");
//
//        // Patient
//        Patient patient = new Patient();
//        patient.setId(300);
//        patient.setFirstName("Jane");
//        patient.setLastName("Doe");
//        patient.setDateOfBirth(LocalDate.of(1990, 5, 15));
//        patient.setGender("Female");
//        patient.setPhone("+1 555-0200");
//        patient.setAddress("123 Main St");
//        patient.setBloodType("O+");
//        patient.setAllergies("Penicillin");
//        patient.setMedicalHistory("No major conditions.");
//        patient.setUser(user);
//        patient.setDoctors(Set.of(doc));
//
//        // Medicine
//        Medicine med = new Medicine();
//        med.setId(400);
//        med.setName("Amoxicillin");
//        med.setGenericName("Amoxicillin");
//        med.setManufacturer("Pharma Inc.");
//        med.setDosageForm("Tablet");
//        med.setStrength("500 mg");
//        med.setDescription("Antibiotic");
//        med.setSideEffects("Nausea, rash");
//        med.setContraindications("Allergy to penicillin");
//
//        // Prescription
//        Prescription p = new Prescription();
//        p.setId(500);
//        p.setPatient(patient);
//        p.setDoctor(doc);
//        p.setMedicine(med);
//        p.setStartDate(LocalDateTime.now().minusDays(5));
//        p.setEndDate(LocalDateTime.now().plusDays(5));
//        p.setRoute("Oral");
//        p.setFrequencyType(FrequencyType.INTERVAL);
//        p.setIntervalValue(8);
//        p.setTimeOfDay(LocalTime.of(8, 0));
//        p.setDaysOfWeek(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));
//        p.setDayOfWeekPattern("[\"MONDAY\",\"WEDNESDAY\",\"FRIDAY\"]");
//        p.setNotes("Take with food.");
//
//        PrescriptionDosage d1 = new PrescriptionDosage();
//        d1.setId(1);
//        d1.setPrescription(p);
//        d1.setDosageAmount("1 tablet");
//        d1.setTimeOfDay(LocalTime.of(8, 0));
//        d1.setCondition("With breakfast");
//        d1.setDosageSequence(1);
//
//        PrescriptionDosage d2 = new PrescriptionDosage();
//        d2.setId(2);
//        d2.setPrescription(p);
//        d2.setDosageAmount("1 tablet");
//        d2.setTimeOfDay(LocalTime.of(20, 0));
//        d2.setCondition("With dinner");
//        d2.setDosageSequence(2);
//
//        p.setDosages(List.of(d1, d2));
//
//        model.addAttribute("patient", patient);
//        model.addAttribute("prescription", p);
//        return "patient/patient-entity-test";
//    }
//}
//
