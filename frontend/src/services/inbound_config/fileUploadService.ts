import { api } from '../core/apiService';
import { ProcessedFile } from './types';
import { handleApiError } from '../../utils/errorHandler';
import { clientService } from '../administration/clientService';

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

class FileUploadService {
  validateFileSize(file: File): boolean {
    return file.size <= MAX_FILE_SIZE;
  }

  validateFileType(file: File, interfaceType: string): boolean {
    if (interfaceType === 'ASN' || interfaceType === 'ORDER') {
      return file.type === 'text/xml' || file.name.toLowerCase().endsWith('.xml');
    }
    // For other interface types
    const fileName = file.name.toLowerCase();
    return file.type === 'text/xml' || 
           file.type === 'application/json' || 
           file.type === 'text/csv' ||
           !!fileName.match(/\.(xml|json|csv|edi)$/);
  }

  async verifyClientAndInterface(clientId: number, interfaceId: number): Promise<{ type: string }> {
    try {
      // Verify client exists and is active
      const client = await clientService.getClientById(clientId);
      if (!client || client.status !== 'ACTIVE') {
        throw new Error('Invalid or inactive client');
      }

      // Verify interface exists and belongs to the client
      const interfaces = await clientService.getClientInterfaces(clientId);
      const targetInterface = interfaces.find(i => i.id === interfaceId);
      if (!targetInterface || !targetInterface.isActive) {
        throw new Error('Invalid or inactive interface for this client');
      }

      return { type: targetInterface.type };
    } catch (error) {
      throw handleApiError(error);
    }
  }

  async uploadFile(file: File, clientId: number, interfaceId: number): Promise<ProcessedFile> {
    try {
      // First verify client and interface
      const { type: interfaceType } = await this.verifyClientAndInterface(clientId, interfaceId);

      // Validate file
      if (!this.validateFileSize(file)) {
        throw new Error('File size exceeds maximum limit of 10MB');
      }
      if (!this.validateFileType(file, interfaceType)) {
        throw new Error(`Invalid file type for ${interfaceType} interface. Please check the file format.`);
      }

      const formData = new FormData();
      formData.append('file', file);
      formData.append('clientId', clientId.toString());
      formData.append('interfaceId', interfaceId.toString());

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
  }
}

export const fileUploadService = new FileUploadService(); 