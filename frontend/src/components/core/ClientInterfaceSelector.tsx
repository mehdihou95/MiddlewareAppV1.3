import React from 'react';
import {
  Box,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Typography,
  Alert,
  CircularProgress,
  Button
} from '@mui/material';
import { useClientInterface } from '../../context/ClientInterfaceContext';
import { useNavigate } from 'react-router-dom';

interface ClientInterfaceSelectorProps {
  required?: boolean;
  showManageButtons?: boolean;
}

const ClientInterfaceSelector: React.FC<ClientInterfaceSelectorProps> = ({ 
  required = true,
  showManageButtons = false
}) => {
  const navigate = useNavigate();
  const {
    clients,
    interfaces,
    selectedClient,
    selectedInterface,
    setSelectedClient,
    setSelectedInterface,
    loading,
    error
  } = useClientInterface();

  const handleClientChange = (event: any) => {
    const clientId = event.target.value;
    const client = clients?.find(c => c.id === clientId);
    if (client) {
      setSelectedClient(client);
    }
  };

  const handleInterfaceChange = (event: any) => {
    const interfaceId = event.target.value;
    const interfaceObj = interfaces?.find(i => i.id === interfaceId);
    if (interfaceObj) {
      setSelectedInterface(interfaceObj);
    }
  };

  const navigateToClientManagement = () => {
    navigate('/clients');
  };

  const navigateToInterfaceManagement = () => {
    navigate('/interfaces');
  };

  return (
    <Box sx={{ mb: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">
          Select Client and Interface
        </Typography>
        
        {showManageButtons && (
          <Box>
            <Button 
              variant="outlined" 
              size="small" 
              onClick={navigateToClientManagement}
              sx={{ mr: 1 }}
            >
              Manage Clients
            </Button>
            <Button 
              variant="outlined" 
              size="small" 
              onClick={navigateToInterfaceManagement}
              disabled={!selectedClient}
            >
              Manage Interfaces
            </Button>
          </Box>
        )}
      </Box>
      
      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 2 }}>
          <CircularProgress size={24} />
        </Box>
      )}
      
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      
      <FormControl fullWidth sx={{ mb: 2 }}>
        <InputLabel>Client</InputLabel>
        <Select
          value={selectedClient?.id || ''}
          onChange={handleClientChange}
          label="Client"
          required={required}
        >
          {!clients || clients.length === 0 ? (
            <MenuItem disabled value="">
              {loading ? 'Loading clients...' : 'No clients available'}
            </MenuItem>
          ) : (
            clients.map((client) => (
              <MenuItem key={client.id} value={client.id}>
                {client.name}
              </MenuItem>
            ))
          )}
        </Select>
      </FormControl>
      
      <FormControl fullWidth sx={{ mb: 2 }} disabled={!selectedClient}>
        <InputLabel>Interface</InputLabel>
        <Select
          value={selectedInterface?.id || ''}
          onChange={handleInterfaceChange}
          label="Interface"
          required={required}
        >
          {!interfaces || interfaces.length === 0 ? (
            <MenuItem disabled value="">
              {loading ? 'Loading interfaces...' : selectedClient ? 'No interfaces available for this client' : 'Select a client first'}
            </MenuItem>
          ) : (
            interfaces.map((interfaceObj) => (
              <MenuItem key={interfaceObj.id} value={interfaceObj.id}>
                {interfaceObj.name}
              </MenuItem>
            ))
          )}
        </Select>
      </FormControl>
      
      {required && (!clients || clients.length === 0) && !loading && (
        <Alert severity="warning">
          No clients are set up. Please <Button size="small" onClick={navigateToClientManagement}>add a client</Button> first.
        </Alert>
      )}
      
      {required && selectedClient && (!interfaces || interfaces.length === 0) && !loading && (
        <Alert severity="warning">
          No interfaces are set up for this client. Please <Button size="small" onClick={navigateToInterfaceManagement}>add an interface</Button> first.
        </Alert>
      )}
      
      {required && selectedClient && interfaces && interfaces.length > 0 && !selectedInterface && (
        <Alert severity="info">
          Please select an interface to continue
        </Alert>
      )}
    </Box>
  );
};

export default ClientInterfaceSelector; 