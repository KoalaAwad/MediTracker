package org.springbozo.meditracker.DAO;

import java.util.List;

public class ProfileDto {
    private int userId;
    private String email;
    private String name;
    private List<String> roles;
    private PatientProfileDto patientProfile;
    private DoctorProfileDto doctorProfile;

    // Constructors
    public ProfileDto() {}

    public ProfileDto(int userId, String email, String name, List<String> roles) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.roles = roles;
    }

    // Getters & Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public PatientProfileDto getPatientProfile() {
        return patientProfile;
    }

    public void setPatientProfile(PatientProfileDto patientProfile) {
        this.patientProfile = patientProfile;
    }

    public DoctorProfileDto getDoctorProfile() {
        return doctorProfile;
    }

    public void setDoctorProfile(DoctorProfileDto doctorProfile) {
        this.doctorProfile = doctorProfile;
    }
}

