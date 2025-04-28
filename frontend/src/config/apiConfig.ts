export const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';
export const API_URL = API_BASE_URL;

// Default request timeout
export const DEFAULT_TIMEOUT = 30000; // 30 seconds

// API endpoints
export const ENDPOINTS = {
  // Auth endpoints
  AUTH: {
    LOGIN: '/api/auth/login',
    LOGOUT: '/api/auth/logout',
    REFRESH: '/api/auth/refresh',
    VALIDATE: '/api/auth/validate',
  },
  // Client endpoints
  CLIENTS: {
    BASE: '/api/clients',
    BY_ID: (id: number) => `/api/clients/${id}`,
    INTERFACES: (id: number) => `/api/interfaces/client/${id}`,
    ONBOARDING: {
      NEW: '/api/clients/onboarding/new',
      CLONE: (sourceId: number) => `/api/clients/onboarding/clone/${sourceId}`,
    },
  },
  // Interface endpoints
  INTERFACES: {
    BASE: '/api/interfaces',
    BY_ID: (id: number) => `/api/interfaces/${id}`,
    BY_CLIENT: (clientId: number) => `/api/clients/${clientId}/interfaces`,
    CLIENT_INTERFACES: (clientId: number) => `/api/clients/${clientId}/interfaces`,
    STATUS: (isActive: boolean) => `/api/interfaces/status/${isActive}`,
    MAPPINGS: (id: number) => `/api/interfaces/${id}/mappings`,
  },
  // Mapping rule endpoints
  MAPPING_RULES: {
    BASE: '/api/mapping-rules',
    BY_ID: (id: number) => `/api/mapping-rules/${id}`,
    BY_INTERFACE: (interfaceId: number) => `/api/interfaces/${interfaceId}/mapping-rules`,
  },
  // Processed file endpoints
  PROCESSED_FILES: {
    BASE: '/api/processed-files',
    BY_ID: (id: number) => `/api/processed-files/${id}`,
    BY_CLIENT: (clientId: number) => `/api/processed-files/client/${clientId}`,
    SEARCH: '/api/processed-files/search',
    STATUS: (status: string) => `/api/processed-files/status/${status}`,
    DATE_RANGE: '/api/processed-files/date-range',
    CLIENT_STATUS: (clientId: number, status: string) => 
      `/api/processed-files/client/${clientId}/status/${status}`,
    CLIENT_DATE_RANGE: (clientId: number) => 
      `/api/processed-files/client/${clientId}/date-range`,
  },
  // User endpoints
  USER: {
    CURRENT: '/api/user',
  },
}; 