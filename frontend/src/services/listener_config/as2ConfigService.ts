import { listenerApiService } from './listenerApiService';
import { As2Config } from './types';
import { AxiosError } from 'axios';

const API_PATH = '/api/as2/config';

class As2ConfigService {
  async getConfigurationByClientAndInterface(clientId: number, interfaceId: number): Promise<As2Config | null> {
    try {
      return await listenerApiService.get<As2Config>(`${API_PATH}/client/${clientId}/interface/${interfaceId}`);
    } catch (error) {
      if ((error as AxiosError).response?.status === 404) {
        return null;
      }
      throw error;
    }
  }

  async createConfiguration(config: Omit<As2Config, 'id'>): Promise<As2Config> {
    return listenerApiService.post<As2Config>(API_PATH, config);
  }

  async updateConfiguration(id: number, config: Omit<As2Config, 'id'>): Promise<As2Config> {
    return listenerApiService.put<As2Config>(`${API_PATH}/${id}`, config);
  }

  async deleteConfiguration(id: number): Promise<void> {
    return listenerApiService.delete(`${API_PATH}/${id}`);
  }

  async toggleActive(id: number): Promise<As2Config> {
    return listenerApiService.post<As2Config>(`${API_PATH}/${id}/toggle`);
  }
}

export const as2ConfigService = new As2ConfigService(); 