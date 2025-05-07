import axios from 'axios';
import { MappingRule } from './types';
import { PageResponse } from '../core/types';
import { API_URL } from '../../config/apiConfig';
import { api, apiService } from '../core/apiService';
import { tokenService } from '../core/tokenService';

export const mappingRuleService = {
  getAllMappingRules: async (
    page: number = 0,
    size: number = 10,
    sortBy: string = 'name',
    direction: string = 'asc',
    clientId?: number,
    interfaceId?: number,
    isActive?: boolean
  ): Promise<PageResponse<MappingRule>> => {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sortBy', sortBy);
    params.append('direction', direction);
    if (clientId) params.append('clientId', clientId.toString());
    if (interfaceId) params.append('interfaceId', interfaceId.toString());
    if (isActive !== undefined) params.append('isActive', isActive.toString());

    // Ensure we have a CSRF token before making the request
    await apiService.ensureCsrfToken();
    
    console.debug('Making mapping rules request:', {
      url: `/api/mapping/rules?${params.toString()}`,
      csrfToken: tokenService.getCsrfToken(),
      jwt: tokenService.getAccessToken()?.substring(0, 20) + '...',
    });

    try {
      const response = await api.get<PageResponse<MappingRule>>(`/api/mapping/rules?${params.toString()}`);
      console.debug('Mapping rules response:', {
        status: response.status,
        headers: response.headers,
        data: response.data
      });
      return response.data;
    } catch (error) {
      console.error('Failed to fetch mapping rules:', error);
      throw error;
    }
  },

  getMappingRuleById: async (id: number): Promise<MappingRule> => {
    const response = await api.get<MappingRule>(`/api/mapping/rules/${id}`);
    return response.data;
  },

  createMappingRule: async (mappingRuleData: Omit<MappingRule, 'id'>): Promise<MappingRule> => {
    const response = await api.post<MappingRule>(`/api/mapping/rules`, mappingRuleData);
    return response.data;
  },

  updateMappingRule: async (id: number, mappingRuleData: Partial<MappingRule>): Promise<MappingRule> => {
    const response = await api.put<MappingRule>(`/api/mapping/rules/${id}`, mappingRuleData);
    return response.data;
  },

  deleteMappingRule: async (id: number): Promise<void> => {
    await api.delete(`/api/mapping/rules/${id}`);
  },

  getMappingRulesByInterface: async (
    interfaceId: number,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'name',
    direction: string = 'asc'
  ): Promise<PageResponse<MappingRule>> => {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sortBy', sortBy);
    params.append('direction', direction);
    params.append('interfaceId', interfaceId.toString());

    const response = await api.get<PageResponse<MappingRule>>(`/api/mapping/rules?${params.toString()}`);
    return response.data;
  }
}; 