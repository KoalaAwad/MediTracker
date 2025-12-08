import axiosClient from "../lib/axiosClient";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  roles: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface RegisterResponse {
  message: string;
  userId?: number;
}

export interface User {
  email: string;
  name: string;
  role: string;
  userId: number;
}

export const authApi = {
  login: (data: LoginRequest) =>
    axiosClient.post<LoginResponse>("/auth/login", {
      email: data.email.trim().toLowerCase(),
      password: data.password
    }),

  register: (data: RegisterRequest) =>
    axiosClient.post<RegisterResponse>("/auth/register", {
      name: data.name,
      email: data.email.trim().toLowerCase(),
      password: data.password
    }),

  getCurrentUser: (token: string) =>
    axiosClient.get<User>("/auth/me", {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
};