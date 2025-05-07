import { jwtDecode } from 'jwt-decode';

// Token storage keys
const ACCESS_TOKEN_KEY = 'auth_token';
const REFRESH_TOKEN_KEY = 'refresh_token';
const CSRF_TOKEN_COOKIE = 'XSRF-TOKEN';  // Spring Security's default CSRF cookie name

// Token-related types
export interface JwtPayload {
  sub: string;
  exp: number;
  roles: string[];
  [key: string]: any;
}

export interface TokenResponse {
  token: string;
  refreshToken: string;
  username?: string;
  roles?: string[];
}

export const tokenService = {
  // Access token methods
  setAccessToken: (token: string): void => {
    localStorage.setItem(ACCESS_TOKEN_KEY, token);
  },
  
  getAccessToken: (): string | null => {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  },
  
  removeAccessToken: (): void => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
  },
  
  // Refresh token methods
  setRefreshToken: (token: string): void => {
    localStorage.setItem(REFRESH_TOKEN_KEY, token);
  },
  
  getRefreshToken: (): string | null => {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },
  
  removeRefreshToken: (): void => {
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
  
  // Combined token operations
  setTokens: (accessToken: string, refreshToken: string): void => {
    tokenService.setAccessToken(accessToken);
    tokenService.setRefreshToken(refreshToken);
  },
  
  clearTokens: (): void => {
    tokenService.removeAccessToken();
    tokenService.removeRefreshToken();
  },
  
  // Token validation
  isTokenValid: (token?: string): boolean => {
    const accessToken = token || tokenService.getAccessToken();
    if (!accessToken) return false;
    
    try {
      const payload = jwtDecode<JwtPayload>(accessToken);
      
      // Validate payload structure
      if (!payload || typeof payload.exp !== 'number') {
        console.error('Invalid token format: missing or invalid expiration');
        return false;
      }
      
      // Add buffer time (5 minutes) to prevent edge cases
      const bufferTime = 5 * 60 * 1000; // 5 minutes in milliseconds
      const currentTime = Date.now();
      const expirationTime = payload.exp * 1000; // Convert to milliseconds
      
      return expirationTime > currentTime + bufferTime;
    } catch (error) {
      console.error('Error validating token:', error);
      return false;
    }
  },
  
  // CSRF token methods
  getCsrfToken: (): string | null => {
    const cookies = document.cookie.split(';');
    const csrfCookie = cookies.find(cookie => cookie.trim().startsWith(`${CSRF_TOKEN_COOKIE}=`));
    if (csrfCookie) {
      const token = decodeURIComponent(csrfCookie.split('=')[1].trim());
      console.log('Found CSRF token in cookies:', token);
      return token;
    }
    console.log('No CSRF token found in cookies');
    return null;
  },
  
  setCsrfToken: (token: string): void => {
    console.log('Setting CSRF token cookie:', token);
    document.cookie = `${CSRF_TOKEN_COOKIE}=${token}; path=/`;
  },
  
  // Token payload extraction
  getTokenPayload: (): JwtPayload | null => {
    const token = tokenService.getAccessToken();
    if (!token) return null;
    
    try {
      return jwtDecode<JwtPayload>(token);
    } catch (error) {
      console.error('Error decoding token:', error);
      return null;
    }
  },
  
  getUserInfo: () => {
    const payload = tokenService.getTokenPayload();
    if (!payload) return null;
    
    return {
      username: payload.sub,
      roles: payload.roles || [],
    };
  }
}; 