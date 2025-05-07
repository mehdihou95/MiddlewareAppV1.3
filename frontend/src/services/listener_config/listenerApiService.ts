import axios, { AxiosInstance, AxiosRequestConfig, AxiosError, AxiosHeaders } from 'axios';
import { tokenService } from '../core/tokenService';
import { handleApiError } from '../../utils/errorHandler';
import { authService } from '../administration/authService';
import { apiService } from '../core/apiService';

// Create axios instance for listener service
const listenerApi: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8081',
  timeout: 30000,
  withCredentials: true
});

// Function to ensure we have a valid CSRF token from main API
const ensureMainApiCsrfToken = async () => {
  try {
    // First check if we already have a valid token
    const existingToken = tokenService.getCsrfToken();
    if (!existingToken) {
      // If no token exists, get one from the main API
      console.log('No CSRF token found, fetching from main API');
      await apiService.ensureCsrfToken();
    }
  } catch (error) {
    console.error('Failed to ensure CSRF token:', error);
    throw error;
  }
};

// Request interceptor
listenerApi.interceptors.request.use(
  async (config) => {
    // Get the token
    let currentToken = tokenService.getAccessToken();
    
    // Check if token needs refresh before it expires
    if (currentToken && !tokenService.isTokenValid(currentToken)) {
      try {
        // Proactively refresh the token
        const newToken = await authService.refreshToken();
        if (newToken) {
          currentToken = newToken;
        }
      } catch (refreshError) {
        console.error('Proactive token refresh failed:', refreshError);
      }
    }
    
    // For non-GET requests, ensure we have a valid CSRF token from main API
    if (config.method?.toUpperCase() !== 'GET') {
      await ensureMainApiCsrfToken();
      const csrfToken = tokenService.getCsrfToken();
      if (csrfToken) {
        if (!config.headers) {
          config.headers = new AxiosHeaders();
        }
        config.headers.set('X-XSRF-TOKEN', csrfToken);
        console.log('Using CSRF token from main API:', csrfToken);
      } else {
        console.warn('No CSRF token available after ensuring token');
      }
    }

    // Set headers
    if (!config.headers) {
      config.headers = new AxiosHeaders();
    }
    
    config.headers.set('Content-Type', 'application/json');
    if (currentToken) {
      config.headers.set('Authorization', `Bearer ${currentToken}`);
    }

    // Log request details
    console.debug('Listener API Request:', {
      method: config.method?.toUpperCase(),
      url: config.url,
      headers: config.headers,
      data: config.data
    });

    return config;
  },
  (error) => {
    console.error('Listener API Request Error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor
listenerApi.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    if (error.response?.status === 403 && error.response?.data?.error === 'Invalid CSRF token') {
      console.error('Invalid CSRF token, will refresh from main API');
      try {
        // Clear existing token and get a new one from main API
        document.cookie = `XSRF-TOKEN=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT`;
        await ensureMainApiCsrfToken();
        
        // Retry the request
        const config = error.config;
        const csrfToken = tokenService.getCsrfToken();
        if (csrfToken && config) {
          if (!config.headers) {
            config.headers = new AxiosHeaders();
          }
          config.headers.set('X-XSRF-TOKEN', csrfToken);
          return listenerApi(config);
        }
      } catch (retryError) {
        console.error('Failed to refresh CSRF token:', retryError);
      }
    }
    return Promise.reject(error);
  }
);

// Export the API service methods
export const listenerApiService = {
  get: async <T>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    const response = await listenerApi.get<T>(url, config);
    return response.data;
  },

  post: async <T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    // Ensure we have a valid CSRF token before making the request
    await ensureMainApiCsrfToken();
    const response = await listenerApi.post<T>(url, data, config);
    return response.data;
  },

  put: async <T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    // Ensure we have a valid CSRF token before making the request
    await ensureMainApiCsrfToken();
    const response = await listenerApi.put<T>(url, data, config);
    return response.data;
  },

  delete: async <T>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    // Ensure we have a valid CSRF token before making the request
    await ensureMainApiCsrfToken();
    const response = await listenerApi.delete<T>(url, config);
    return response.data;
  }
};

export { listenerApi }; 