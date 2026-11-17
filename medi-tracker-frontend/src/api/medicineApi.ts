import axiosClient from "../lib/axiosClient";

export interface Medicine {
  id?: number;
  name: string;
  genericName?: string;
  manufacturer?: string;
  active?: boolean;
  productNdc?: string;
  strength?: string; // e.g. "1 mg" or "5 mg / 10 mg"
  dosageForm?: string;
  route?: string;
  productType?: string;
  splSetId?: string;
  openfda?: Record<string, string[]>; // FDA data arrays
}

export interface PillAppearance {
  color?: string;
  shape?: string;
  size?: string;
  imprint?: string;
  score?: string;
}

// From the official DailyMed label; available is false when the label can't be fetched
export interface MedicineLabel {
  available: boolean;
  images?: string[];
  appearance?: PillAppearance | null;
  dailyMedUrl?: string;
}

export interface SyncStatus {
  running: boolean;
  startedAt?: string;
  finishedAt?: string;
  imported: number;
  error?: string;
  total: number;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const medicineApi = {
  getPaged: (token: string, page = 0, size = 20, q?: string) =>
    axiosClient.get<PagedResponse<Medicine>>(
      `/medicine/paged?page=${page}&size=${size}${q ? `&q=${encodeURIComponent(q)}` : ""}`,
      { headers: { Authorization: `Bearer ${token}` } }
    ),

  getById: (id: number, token: string) =>
    axiosClient.get<Medicine>(`/medicine/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    }),

  getLabel: (id: number, token: string) =>
    axiosClient.get<MedicineLabel>(`/medicine/${id}/label`, {
      headers: { Authorization: `Bearer ${token}` }
    }),

  delete: (id: number, token: string) =>
    axiosClient.delete(`/medicine/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    }),

  startSync: (token: string) =>
    axiosClient.post(`/medicine/sync`, null, {
      headers: { Authorization: `Bearer ${token}` }
    }),

  getSyncStatus: (token: string) =>
    axiosClient.get<SyncStatus>(`/medicine/sync/status`, {
      headers: { Authorization: `Bearer ${token}` }
    }),
};
