import axiosClient from "../lib/axiosClient";

export interface Medicine {
  id?: number;
  name: string;
  genericName?: string;
  manufacturer?: string;
  active?: boolean;
  openfda?: Record<string, string[]>; // FDA data arrays
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const medicineApi = {
  getAll: (token: string) =>
    axiosClient.get<Medicine[]>("/medicine", {
      headers: { Authorization: `Bearer ${token}` }
    }),

  getPaged: (token: string, page = 0, size = 20, q?: string) =>
    axiosClient.get<PagedResponse<Medicine>>(
      `/medicine/paged?page=${page}&size=${size}${q ? `&q=${encodeURIComponent(q)}` : ""}`,
      { headers: { Authorization: `Bearer ${token}` } }
    ),

  getById: (id: number, token: string) =>
    axiosClient.get<Medicine>(`/medicine/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    }),


  delete: (id: number, token: string) =>
    axiosClient.delete(`/medicine/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    }),

  importMedicines: (jsonData: any, token: string) =>
    axiosClient.post(`/medicine/import`, jsonData, {
      headers: { Authorization: `Bearer ${token}` }
    })
};
