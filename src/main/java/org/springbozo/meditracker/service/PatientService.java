package org.springbozo.meditracker.service;

import org.springbozo.meditracker.model.Patient;
import org.springbozo.meditracker.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    @Autowired
    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }


    public Optional<Patient> getPatientById(int id){
        return patientRepository.findById(id);
    }

    public Optional<Patient> getPatientByUserId(int userId) {
        return patientRepository.findByUserId(userId);
    }

    // overload to accept boxed Integer
    public Optional<Patient> getPatientByUserId(Integer userId) {
        if (userId == null) return Optional.empty();
        return patientRepository.findByUserId(userId);
    }

    public List<Patient> listPatients() {
        return patientRepository.findAll();
    }

    public boolean createNewPerson(Patient patient) {
        boolean isSaved = false;
        patient = patientRepository.save(patient);
        if (patient.getId() > 0)
        {
            isSaved = true;
        }
        return isSaved;
    }

    /**
     * Save or update a Patient for the given user. If a Patient already exists for the user
     * (unique user_id), update its editable fields; otherwise create a new Patient linked to the user.
     */
    public Patient saveOrUpdateForUser(Patient incoming, Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        Optional<Patient> existingOpt = patientRepository.findByUserId(userId);
        if (existingOpt.isPresent()) {
            Patient existing = existingOpt.get();
            // copy editable fields from incoming to existing
            existing.setName(incoming.getName() != null ? incoming.getName() : existing.getName());
            existing.setPhone(incoming.getPhone());
            existing.setAddress(incoming.getAddress());
            existing.setBloodType(incoming.getBloodType());
            existing.setAllergies(incoming.getAllergies());
            existing.setGender(incoming.getGender());
            existing.setMedicalHistory(incoming.getMedicalHistory());
            // keep existing.user unchanged
            return patientRepository.save(existing);
        } else {
            // ensure incoming.user is not null and points to correct user id (controller sets incoming.user)
            return patientRepository.save(incoming);
        }
    }

    /**
     * Delete the patient record associated with the given user id, if present.
     * Returns true if a record was found and deleted, false otherwise.
     */
    public boolean deleteByUserId(Integer userId) {
        if (userId == null) return false;
        Optional<Patient> existing = patientRepository.findByUserId(userId);
        if (existing.isPresent()) {
            patientRepository.delete(existing.get());
            return true;
        }
        return false;
    }
}
