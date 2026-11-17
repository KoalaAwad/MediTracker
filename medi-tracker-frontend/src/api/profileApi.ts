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

export interface ProfileDto {
  userId: number;
  email: string;
  name: string;
  roles: string[];
  patientProfile?: PatientProfileDto;
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
};
