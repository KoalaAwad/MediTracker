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
  // Build query string manually to avoid any params serialization issues
  getAllUsers: (token: string, role?: string | null, only?: boolean) => {
    let url = "/admin/users";
    const parts: string[] = [];
    if (role) parts.push(`role=${encodeURIComponent(role)}`);
    if (only === true) parts.push(`only=true`);
    if (parts.length > 0) url += `?${parts.join("&")}`;
    return axiosClient.get<UsersResponse>(url, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
  },

  getAvailableRoles: (token: string) =>
    axiosClient.get<RolesResponse>("/admin/roles", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }),

  updateUserRoles: (token: string, data: UpdateRoleRequest) =>
    axiosClient.put(`/admin/users/${data.userId}/roles`,
      { roles: data.roles },
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    ),

  deleteUser: (token: string, userId: number) =>
    axiosClient.delete(`/admin/users/${userId}`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }),
};
