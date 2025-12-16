import axiosClient from "../lib/axiosClient";

export interface ScheduleEntryDto {
  dayOfWeek: string; // e.g., "MONDAY"
  timeOfDay: string; // e.g., "08:00"
}

export interface PrescriptionRequestDto {
  medicineId: number;
  dosage: { amount: number; unit: string };
  startDate: string; // YYYY-MM-DD
  endDate?: string;  // YYYY-MM-DD
  timeZone: string;  // IANA zone, e.g., "Europe/London"
  schedule: ScheduleEntryDto[];
}

export interface PrescriptionDto {
  id: number;
  medicineId: number;
  medicineName: string;
  dosageAmount: number;
  dosageUnit: string;
  startDate: string;
  endDate?: string;
  timeZone: string;
  schedule: ScheduleEntryDto[];
}

export const prescriptionApi = {
  createForMe: (token: string, dto: PrescriptionRequestDto) =>
    axiosClient.post("/prescriptions", dto, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  listMine: (token: string) =>
    axiosClient.get<PrescriptionDto[]>("/prescriptions/me", {
      headers: { Authorization: `Bearer ${token}` },
    }),
};

