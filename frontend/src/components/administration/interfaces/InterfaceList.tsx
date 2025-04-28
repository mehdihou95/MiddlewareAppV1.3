import React, { useState, useEffect } from 'react';
import { 
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, 
  Paper, Button, Typography, Box, Chip, IconButton, Tooltip,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  MenuItem, FormControlLabel, Switch, Alert
} from '@mui/material';
import {
  Delete as DeleteIcon,
  Edit as EditIcon,
  Visibility as ViewIcon, 
  Settings as SettingsIcon,
  Add as AddIcon
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { interfaceService } from '../../../services/administration/interfaceService';
import { useSnackbar } from 'notistack';
import { Interface } from '../../../services/administration/types';
import { useClientInterface } from '../../../context/ClientInterfaceContext';

interface FormData {
  name: string;
  type: string;
  description: string;
  schemaPath: string;
  rootElement: string;
  namespace: string;
  isActive: boolean;
  priority: number;
  status: string;
}

const DEFAULT_FORM_DATA: FormData = {
  name: '',
  type: 'XML',
  description: '',
  schemaPath: '',
  rootElement: '',
  namespace: '',
  isActive: true,
  priority: 0,
  status: 'ACTIVE'
};

const InterfaceList: React.FC = () => {
  const { selectedClient, refreshInterfaces, interfaces: contextInterfaces } = useClientInterface();
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState<boolean>(false);
  const [selectedInterface, setSelectedInterface] = useState<Interface | null>(null);
  const [formData, setFormData] = useState<FormData>(DEFAULT_FORM_DATA);
  
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  
  const handleError = (error: any, action: string) => {
    const errorMessage = `Failed to ${action}: ${error.message || 'Unknown error'}`;
    console.error(errorMessage, error);
    setError(errorMessage);
    enqueueSnackbar(errorMessage, { variant: 'error' });
  };
  
  const handleSuccess = (action: string) => {
    const successMessage = `Interface ${action} successfully`;
    enqueueSnackbar(successMessage, { variant: 'success' });
  };
  
  const loadInterfaces = async () => {
    if (!selectedClient) {
      setError('Please select a client first');
      return;
    }

    try {
      setLoading(true);
      await refreshInterfaces();
      setError(null);
    } catch (err: any) {
      handleError(err, 'load interfaces');
    } finally {
      setLoading(false);
    }
  };
  
  useEffect(() => {
    if (selectedClient) {
      loadInterfaces();
    }
  }, [selectedClient, refreshInterfaces]);
  
  const handleOpenDialog = (interfaceData?: Interface) => {
    if (interfaceData) {
      setFormData({
        name: interfaceData.name,
        type: interfaceData.type,
        description: interfaceData.description || '',
        schemaPath: interfaceData.schemaPath || '',
        rootElement: interfaceData.rootElement,
        namespace: interfaceData.namespace || '',
        isActive: interfaceData.isActive,
        priority: interfaceData.priority || 0,
        status: interfaceData.status || 'ACTIVE'
      });
      setSelectedInterface(interfaceData);
    } else {
      setFormData(DEFAULT_FORM_DATA);
      setSelectedInterface(null);
    }
    setOpenDialog(true);
  };
  
  const handleCloseDialog = () => {
    setOpenDialog(false);
    setSelectedInterface(null);
    setFormData(DEFAULT_FORM_DATA);
  };
  
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'isActive' ? checked : value
    }));
  };
  
  const handleSubmit = async () => {
    if (!selectedClient) {
      enqueueSnackbar('Please select a client first', { variant: 'error' });
      return;
    }

    try {
      const interfaceData: Omit<Interface, 'id'> = {
        name: formData.name,
        type: formData.type,
        description: formData.description,
        schemaPath: formData.schemaPath,
        rootElement: formData.rootElement,
        namespace: formData.namespace,
        isActive: formData.isActive,
        priority: formData.priority,
        status: formData.status,
        client: {
          id: selectedClient.id
        }
      };

      if (selectedInterface) {
        await interfaceService.updateInterface(selectedInterface.id, interfaceData);
        handleSuccess('updated');
      } else {
        await interfaceService.createInterface(interfaceData);
        handleSuccess('created');
      }
      handleCloseDialog();
      await refreshInterfaces();
    } catch (err: any) {
      handleError(err, selectedInterface ? 'update interface' : 'create interface');
    }
  };
  
  const handleDelete = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this interface?')) {
      try {
        setLoading(true);
        await interfaceService.deleteInterface(id);
        
        const updatedInterfaces = contextInterfaces.filter(item => item.id !== id);
        setLoading(false);
        
        await refreshInterfaces();
        handleSuccess('deleted');
      } catch (err: any) {
        setLoading(false);
        handleError(err, 'delete interface');
      }
    }
  };
  
  const handleViewMappings = (id: number) => {
    navigate(`/interfaces/${id}/mappings`);
  };
  
  if (!selectedClient) {
    return (
      <Alert severity="info">
        Please select a client to manage interfaces
      </Alert>
    );
  }

  if (loading) {
    return <Typography>Loading interfaces...</Typography>;
  }

  if (error) {
    return <Typography color="error">{error}</Typography>;
  }
  
  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Interface Management</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          Create New Interface
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Root Element</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Priority</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {contextInterfaces.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  <Typography>No interfaces found. Click "Create New Interface" to add one.</Typography>
                </TableCell>
              </TableRow>
            ) : (
              contextInterfaces.map((interfaceItem) => (
                <TableRow key={interfaceItem.id}>
                  <TableCell>{interfaceItem.name}</TableCell>
                  <TableCell>{interfaceItem.type}</TableCell>
                  <TableCell>{interfaceItem.rootElement}</TableCell>
                  <TableCell>
                    <Chip 
                      label={interfaceItem.isActive ? 'Active' : 'Inactive'} 
                      color={interfaceItem.isActive ? 'success' : 'default'} 
                      size="small" 
                    />
                  </TableCell>
                  <TableCell>{interfaceItem.priority}</TableCell>
                  <TableCell>
                    <Tooltip title="Edit Interface">
                      <IconButton onClick={() => handleOpenDialog(interfaceItem)} size="small">
                        <EditIcon />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="View Mappings">
                      <IconButton onClick={() => handleViewMappings(interfaceItem.id)} size="small">
                        <SettingsIcon />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Delete Interface">
                      <IconButton onClick={() => handleDelete(interfaceItem.id)} size="small">
                        <DeleteIcon />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {selectedInterface ? 'Edit Interface' : 'Create New Interface'}
        </DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={2}>
            <TextField
              label="Name"
              name="name"
              value={formData.name}
              onChange={handleInputChange}
              fullWidth
              required
            />
            <TextField
              label="Type"
              name="type"
              value={formData.type}
              onChange={handleInputChange}
              fullWidth
              required
              select
            >
              <MenuItem value="XML">XML</MenuItem>
              <MenuItem value="JSON">JSON</MenuItem>
              <MenuItem value="CSV">CSV</MenuItem>
            </TextField>
            <TextField
              label="Description"
              name="description"
              value={formData.description}
              onChange={handleInputChange}
              fullWidth
              multiline
              rows={2}
            />
            <TextField
              label="Schema Path"
              name="schemaPath"
              value={formData.schemaPath}
              onChange={handleInputChange}
              fullWidth
            />
            <TextField
              label="Root Element"
              name="rootElement"
              value={formData.rootElement}
              onChange={handleInputChange}
              fullWidth
              required
            />
            <TextField
              label="Namespace"
              name="namespace"
              value={formData.namespace}
              onChange={handleInputChange}
              fullWidth
            />
            <TextField
              label="Priority"
              name="priority"
              type="number"
              value={formData.priority}
              onChange={handleInputChange}
              fullWidth
            />
            <FormControlLabel
              control={
                <Switch
                  checked={formData.isActive}
                  onChange={handleInputChange}
                  name="isActive"
                />
              }
              label="Active"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained" color="primary">
            {selectedInterface ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default InterfaceList; 