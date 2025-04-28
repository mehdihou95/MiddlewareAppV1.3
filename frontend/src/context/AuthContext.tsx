import React, { createContext, useContext, useState, useEffect } from 'react';
import { authService } from '../services/administration/authService';
import { tokenService } from '../services/core/tokenService';

interface User {
  username: string;
  roles: string[];
  authenticated: boolean;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  error: string | null;
  login: (username: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      if (!tokenService.getAccessToken()) {
        setUser(null);
        setLoading(false);
        return;
      }

      const isValid = await authService.validateToken();
      if (isValid) {
        const userInfo = tokenService.getUserInfo();
        if (userInfo) {
          setUser({
            username: userInfo.username,
            roles: userInfo.roles,
            authenticated: true
          });
        } else {
          setUser({
            username: '',
            roles: [],
            authenticated: false
          });
        }
      } else {
        setUser({
          username: '',
          roles: [],
          authenticated: false
        });
      }
    } catch (err) {
      console.error('Auth check error:', err);
      setUser(null);
      tokenService.clearTokens();
    } finally {
      setLoading(false);
    }
  };

  const login = async (username: string, password: string): Promise<boolean> => {
    try {
      setLoading(true);
      setError(null);

      const response = await authService.login(username, password);
      
      await tokenService.setTokens(response.token, response.refreshToken);
      
      setUser({
        username: response.username,
        roles: response.roles || [],
        authenticated: true
      });
      
      return true;
    } catch (err: any) {
      console.error('Login error:', err);
      const errorMessage = err.message || 'Invalid username or password';
      setError(errorMessage);
      setUser(null);
      return false;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
    setError(null);
  };

  const hasRole = (role: string): boolean => {
    return user?.roles.includes(role) ?? false;
  };

  const value = {
    user,
    loading,
    error,
    login,
    logout,
    isAuthenticated: !!user,
    hasRole,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext; 