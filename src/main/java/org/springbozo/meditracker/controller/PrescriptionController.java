package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.model.Prescription;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.service.PatientService;
import org.springbozo.meditracker.service.UserService;
import org.springbozo.meditracker.repository.PrescriptionRepository;
import org.springbozo.meditracker.repository.MedicineRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/prescriptions")
public class PrescriptionController {

    private final PrescriptionRepository prescriptionRepository;
    private final UserService userService;
    private final PatientService patientService;
    private final MedicineRepository medicineRepository;

    public PrescriptionController(PrescriptionRepository prescriptionRepository, UserService userService, PatientService patientService, MedicineRepository medicineRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.userService = userService;
        this.patientService = patientService;
        this.medicineRepository = medicineRepository;
    }

    @GetMapping("/list")
    public String listPatientPrescriptions(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";
        Optional<User> userOpt = userService.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) return "redirect:/login";
        User user = userOpt.get();
        // get patient id
        var patientOpt = patientService.getPatientByUserId(Integer.valueOf(user.getId()));
        if (patientOpt.isEmpty()) {
            model.addAttribute("prescriptions", List.of());
            return "prescriptions/prescription-list";
        }
        var prescriptions = prescriptionRepository.findByPatientId(patientOpt.get().getId());
        model.addAttribute("prescriptions", prescriptions);
        return "prescriptions/prescription-list";
    }

    @GetMapping("/new")
    public String newPrescriptionForm(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";
        model.addAttribute("prescription", new Prescription());
        // provide list of medicines for selection
        var meds = medicineRepository.findAll();
        model.addAttribute("medicines", meds);
        return "prescriptions/prescription-form";
    }

    @PostMapping("/new")
    public String savePrescription(@ModelAttribute("prescription") Prescription prescription,
                                   @RequestParam(value = "dayTimes", required = false) List<String> dayTimes,
                                   @RequestParam(value = "medicineId", required = false) Integer medicineId,
                                   Authentication authentication,
                                   Model model) {
        if (authentication == null) return "redirect:/login";
        Optional<User> userOpt = userService.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) return "redirect:/login";
        var user = userOpt.get();
        var patientOpt = patientService.getPatientByUserId(Integer.valueOf(user.getId()));
        if (patientOpt.isEmpty()) return "redirect:/profile";
        prescription.setPatient(patientOpt.get());
        // server-side validation
        var errors = new java.util.HashMap<String,String>();

        if (medicineId == null) {
            errors.put("medicine", "Please select a medicine or add one first.");
        } else {
            var medOpt = medicineRepository.findById(medicineId);
            if (medOpt.isEmpty()) {
                errors.put("medicine", "Selected medicine not found.");
            } else {
                prescription.setMedicine(medOpt.get());
            }
        }

        // week days stored in prescription.weekDays as comma separated; validate at least one
        if (prescription.getWeekDays() == null || prescription.getWeekDays().trim().isEmpty()) {
            errors.put("weekDays", "Please select at least one day or choose Daily.");
        }

        // required fields: dosageAmount (non-zero), dosageUnit
        if (prescription.getDosageAmount() == 0) {
            errors.put("dosageAmount", "Dosage amount is required and must be non-zero.");
        }
        if (prescription.getDosageUnit() == null || prescription.getDosageUnit().trim().isEmpty()) {
            errors.put("dosageUnit", "Dosage unit is required.");
        }
        // if startDate is missing, default to today
        if (prescription.getStartDate() == null) {
            prescription.setStartDate(java.time.LocalDate.now());
        }

        // parse dayTimes list into LocalTime
        if (dayTimes != null && !dayTimes.isEmpty()) {
            List<LocalTime> times = new ArrayList<>();
            for (String t : dayTimes) {
                try {
                    times.add(LocalTime.parse(t.trim()));
                } catch (Exception ex) {
                    // invalid time format
                    errors.put("dayTimes", "One or more times have invalid format. Use HH:mm.");
                }
            }
            prescription.setDayTimes(times);
        }

        if (!errors.isEmpty()) {
            // reload form with errors and medicines
            model.addAttribute("errors", errors);
            model.addAttribute("prescription", prescription);
            model.addAttribute("medicines", medicineRepository.findAll());
            return "prescriptions/prescription-form";
        }

        prescriptionRepository.save(prescription);
        return "redirect:/prescriptions/list";
    }
}
