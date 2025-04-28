import { api } from '../core/apiService';
import { handleApiError } from '../../utils/errorHandler';
import { MappingRule, XsdElement, DatabaseField } from './types';

export interface XsdAttribute {
    name: string;
    type: string;
    required: boolean;
}

export interface XsdNamespace {
    prefix: string;
    uri: string;
}

export const xsdService = {
    getXsdStructure: async (xsdPath: string): Promise<XsdElement[]> => {
        try {
            const response = await api.get<XsdElement[]>('/api/mapping/xsd-structure', {
                params: { xsdPath }
            });
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    getXsdStructureById: async (interfaceId: number): Promise<XsdElement[]> => {
        try {
            const response = await api.get<XsdElement[]>(`/api/mapping/xsd-structure/${interfaceId}`);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    getDatabaseFields: async (clientId: number, interfaceId: number): Promise<DatabaseField[]> => {
        try {
            const response = await api.get<DatabaseField[]>('/api/mapping/database-fields', {
                params: { clientId, interfaceId }
            });
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    // New method to analyze an XSD file and detect its structure and namespaces
    analyzeXsdFile: async (file: File): Promise<{
        rootElement: string;
        namespaces: XsdNamespace[];
        structure: XsdElement[];
    }> => {
        try {
            const formData = new FormData();
            formData.append('file', file);
            
            const response = await api.post('/api/mapping/analyze-xsd', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            });
            
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    }
};
