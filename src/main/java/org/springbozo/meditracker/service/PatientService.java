package org.springbozo.meditracker.service;

import org.springbozo.meditracker.model.Medicine;
import org.springbozo.meditracker.model.Patient;
import org.springbozo.meditracker.repository.MedicineRepository;
import org.springbozo.meditracker.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final MedicineRepository medicineRepository;

    @Autowired
    public PatientService(PatientRepository patientRepository, MedicineRepository medicineRepository) {
        this.patientRepository = patientRepository;
        this.medicineRepository = medicineRepository;
    }

    public String addPrescription(int patientId, Medicine medicine){
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        medicine.setPatient(patient);
        patient.getPrescribedMedicine().add(medicine);
        medicineRepository.save(medicine);
        return "Prescribed Medicine: " + medicine.getName();
    }

    public Optional<Patient> getPatientById(int id){
        return patientRepository.findById(id);
    }

    public List<Patient> listPatients() {
        return patientRepository.findAll();
    }

    public boolean createNewPerson(Patient patient) {
        boolean isSaved = false;
        patient = patientRepository.save(patient);
        if (null != patient && patient.getId() > 0)
        {
            isSaved = true;
        }
        return isSaved;
    }
}
