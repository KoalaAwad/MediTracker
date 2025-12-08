import axiosClient from "../lib/axiosClient";

export interface PatientProfileDto {
  id: number;
  name: string;
  gender?: string;
  dateOfBirth?: string;
  phone?: string;
  address?: string;
  bloodType?: string;
  allergies?: string;
  medicalHistory?: string;
  active: boolean;
}

export interface DoctorProfileDto {
  id: number;
  firstName?: string;
  lastName?: string;
  specialization?: string;
  licenseNumber?: string;
  phone?: string;
  clinicAddress?: string;
  active: boolean;
}

export interface ProfileDto {
  userId: number;
  email: string;
  name: string;
  roles: string[];
  patientProfile?: PatientProfileDto;
  doctorProfile?: DoctorProfileDto;
}

export const profileApi = {
  getProfile: (token: string) =>
    axiosClient.get<ProfileDto>("/auth/profile", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }),

  updatePatientProfile: (token: string, dto: PatientProfileDto) =>
    axiosClient.put("/auth/profile/patient", dto, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }),

  updateDoctorProfile: (token: string, dto: DoctorProfileDto) =>
    axiosClient.put("/auth/profile/doctor", dto, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }),
};

