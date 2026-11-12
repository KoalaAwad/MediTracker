import axiosClient from "../lib/axiosClient";

export type DoseStatus = "TAKEN" | "SKIPPED" | "NOW" | "UPCOMING" | "PAST";

export interface TodayDose {
  prescriptionId: number;
  medicineId: number;
  medicineName: string; // includes strength, e.g. "Prograf 1 mg"
  dosage: string; // "1 capsule"
  notes: string | null;
  scheduledAt: string; // ISO instant
  localTime: string; // "08:00" in the prescription's time zone
  status: DoseStatus;
  statusAt: string | null;
}

export const doseApi = {
  today: (token: string) =>
    axiosClient.get<TodayDose[]>("/doses/today", {
      headers: { Authorization: `Bearer ${token}` },
    }),

  // PENDING clears a previous taken/skipped
  log: (token: string, prescriptionId: number, scheduledAt: string, status: "TAKEN" | "SKIPPED" | "PENDING") =>
    axiosClient.post<{ status: DoseStatus }>(
      "/doses",
      { prescriptionId: String(prescriptionId), scheduledAt, status },
      { headers: { Authorization: `Bearer ${token}` } }
    ),
};
