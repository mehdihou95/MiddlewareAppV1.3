import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { Client, Interface } from '../types';
import { clientService } from '../services/administration/clientService';
import { interfaceService } from '../services/administration/interfaceService';
import { useAuth } from './AuthContext';
import { useNavigate } from 'react-router-dom';

type SortOrder = 'asc' | 'desc';

interface ClientInterfaceContextType {
  clients: Client[];
  interfaces: Interface[];
  selectedClient: Client | null;
  selectedInterface: Interface | null;
  loading: boolean;
  error: string | null;
  refreshClients: (page?: number, pageSize?: number, sortField?: string, sortOrder?: SortOrder) => Promise<void>;
  refreshInterfaces: () => Promise<void>;
  setSelectedClient: (client: Client | null) => void;
  setSelectedInterface: (interface_: Interface | null) => void;
  hasRole: (role: string) => boolean;
  setError: (error: string | null) => void;
  setClients: (clients: Client[]) => void;
  setInterfaces: (interfaces: Interface[]) => void;
}

const ClientInterfaceContext = createContext<ClientInterfaceContextType | undefined>(undefined);

const DEFAULT_PAGE_SIZE = 10;
const DEFAULT_SORT_FIELD = 'name';
const DEFAULT_SORT_ORDER: SortOrder = 'asc';

export const ClientInterfaceProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [clients, setClients] = useState<Client[]>([]);
  const [interfaces, setInterfaces] = useState<Interface[]>([]);
  const [selectedClient, setSelectedClient] = useState<Client | null>(null);
  const [selectedInterface, setSelectedInterface] = useState<Interface | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const { isAuthenticated, hasRole } = useAuth();

  const clearAllData = useCallback(() => {
    setClients([]);
    setInterfaces([]);
    setSelectedClient(null);
    setSelectedInterface(null);
    setError(null);
  }, []);

  const clearSavedSelections = useCallback(() => {
    localStorage.removeItem('selectedClientId');
    localStorage.removeItem('selectedInterfaceId');
  }, []);

  const handleError = useCallback((error: any, action: string) => {
    console.error(`Error ${action}:`, error);
    
    if (error.response?.status === 401 || error.response?.status === 403) {
      clearAllData();
      if (error.response.status === 403) {
        setError('You do not have permission to access this data.');
      }
      navigate('/login');
      return;
    }
    
    setError(error.message || `Failed to ${action}. Please try again.`);
  }, [navigate, clearAllData]);

  // Load clients when authentication state changes
  useEffect(() => {
    const loadInitialClients = async () => {
      if (!isAuthenticated) {
        clearAllData();
        return;
      }
      
      try {
        setLoading(true);
        setError(null);
        console.log('Loading clients after auth state change...');
        const response = await clientService.getAllClients({ 
          page: 0, 
          size: DEFAULT_PAGE_SIZE, 
          sort: DEFAULT_SORT_FIELD, 
          direction: DEFAULT_SORT_ORDER 
        });
        
        if (response?.content) {
          setClients(response.content);
        } else {
          console.warn('No clients found in response');
          setClients([]);
        }
      } catch (error: any) {
        handleError(error, 'load initial clients');
      } finally {
        setLoading(false);
      }
    };

    loadInitialClients();
  }, [isAuthenticated, handleError, clearAllData]);

  const refreshClients = useCallback(async (
    page = 0,
    pageSize = DEFAULT_PAGE_SIZE,
    sortField = DEFAULT_SORT_FIELD,
    sortOrder: SortOrder = DEFAULT_SORT_ORDER
  ) => {
    if (!isAuthenticated) {
      clearAllData();
      return;
    }
    
    try {
      setLoading(true);
      setError(null);
      const response = await clientService.getAllClients({
        page,
        size: pageSize,
        sort: sortField,
        direction: sortOrder
      });
      setClients(response.content);
    } catch (err: any) {
      handleError(err, 'load clients');
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated, handleError, clearAllData]);

  const loadInterfaces = useCallback(async () => {
    if (!selectedClient?.id) {
      console.log('No client selected, clearing interfaces');
      setInterfaces([]);
      return;
    }

    const clientId = selectedClient.id;
    console.log('Loading interfaces for client ID:', clientId);
    
    try {
      setLoading(true);
      setError(null);
      const response = await interfaceService.getInterfacesByClientId(clientId);
      console.log('Received interfaces response:', response);
      
      // Map the interfaces with consistent properties
      const mappedInterfaces = (response || []).map(intf => ({
        id: intf.id,
        name: intf.name,
        type: intf.type,
        description: intf.description || '',
        rootElement: intf.rootElement || '',
        namespace: intf.namespace || '',
        schemaPath: intf.schemaPath || '',
        priority: intf.priority || 0,
        status: intf.status || 'ACTIVE',
        isActive: intf.isActive ?? true,
        client: intf.client || selectedClient
      }));

      setInterfaces(mappedInterfaces);
      console.log('Interfaces state updated with:', mappedInterfaces);
      
    } catch (err: any) {
      console.error('Error loading interfaces:', err);
      handleError(err, 'load interfaces');
      setInterfaces([]);
    } finally {
      setLoading(false);
    }
  }, [selectedClient, handleError]);

  const refreshInterfaces = useCallback(async () => {
    if (!selectedClient || !isAuthenticated) {
      console.log('Cannot refresh interfaces:', { 
        hasSelectedClient: !!selectedClient, 
        isAuthenticated,
        selectedClientId: selectedClient?.id 
      });
      setInterfaces([]);
      return;
    }
    
    try {
      console.log('Refreshing interfaces for client:', selectedClient.id);
      setLoading(true);
      setError(null);
      
      const response = await interfaceService.getInterfacesByClientId(selectedClient.id);
      
      const mappedInterfaces = (response || []).map(intf => ({
        id: intf.id,
        name: intf.name,
        type: intf.type,
        description: intf.description || '',
        rootElement: intf.rootElement || '',
        namespace: intf.namespace || '',
        schemaPath: intf.schemaPath || '',
        priority: intf.priority || 0,
        status: intf.status || 'ACTIVE',
        isActive: intf.isActive ?? true,
        client: intf.client || selectedClient
      }));

      setInterfaces(mappedInterfaces);
      console.log('Interfaces refreshed:', mappedInterfaces);
      
    } catch (err: any) {
      console.error('Error refreshing interfaces:', err);
      handleError(err, 'refresh interfaces');
      setInterfaces([]);
    } finally {
      setLoading(false);
    }
  }, [selectedClient, isAuthenticated, handleError]);

  const handleSetSelectedClient = useCallback(async (client: Client | null) => {
    console.log('Setting selected client:', client?.id);
    setLoading(true);
    try {
      setSelectedClient(client);
      setSelectedInterface(null);
      setInterfaces([]); // Clear interfaces immediately when client changes
      
      if (client) {
        localStorage.setItem('selectedClientId', client.id.toString());
        console.log('Loading interfaces for new client:', client.id);
        const response = await interfaceService.getInterfacesByClientId(client.id);
        
        const mappedInterfaces = (response || []).map(intf => ({
          id: intf.id,
          name: intf.name,
          type: intf.type,
          description: intf.description || '',
          rootElement: intf.rootElement || '',
          namespace: intf.namespace || '',
          schemaPath: intf.schemaPath || '',
          priority: intf.priority || 0,
          status: intf.status || 'ACTIVE',
          isActive: intf.isActive ?? true,
          client: intf.client || client
        }));

        setInterfaces(mappedInterfaces);
        console.log('Interfaces loaded:', mappedInterfaces);
      } else {
        clearSavedSelections();
      }
    } catch (err: any) {
      console.error('Error loading interfaces:', err);
      handleError(err, 'load interfaces');
      setInterfaces([]);
    } finally {
      setLoading(false);
    }
  }, [clearSavedSelections, handleError]);

  const value: ClientInterfaceContextType = {
    clients,
    interfaces,
    selectedClient,
    selectedInterface,
    loading,
    error,
    refreshClients,
    refreshInterfaces,
    setSelectedClient: handleSetSelectedClient,
    setSelectedInterface,
    hasRole,
    setError,
    setClients,
    setInterfaces
  };

  return (
    <ClientInterfaceContext.Provider value={value}>
      {children}
    </ClientInterfaceContext.Provider>
  );
};

export const useClientInterface = () => {
  const context = useContext(ClientInterfaceContext);
  if (context === undefined) {
    throw new Error('useClientInterface must be used within a ClientInterfaceProvider');
  }
  return context;
}; 