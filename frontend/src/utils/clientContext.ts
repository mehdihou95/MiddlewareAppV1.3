import { api } from '../services/core/apiService';

const CLIENT_ID_HEADER = 'X-Client-ID';
const CLIENT_NAME_HEADER = 'X-Client-Name';

export const setClientContext = (clientId: number, clientName?: string): void => {
  console.log('Setting client context:', clientId);
  if (clientId) {
    api.defaults.headers.common[CLIENT_ID_HEADER] = clientId.toString();
    
    if (clientName) {
      api.defaults.headers.common[CLIENT_NAME_HEADER] = clientName;
    }
  }
};

export const clearClientContext = (): void => {
  delete api.defaults.headers.common[CLIENT_ID_HEADER];
  delete api.defaults.headers.common[CLIENT_NAME_HEADER];
};

export const getClientContext = (): number | null => {
  const clientId = api.defaults.headers.common[CLIENT_ID_HEADER];
  return clientId ? Number(clientId) : null;
}; 