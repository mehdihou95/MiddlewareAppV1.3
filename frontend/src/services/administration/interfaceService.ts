import { Interface, InterfaceMapping } from './types';
import { PageResponse } from '../core/types';
import { handleApiError } from '../../utils/errorHandler';
import { setClientContext } from '../../utils/clientContext';
import { api } from '../core/apiService';
import { ENDPOINTS } from '../../config/apiConfig';
import { createPaginationParams, PaginationParams } from '../../utils/paginationUtils';

interface InterfaceResponse {
  content: Interface[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// Constants for pagination and sorting
const DEFAULT_PAGE = 0;
const DEFAULT_PAGE_SIZE = 10;
const DEFAULT_SORT_FIELD = 'name';
const DEFAULT_SORT_ORDER = 'asc';

// Validation constants
const VALIDATION_RULES = {
  NAME: { min: 3, max: 50 },
  TYPE: { max: 20 },
  DESCRIPTION: { max: 500 },
  SCHEMA_PATH: { max: 255 },
  ROOT_ELEMENT: { max: 100 },
  NAMESPACE: { max: 255 }
} as const;

// Helper function to create URLSearchParams
const createSearchParams = (params: Record<string, any>): URLSearchParams => {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      searchParams.append(key, value.toString());
    }
  });
  return searchParams;
};

const validateInterfaceData = (data: Partial<Interface>): void => {
  if (!data.name || data.name.length < VALIDATION_RULES.NAME.min || data.name.length > VALIDATION_RULES.NAME.max) {
    throw new Error(`Name must be between ${VALIDATION_RULES.NAME.min} and ${VALIDATION_RULES.NAME.max} characters`);
  }
  if (!data.type || data.type.length > VALIDATION_RULES.TYPE.max) {
    throw new Error(`Type must not exceed ${VALIDATION_RULES.TYPE.max} characters`);
  }
  if (data.description && data.description.length > VALIDATION_RULES.DESCRIPTION.max) {
    throw new Error(`Description must not exceed ${VALIDATION_RULES.DESCRIPTION.max} characters`);
  }
  if (data.schemaPath && data.schemaPath.length > VALIDATION_RULES.SCHEMA_PATH.max) {
    throw new Error(`Schema path must not exceed ${VALIDATION_RULES.SCHEMA_PATH.max} characters`);
  }
  if (!data.rootElement || data.rootElement.length > VALIDATION_RULES.ROOT_ELEMENT.max) {
    throw new Error(`Root element must not exceed ${VALIDATION_RULES.ROOT_ELEMENT.max} characters`);
  }
  if (data.namespace && data.namespace.length > VALIDATION_RULES.NAMESPACE.max) {
    throw new Error(`Namespace must not exceed ${VALIDATION_RULES.NAMESPACE.max} characters`);
  }
  if (!data.client?.id) {
    throw new Error('Client ID is required');
  }
};

class InterfaceService {
  private handleServiceError(error: any, action: string): never {
    const apiError = handleApiError(error);
    const enhancedError = new Error(apiError.message);
    (enhancedError as any).status = error.response?.status;
    (enhancedError as any).action = action;
    throw enhancedError;
  }

  async getAllInterfaces(params: PaginationParams): Promise<InterfaceResponse> {
    try {
      const response = await api.get<InterfaceResponse>(ENDPOINTS.INTERFACES.BASE, {
        params: createPaginationParams(params)
      });
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  }

  async getInterfacesByClientId(clientId: number): Promise<Interface[]> {
    try {
      const response = await api.get<Interface[]>(ENDPOINTS.INTERFACES.BY_CLIENT(clientId));
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      throw handleApiError(error);
    }
  }

  async getInterfaceById(id: number): Promise<Interface> {
    try {
      const response = await api.get<Interface>(ENDPOINTS.INTERFACES.BY_ID(id));
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  }

  async createInterface(interfaceData: Omit<Interface, 'id'>): Promise<Interface> {
    try {
      validateInterfaceData(interfaceData);
      const response = await api.post<Interface>(ENDPOINTS.INTERFACES.BASE, interfaceData);
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  }

  async updateInterface(id: number, interfaceData: Partial<Interface>): Promise<Interface> {
    try {
      validateInterfaceData(interfaceData);
      const response = await api.put<Interface>(ENDPOINTS.INTERFACES.BY_ID(id), interfaceData);
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  }

  async deleteInterface(id: number): Promise<void> {
    try {
      await api.delete(ENDPOINTS.INTERFACES.BY_ID(id));
    } catch (error) {
      throw handleApiError(error);
    }
  }

  async getInterfacesByStatus(
    isActive: boolean,
    page: number = DEFAULT_PAGE,
    size: number = DEFAULT_PAGE_SIZE,
    sortBy: string = DEFAULT_SORT_FIELD,
    sortDirection: string = DEFAULT_SORT_ORDER,
    clientId?: number
  ): Promise<PageResponse<Interface>> {
    try {
      if (clientId) {
        setClientContext(clientId);
      }

      const params = createSearchParams({
        page,
        size,
        sortBy,
        sortDirection
      });

      const response = await api.get<PageResponse<Interface>>(
        `${ENDPOINTS.INTERFACES.STATUS(isActive)}?${params.toString()}`
      );
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  }

  async getInterfaceMappings(interfaceId: number): Promise<InterfaceMapping[]> {
    try {
      const response = await api.get<InterfaceMapping[]>(ENDPOINTS.INTERFACES.MAPPINGS(interfaceId));
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  }

  async updateInterfaceMappings(id: number, mappings: InterfaceMapping[]): Promise<InterfaceMapping[]> {
    try {
      const response = await api.put<InterfaceMapping[]>(
        ENDPOINTS.INTERFACES.MAPPINGS(id), 
        mappings,
        {
          params: {
            clientId: mappings[0]?.client?.id
          }
        }
      );
      return response.data;
    } catch (error) {
      throw handleApiError(error);
    }
  }

  async createInterfaceMapping(interfaceId: number, mapping: InterfaceMapping): Promise<InterfaceMapping> {
    try {
      const response = await api.post<InterfaceMapping>(ENDPOINTS.INTERFACES.MAPPINGS(interfaceId), mapping);
      return response.data;
    } catch (error) {
      this.handleServiceError(error, 'creating interface mapping');
    }
  }

  async updateInterfaceMapping(interfaceId: number, mappingId: number, mapping: InterfaceMapping): Promise<InterfaceMapping> {
    try {
      const response = await api.put<InterfaceMapping>(`${ENDPOINTS.INTERFACES.MAPPINGS(interfaceId)}/${mappingId}`, mapping);
      return response.data;
    } catch (error) {
      this.handleServiceError(error, 'updating interface mapping');
    }
  }

  async deleteInterfaceMapping(interfaceId: number, mappingId: number): Promise<void> {
    try {
      await api.delete(`${ENDPOINTS.INTERFACES.MAPPINGS(interfaceId)}/${mappingId}`);
    } catch (error) {
      this.handleServiceError(error, 'deleting interface mapping');
    }
  }
}

export const interfaceService = new InterfaceService(); 