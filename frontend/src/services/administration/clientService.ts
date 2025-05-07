import { api } from '../core/apiService';
import { handleApiError } from '../../utils/errorHandler';
import { createPaginationParams, PaginationParams } from '../../utils/paginationUtils';
import { Client, ClientInput, Interface } from './types';
import { PageResponse } from '../core/types';
import { setClientContext } from '../../utils/clientContext';

interface ClientResponse {
    content: Client[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

interface ClientOnboardingData {
    name: string;
    description?: string;
    active?: boolean;
}

export const clientService = {
    getAllClients: async (params: PaginationParams): Promise<ClientResponse> => {
        try {
            const response = await api.get<ClientResponse>('/api/clients', { params: createPaginationParams(params) });
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    getClientById: async (id: number): Promise<Client> => {
        try {
            const response = await api.get<Client>(`/api/clients/${id}`);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    createClient: async (clientData: ClientInput): Promise<Client> => {
        try {
            const response = await api.post<Client>('/api/clients', clientData);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    updateClient: async (id: number, clientData: Partial<ClientInput>): Promise<Client> => {
        try {
            const response = await api.put<Client>(`/api/clients/${id}`, clientData);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    deleteClient: async (id: number): Promise<void> => {
        try {
            await api.delete(`/api/clients/${id}`);
        } catch (error) {
            throw handleApiError(error);
        }
    },

    getClientInterfaces: async (clientId: number): Promise<Interface[]> => {
        try {
            const response = await api.get<Interface[]>(`/api/clients/${clientId}/interfaces`);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    onboardNewClient: async (clientData: ClientInput): Promise<Client> => {
        try {
            const response = await api.post<Client>('/api/clients/onboarding/new', clientData);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    cloneClientConfiguration: async (sourceClientId: number, newClientData: ClientInput): Promise<Client> => {
        try {
            const response = await api.post<Client>(`/api/clients/onboarding/clone/${sourceClientId}`, newClientData);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    searchClients: async (
        searchTerm: string,
        page = 0,
        size = 10,
        sortBy = 'name',
        direction = 'asc'
    ): Promise<PageResponse<Client>> => {
        const params = new URLSearchParams({
            search: searchTerm,
            page: page.toString(),
            size: size.toString(),
            sort: `${sortBy},${direction}`
        });

        const response = await api.get<PageResponse<Client>>(`/api/clients/search?${params.toString()}`);
        return response.data;
    },

    getClientsByStatus: async (
        active: boolean,
        page: number = 0,
        size: number = 10,
        sortBy: string = 'name',
        direction: string = 'asc'
    ): Promise<PageResponse<Client>> => {
        try {
            const params = new URLSearchParams();
            params.append('active', active.toString());
            params.append('page', page.toString());
            params.append('size', size.toString());
            params.append('sortBy', sortBy);
            params.append('direction', direction);

            const response = await api.get<PageResponse<Client>>(`/api/clients/status?${params.toString()}`);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    }
};