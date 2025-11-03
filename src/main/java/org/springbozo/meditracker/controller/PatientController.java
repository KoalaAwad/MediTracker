package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.model.Patient;
import org.springbozo.meditracker.service.PatientService;
import org.springframework.boot.Banner;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;


    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public String listPatients(Model model) {
        model.addAttribute("patients", patientService.listPatients());
        return "patient-list";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')")
    public String viewPatient(@PathVariable int id, Model model) {
        var patientOpt = patientService.getPatientById(id);
        if (patientOpt.isEmpty()) {
            return "redirect:/public/home";
        }
        model.addAttribute("patient", patientOpt.get());
        return "patient-view";
    }

    @GetMapping("/new")
    public String getAddPatient(Model model){
        model.addAttribute("patient", new Patient());
        return "patient-form";
    }

    @PostMapping("/add")
    public String addPatient(@ModelAttribute("patient") Patient patient, Model model){
        boolean isSaved = patientService.createNewPerson(patient);
        if(isSaved){
            return "redirect:/home";
        }
        return  "redirect:/home";
    }
}
