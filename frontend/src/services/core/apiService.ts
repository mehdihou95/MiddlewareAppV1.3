import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError, AxiosRequestHeaders } from 'axios';
import { tokenService, TokenResponse } from 'services/core/tokenService';
import { API_URL, DEFAULT_TIMEOUT } from '../../config/apiConfig';
import { ApiResponse, ErrorResponse } from './types';

// Request retry configuration
interface RetryConfig extends AxiosRequestConfig {
  _retry?: boolean;
  _csrfRetry?: boolean;
}

// Create API instance
const createApiInstance = (): AxiosInstance => {
  const instance = axios.create({
    baseURL: API_URL,
    timeout: DEFAULT_TIMEOUT,
    headers: {
      'Content-Type': 'application/json',
    },
    withCredentials: true, // Enable cookies for CSRF
  });
  
  // Configure request interceptor
  instance.interceptors.request.use(
    async (config) => {
      if (!config.headers) {
        config.headers = {} as AxiosRequestHeaders;
      }
      
      // Debug: Log request details
      console.log('Making request to:', config.url);
      console.log('Request method:', config.method);
      
      // Skip auth token for auth endpoints
      if (!config.url?.includes('/auth/')) {
        const token = tokenService.getAccessToken();
        if (token) {
          config.headers['Authorization'] = `Bearer ${token}`;
        }
      }
      
      // Add CSRF token for non-GET requests
      if (config.method?.toUpperCase() !== 'GET') {
        const csrfToken = tokenService.getCsrfToken();
        if (csrfToken) {
          config.headers['X-XSRF-TOKEN'] = csrfToken;
          console.log('Using existing CSRF token:', csrfToken);
        } else if (!config.url?.includes('/auth/refresh-csrf')) {
          // Get new CSRF token if not present and not already requesting one
          try {
            console.log('No CSRF token found, requesting a new one');
            const response = await axios.post(`${API_URL}/api/auth/refresh-csrf`, {}, { withCredentials: true });
            const newCsrfToken = response.headers['x-xsrf-token'];
            if (newCsrfToken) {
              console.log('New CSRF token received:', newCsrfToken);
              tokenService.setCsrfToken(newCsrfToken);
              config.headers['X-XSRF-TOKEN'] = newCsrfToken;
            } else {
              console.error('No CSRF token in response headers');
            }
          } catch (error) {
            console.error('Failed to refresh CSRF token:', error);
          }
        }
      }
      
      // Add client context if available
      const clientId = localStorage.getItem('selectedClientId');
      if (clientId) {
        config.headers['X-Client-ID'] = clientId;
        console.log('Added Client ID header:', clientId);
      }
      
      // Log final request configuration
      console.log('Final request headers:', config.headers);
      console.log('Final request configuration:', {
        url: config.url,
        method: config.method,
        baseURL: config.baseURL,
        withCredentials: config.withCredentials
      });
      
      return config;
    },
    (error) => {
      console.error('Request interceptor error:', error);
      return Promise.reject(error);
    }
  );
  
  // Configure response interceptor
  instance.interceptors.response.use(
    (response) => {
      // Debug: Log response details
      console.log('Response received from:', response.config.url);
      console.log('Response status:', response.status);
      console.log('Response headers:', response.headers);
      
      // Store CSRF token from response headers if present
      const csrfToken = response.headers['x-xsrf-token'];
      if (csrfToken) {
        console.log('New CSRF token received');
        tokenService.setCsrfToken(csrfToken);
      }

      // Store new tokens if they are in the response data
      if (response.data?.token && response.data?.refreshToken) {
        console.log('New tokens received in response');
        tokenService.setTokens(response.data.token, response.data.refreshToken);
      }

      console.debug('Response:', {
        url: response.config.url,
        status: response.status,
        data: response.data
      });
      return response;
    },
    async (error: AxiosError) => {
      // Debug: Log error details
      console.error('Response error:', {
        url: error.config?.url,
        status: error.response?.status,
        data: error.response?.data,
        message: error.message
      });
      
      const originalRequest = error.config as RetryConfig;
      if (!originalRequest) {
        return Promise.reject(error);
      }
      
      // Handle CSRF token expiration (403 Forbidden)
      if (error.response?.status === 403 && !originalRequest._csrfRetry) {
        console.log('Attempting CSRF token refresh');
        originalRequest._csrfRetry = true;
        
        try {
          const response = await instance.post<{ csrfToken: string }>('/api/auth/refresh-csrf');
          if (response.data.csrfToken) {
            console.log('New CSRF token obtained');
            tokenService.setCsrfToken(response.data.csrfToken);
            
            if (!originalRequest.headers) {
              originalRequest.headers = {};
            }
            originalRequest.headers['X-XSRF-TOKEN'] = response.data.csrfToken;
            
            return instance(originalRequest);
          }
        } catch (csrfError) {
          console.error('CSRF refresh failed:', csrfError);
          return Promise.reject(csrfError);
        }
      }
      
      // Handle JWT token expiration (401 Unauthorized)
      if (error.response?.status === 401 && !originalRequest._retry) {
        console.log('Attempting token refresh');
        originalRequest._retry = true;
        
        try {
          const refreshToken = tokenService.getRefreshToken();
          if (!refreshToken) {
            throw new Error('No refresh token available');
          }
          
          const response = await instance.post<TokenResponse>('/api/auth/refresh', { refreshToken });
          const { token, refreshToken: newRefreshToken } = response.data;
          
          console.log('New tokens obtained');
          tokenService.setTokens(token, newRefreshToken);
          
          if (!originalRequest.headers) {
            originalRequest.headers = {};
          }
          originalRequest.headers['Authorization'] = `Bearer ${token}`;
          
          return instance(originalRequest);
        } catch (refreshError) {
          console.error('Token refresh failed:', refreshError);
          tokenService.clearTokens();
          
          // Redirect to login page if not already there
          if (!window.location.pathname.includes('/login')) {
            window.location.href = '/login';
          }
          return Promise.reject(refreshError);
        }
      }
      
      return Promise.reject(error);
    }
  );
  
  return instance;
};

// Create and export the API instance
export const api = createApiInstance();

// Helper methods for common API operations
export const apiService = {
  ensureCsrfToken: async () => {
    const csrfToken = tokenService.getCsrfToken();
    if (!csrfToken) {
      console.log('No CSRF token found, fetching new one');
      const response = await api.post<{ csrfToken: string }>('/api/auth/refresh-csrf');
      if (response.data.csrfToken) {
        tokenService.setCsrfToken(response.data.csrfToken);
      }
    }
  },

  get: async <T>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    const response = await api.get<T>(url, config);
    return response.data;
  },
  
  post: async <T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    const response = await api.post<T>(url, data, config);
    return response.data;
  },
  
  put: async <T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    const response = await api.put<T>(url, data, config);
    return response.data;
  },
  
  delete: async <T>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    const response = await api.delete<T>(url, config);
    return response.data;
  },
  
  // Method to handle API errors consistently
  handleError: (error: any): ErrorResponse => {
    if (axios.isAxiosError(error)) {
      const axiosError = error as AxiosError<any>;
      
      // Return structured error response
      return {
        code: axiosError.response?.data?.code || 'ERROR',
        message: axiosError.response?.data?.message || 'An error occurred',
        details: axiosError.response?.data?.details,
      };
    }
    
    // Handle non-Axios errors
    return {
      code: 'UNKNOWN_ERROR',
      message: error.message || 'An unknown error occurred',
    };
  }
}; 