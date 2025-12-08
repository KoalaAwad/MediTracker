import axiosClient from "../lib/axiosClient";

export interface Medicine {
  id?: number;
  name: string;
  genericName?: string;
  manufacturer?: string;
  dosageForm?: string;
  strength?: string;
  description?: string;
  sideEffects?: string;
  contraindications?: string;
}

export const medicineApi = {
  getAll: (token: string) =>
    axiosClient.get<Medicine[]>("/medicine", {
      headers: { Authorization: `Bearer ${token}` }
    }),

  getById: (id: number, token: string) =>
    axiosClient.get<Medicine>(`/medicine/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    }),

  create: (medicine: Medicine, token: string) =>
    axiosClient.post<Medicine>("/medicine", medicine, {
      headers: { Authorization: `Bearer ${token}` }
    }),

  update: (id: number, medicine: Medicine, token: string) =>
    axiosClient.put<Medicine>(`/medicine/${id}`, medicine, {
      headers: { Authorization: `Bearer ${token}` }
    }),

  delete: (id: number, token: string) =>
    axiosClient.delete(`/medicine/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
};

