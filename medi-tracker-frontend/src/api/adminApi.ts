import axiosClient from "../lib/axiosClient";

export interface UserDto {
  userId: number;
  name: string;
  email: string;
  role: string;
  createdAt: string;
}

export interface UsersResponse {
  users: UserDto[];
}

export const adminApi = {
  getAllUsers: (token: string) =>
    axiosClient.get<UsersResponse>("/admin/users", {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
};

