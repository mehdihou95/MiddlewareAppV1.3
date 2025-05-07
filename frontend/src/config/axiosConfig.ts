import axios from 'axios';
import { API_URL } from './apiConfig';

// Create axios instance with default config
const axiosInstance = axios.create({
  baseURL: API_URL,
  withCredentials: true // This is important for CSRF token
});

// Add request interceptor to include CSRF token
axiosInstance.interceptors.request.use((config) => {
  // Get CSRF token from cookie
  const token = document.cookie
    .split('; ')
    .find(row => row.startsWith('XSRF-TOKEN='))
    ?.split('=')[1];

  if (token) {
    config.headers['X-XSRF-TOKEN'] = decodeURIComponent(token);
  }

  // Get JWT token from localStorage
  const jwtToken = localStorage.getItem('token');
  if (jwtToken) {
    config.headers['Authorization'] = `Bearer ${jwtToken}`;
  }

  return config;
});

export default axiosInstance; 