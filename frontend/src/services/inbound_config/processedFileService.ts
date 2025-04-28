import { api } from '../core/apiService';
import { ProcessedFile } from './types';
import { PageResponse } from '../core/types';
import { handleApiError } from '../../utils/errorHandler';

export const processedFileService = {
  uploadFile: async (file: File, interfaceId: number): Promise<ProcessedFile> => {
    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await api.post<ProcessedFile>(
        `/api/files/upload/${interfaceId}`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data'
          },
          timeout: 60000 // 60 seconds timeout for large files
        }
      );
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  getAllProcessedFiles: async (
    page: number = 0,
    size: number = 10,
    sortBy: string = 'processedDate',
    direction: string = 'desc',
    fileNameFilter?: string,
    statusFilter?: string,
    startDate?: string,
    endDate?: string,
    clientId?: number,
    interfaceId?: number
  ): Promise<PageResponse<ProcessedFile>> => {
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
        sortBy,
        direction
      });
      
      if (fileNameFilter) params.append('fileNameFilter', fileNameFilter);
      if (statusFilter) params.append('statusFilter', statusFilter);
      if (startDate) params.append('startDate', startDate);
      if (endDate) params.append('endDate', endDate);
      if (clientId) params.append('clientId', clientId.toString());
      if (interfaceId) params.append('interfaceId', interfaceId.toString());
      
      const response = await api.get<PageResponse<ProcessedFile>>('/api/processed-files', { params });
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  getProcessedFileById: async (id: number): Promise<ProcessedFile> => {
    try {
      const response = await api.get<ProcessedFile>(`/api/files/${id}`);
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  createProcessedFile: async (processedFileData: Omit<ProcessedFile, 'id'>): Promise<ProcessedFile> => {
    const response = await api.post<ProcessedFile>(`/api/processed-files`, processedFileData);
    return response.data;
  },

  updateProcessedFile: async (id: number, processedFileData: Partial<ProcessedFile>): Promise<ProcessedFile> => {
    const response = await api.put<ProcessedFile>(`/api/processed-files/${id}`, processedFileData);
    return response.data;
  },

  deleteProcessedFile: async (id: number): Promise<void> => {
    try {
      await api.delete(`/api/files/${id}`);
    } catch (error) {
      throw handleApiError(error);
    }
  },

  getProcessedFilesByClient: async (
    clientId: number,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'processedDate',
    direction: string = 'desc'
  ): Promise<PageResponse<ProcessedFile>> => {
    let params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sortBy', sortBy);
    params.append('direction', direction);
    
    const response = await api.get<PageResponse<ProcessedFile>>(`/api/processed-files/client/${clientId}?${params.toString()}`);
    return response.data;
  },

  searchProcessedFiles: async (
    fileName: string,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'processedDate',
    direction: string = 'desc'
  ): Promise<PageResponse<ProcessedFile>> => {
    let params = new URLSearchParams();
    params.append('fileName', fileName);
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sortBy', sortBy);
    params.append('direction', direction);
    
    const response = await api.get<PageResponse<ProcessedFile>>(`/api/processed-files/search?${params.toString()}`);
    return response.data;
  },

  getProcessedFilesByStatus: async (
    status: string,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'processedDate',
    direction: string = 'desc'
  ): Promise<PageResponse<ProcessedFile>> => {
    let params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sortBy', sortBy);
    params.append('direction', direction);
    
    const response = await api.get<PageResponse<ProcessedFile>>(`/api/processed-files/status/${status}?${params.toString()}`);
    return response.data;
  },

  getProcessedFilesByDateRange: async (
    startDate: string,
    endDate: string,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'processedDate',
    direction: string = 'desc'
  ): Promise<PageResponse<ProcessedFile>> => {
    let params = new URLSearchParams();
    params.append('startDate', startDate);
    params.append('endDate', endDate);
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sortBy', sortBy);
    params.append('direction', direction);
    
    const response = await api.get<PageResponse<ProcessedFile>>(`/api/processed-files/date-range?${params.toString()}`);
    return response.data;
  },

  getProcessedFilesByClientAndStatus: async (
    clientId: number,
    status: string,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'processedDate',
    direction: string = 'desc'
  ): Promise<PageResponse<ProcessedFile>> => {
    let params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sortBy', sortBy);
    params.append('direction', direction);
    
    const response = await api.get<PageResponse<ProcessedFile>>(`/api/processed-files/client/${clientId}/status/${status}?${params.toString()}`);
    return response.data;
  },

  getProcessedFilesByClientAndDateRange: async (
    clientId: number,
    startDate: string,
    endDate: string,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'processedDate',
    direction: string = 'desc'
  ): Promise<PageResponse<ProcessedFile>> => {
    let params = new URLSearchParams();
    params.append('startDate', startDate);
    params.append('endDate', endDate);
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sortBy', sortBy);
    params.append('direction', direction);
    
    const response = await api.get<PageResponse<ProcessedFile>>(`/api/processed-files/client/${clientId}/date-range?${params.toString()}`);
    return response.data;
  },

  downloadFile: async (id: number): Promise<Blob> => {
    try {
      const response = await api.get(`/api/files/${id}/download`, {
        responseType: 'blob'
      });
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  },

  reprocessFile: async (id: number): Promise<void> => {
    try {
      await api.post(`/api/files/reprocess/${id}`);
    } catch (error) {
      throw handleApiError(error);
    }
  }
}; 