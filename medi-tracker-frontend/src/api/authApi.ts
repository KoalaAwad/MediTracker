import axiosClient from "../lib/axiosClient";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

export const authApi = {
  login: (data: LoginRequest) =>
    axiosClient.post<LoginResponse>("/auth/login", data),
};
