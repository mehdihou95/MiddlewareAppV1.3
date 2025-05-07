import axios from 'axios';
import { API_URL } from '../../config/apiConfig';
import { ProcessedFile } from './types';

export const fileService = {
    uploadFile: async (file: File): Promise<ProcessedFile> => {
        const formData = new FormData();
        formData.append('file', file);
        const response = await axios.post<ProcessedFile>(`${API_URL}/api/files/upload`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
        return response.data;
    },

    getProcessedFiles: async (page: number = 0, size: number = 10): Promise<ProcessedFile[]> => {
        const response = await axios.get<ProcessedFile[]>(`${API_URL}/api/files/processed`, {
            params: { page, size }
        });
        return response.data;
    },

    getFileById: async (id: number): Promise<ProcessedFile> => {
        const response = await axios.get<ProcessedFile>(`${API_URL}/api/files/${id}`);
        return response.data;
    },

    deleteFile: async (id: number): Promise<void> => {
        await axios.delete(`${API_URL}/api/files/${id}`);
    },

    downloadFile: async (id: number): Promise<Blob> => {
        const response = await axios.get(`${API_URL}/api/files/${id}/download`, {
            responseType: 'blob'
        });
        return response.data;
    }
}; 