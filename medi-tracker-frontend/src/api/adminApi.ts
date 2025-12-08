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

export interface RolesResponse {
  roles: string[];
}

export interface UpdateRoleRequest {
  userId: number;
  roles: string[];
}

export const adminApi = {
  getAllUsers: (token: string) =>
    axiosClient.get<UsersResponse>("/admin/users", {
      headers: {
        Authorization: `Bearer ${token}`
      }
    }),

  getAvailableRoles: (token: string) =>
    axiosClient.get<RolesResponse>("/admin/roles", {
      headers: {
        Authorization: `Bearer ${token}`
      }
    }),

  updateUserRoles: (token: string, data: UpdateRoleRequest) =>
    axiosClient.put(`/admin/users/${data.userId}/roles`,
      { roles: data.roles },
      {
        headers: {
          Authorization: `Bearer ${token}`
        }
      }
    ),

  deleteUser: (token: string, userId: number) =>
    axiosClient.delete(`/admin/users/${userId}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
};

