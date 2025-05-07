import { listenerApiService } from './listenerApiService';
import { SftpConfig, SftpConfigCreate } from './types';
import { AxiosError } from 'axios';

const API_PATH = '/api/sftp/config';

class SftpConfigService {
  async getAllConfigurations(): Promise<SftpConfig[]> {
    return listenerApiService.get<SftpConfig[]>(API_PATH);
  }

  async getConfigurationByClientAndInterface(clientId: number, interfaceId: number): Promise<SftpConfig | null> {
    try {
      return await listenerApiService.get<SftpConfig>(`${API_PATH}/client/${clientId}/interface/${interfaceId}`);
    } catch (error) {
      if ((error as AxiosError).response?.status === 404) {
        return null;
      }
      throw error;
    }
  }

  async createConfiguration(config: SftpConfigCreate): Promise<SftpConfig> {
    return listenerApiService.post<SftpConfig>(API_PATH, config);
  }

  async updateConfiguration(id: number, config: SftpConfigCreate): Promise<SftpConfig> {
    return listenerApiService.put<SftpConfig>(`${API_PATH}/${id}`, config);
  }

  async deleteConfiguration(id: number): Promise<void> {
    return listenerApiService.delete(`${API_PATH}/${id}`);
  }

  async toggleActive(id: number): Promise<SftpConfig> {
    return listenerApiService.post<SftpConfig>(`${API_PATH}/${id}/toggle`);
  }
}

export const sftpConfigService = new SftpConfigService(); 