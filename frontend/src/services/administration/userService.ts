import { api, apiService } from '../core/apiService';
import { User } from './types';
import { PageResponse } from '../core/types';
import { API_URL } from '../../config/apiConfig';

interface CurrentUser {
    id: number;
    username: string;
    email: string;
    roles: string[];
}

export const userService = {
    getCurrentUser: async (): Promise<CurrentUser> => {
        const response = await api.get<CurrentUser>(`${API_URL}/api/user`);
        return response.data;
    },

    getAllUsers: async (page: number = 0, size: number = 10, sortBy: string = 'username', direction: string = 'asc'): Promise<PageResponse<User>> => {
        const response = await api.get<PageResponse<User>>(`${API_URL}/api/users`, {
            params: { page, size, sortBy, direction }
        });
        return response.data;
    },

    searchUsers: async (searchTerm: string, page: number = 0, size: number = 10): Promise<PageResponse<User>> => {
        const response = await api.get<PageResponse<User>>(`${API_URL}/api/users`, {
            params: { searchTerm, page, size }
        });
        return response.data;
    },

    getUsersByStatus: async (enabled: boolean, page: number = 0, size: number = 10): Promise<PageResponse<User>> => {
        const response = await api.get<PageResponse<User>>(`${API_URL}/api/users`, {
            params: { enabled, page, size }
        });
        return response.data;
    },

    getLockedUsers: async (page: number = 0, size: number = 10, sortBy: string = 'username', direction: string = 'asc'): Promise<PageResponse<User>> => {
        const response = await api.get<PageResponse<User>>(`${API_URL}/api/users/locked`, {
            params: { page, size, sortBy, direction }
        });
        return response.data;
    },

    createUser: async (user: Omit<User, 'id'>): Promise<User> => {
        console.log('Creating user with data:', { ...user, password: '***' });
        const response = await api.post<User>(`${API_URL}/api/users`, user);
        return response.data;
    },

    updateUser: async (id: number, user: Partial<User>): Promise<User> => {
        const response = await api.put<User>(`${API_URL}/api/users/${id}`, user);
        return response.data;
    },

    deleteUser: async (id: number): Promise<void> => {
        await api.delete(`${API_URL}/api/users/${id}`);
    },

    changePassword: async (id: number, oldPassword: string, newPassword: string): Promise<void> => {
        await api.post(`${API_URL}/api/users/${id}/change-password`, null, {
            params: { oldPassword, newPassword }
        });
    },

    resetPassword: async (email: string): Promise<void> => {
        await api.post(`${API_URL}/api/users/reset-password`, null, {
            params: { email }
        });
    },

    unlockAccount: async (id: number): Promise<void> => {
        await api.post(`${API_URL}/api/users/${id}/unlock`);
    }
};
   