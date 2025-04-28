import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  IconButton,
  Alert,
  CircularProgress,
  Chip,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Tooltip,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { interfaceService } from '../../services/administration/interfaceService';
import { Interface } from '../../services/administration/types';
import { useClientInterface } from '../../context/ClientInterfaceContext';
import ClientInterfaceSelector from '../../components/core/ClientInterfaceSelector';
import { useAuth } from '../../context/AuthContext';

interface FormData {
  name: string;
  type: string;
  description: string;
  rootElement: string;
  namespace: string;
  schemaPath: string;
  isActive: boolean;
  priority: number;
  status: string;
}

interface FormErrors {
  name: string;
  type: string;
}

const InterfaceManagementPage: React.FC = () => {
  const { 
    selectedClient, 
    refreshInterfaces, 
    interfaces: contextInterfaces,
    setInterfaces 
  } = useClientInterface();
  const { isAuthenticated, loading: authLoading } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingInterface, setEditingInterface] = useState<Interface | null>(null);
  const [formData, setFormData] = useState<FormData>({
    name: '',
    type: 'XML',
    description: '',
    rootElement: '',
    namespace: '',
    schemaPath: '',
    isActive: true,
    priority: 1,
    status: 'ACTIVE'
  });
  const [formErrors, setFormErrors] = useState<FormErrors>({
    name: '',
    type: '',
  });
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [interfaceToDelete, setInterfaceToDelete] = useState<Interface | null>(null);

  // Load interfaces when client changes
  useEffect(() => {
    const loadData = async () => {
      if (!selectedClient || !isAuthenticated) {
        setLoading(false);
        return;
      }
      
      try {
        console.log('Loading interfaces for client:', selectedClient.id);
        setLoading(true);
        setError(null);
        
        // First try to refresh interfaces
        await refreshInterfaces();
        
        // If no error occurred but interfaces are empty, show appropriate message
        if (contextInterfaces.length === 0) {
          setError(null); // Clear any previous errors
        }
      } catch (err: any) {
        console.error('Error loading interfaces:', err);
        // Only set error if it's a real error, not just empty interfaces
        if (err.response?.status !== 200) {
          setError(err.message || 'Failed to load interfaces. Please try again.');
        }
      } finally {
        setLoading(false);
      }
    };

    if (!authLoading) {
      loadData();
    }
  }, [selectedClient, isAuthenticated, authLoading, refreshInterfaces, contextInterfaces.length]);

  const handleOpenDialog = (interfaceObj?: Interface) => {
    if (interfaceObj) {
      setEditingInterface(interfaceObj);
      setFormData({
        name: interfaceObj.name,
        type: interfaceObj.type,
        description: interfaceObj.description || '',
        isActive: interfaceObj.isActive,
        priority: interfaceObj.priority || 0,
        rootElement: interfaceObj.rootElement || '',
        namespace: interfaceObj.namespace || '',
        schemaPath: interfaceObj.schemaPath || '',
        status: interfaceObj.status || 'ACTIVE'
      });
    } else {
      setEditingInterface(null);
      setFormData({
        name: '',
        type: 'XML',
        description: '',
        rootElement: '',
        namespace: '',
        schemaPath: '',
        isActive: true,
        priority: 1,
        status: 'ACTIVE'
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingInterface(null);
    setFormData({
      name: '',
      type: 'XML',
      description: '',
      rootElement: '',
      namespace: '',
      schemaPath: '',
      isActive: true,
      priority: 1,
      status: 'ACTIVE'
    });
    setFormErrors({
      name: '',
      type: '',
    });
  };

  const validateForm = () => {
    const errors = {
      name: '',
      type: '',
    };
    let isValid = true;

    if (!formData.name.trim()) {
      errors.name = 'Name is required';
      isValid = false;
    }

    if (!formData.type) {
      errors.type = 'Type is required';
      isValid = false;
    }

    setFormErrors(errors);
    return isValid;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm() || !selectedClient) return;

    try {
      setLoading(true);
      const backendInterfaceData: Omit<Interface, 'id'> = {
        name: formData.name,
        type: formData.type,
        description: formData.description || '',
        rootElement: formData.rootElement || formData.name,
        namespace: formData.namespace || `http://xml.processor.com/${formData.type.toLowerCase()}`,
        schemaPath: formData.schemaPath || '',
        isActive: true,
        priority: formData.priority || 0,
        status: formData.status,
        client: {
          id: selectedClient.id
        }
      };

      if (editingInterface) {
        await interfaceService.updateInterface(editingInterface.id, backendInterfaceData);
        console.log('Interface updated successfully');
      } else {
        await interfaceService.createInterface(backendInterfaceData);
        console.log('Interface created successfully');
      }

      await refreshInterfaces();
      handleCloseDialog();
      setError(null);
    } catch (err: any) {
      console.error('Error saving interface:', err);
      setError(err.message || 'Failed to save interface. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteClick = (interfaceObj: Interface) => {
    setInterfaceToDelete(interfaceObj);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!interfaceToDelete) return;

    try {
      setLoading(true);
      
      // Optimistic update - remove from UI immediately
      const updatedInterfaces = contextInterfaces.filter(item => item.id !== interfaceToDelete.id);
      setInterfaces(updatedInterfaces);
      
      // Perform the deletion
      await interfaceService.deleteInterface(interfaceToDelete.id);
      
      // Refresh to ensure sync with backend
      await refreshInterfaces();
      
      setDeleteDialogOpen(false);
      setInterfaceToDelete(null);
      setError(null);
    } catch (err: any) {
      console.error('Error deleting interface:', err);
      setError(err.message || 'Failed to delete interface');
      // Refresh to restore state in case of error
      await refreshInterfaces();
    } finally {
      setLoading(false);
    }
  };

  if (authLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
        <CircularProgress />
      </Box>
    );
  }

  if (!isAuthenticated) {
    return (
      <Box p={3}>
        <Alert severity="warning">Please log in to view interfaces.</Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Interface Management</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
          disabled={!selectedClient || loading}
        >
          Add Interface
        </Button>
      </Box>

      <ClientInterfaceSelector required />

      {!selectedClient ? (
        <Alert severity="info" sx={{ mt: 2 }}>
          Please select a client to manage interfaces
        </Alert>
      ) : (
        <>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
              {loading && ' Retrying...'}
            </Alert>
          )}

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', p: 3 }}>
              <CircularProgress />
              <Typography sx={{ ml: 2 }}>Loading interfaces...</Typography>
            </Box>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {contextInterfaces.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={5} align="center">
                        <Typography sx={{ py: 2 }}>
                          No interfaces found for {selectedClient.name}. Click "Add Interface" to create one.
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    contextInterfaces.map((interfaceObj) => (
                      <TableRow key={interfaceObj.id}>
                        <TableCell>{interfaceObj.name}</TableCell>
                        <TableCell>{interfaceObj.type}</TableCell>
                        <TableCell>{interfaceObj.description || '-'}</TableCell>
                        <TableCell>
                          <Chip
                            label={interfaceObj.isActive ? 'Active' : 'Inactive'}
                            color={interfaceObj.isActive ? 'success' : 'default'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Tooltip title="Edit">
                            <IconButton
                              size="small"
                              onClick={() => handleOpenDialog(interfaceObj)}
                            >
                              <EditIcon />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Delete">
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => handleDeleteClick(interfaceObj)}
                            >
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
          )}

          {/* Create/Edit Dialog */}
          <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
            <form onSubmit={handleSubmit} noValidate>
              <DialogTitle>
                {editingInterface ? 'Edit Interface' : 'Add New Interface'}
              </DialogTitle>
              <DialogContent>
                <Box sx={{ pt: 2 }}>
                  <TextField
                    fullWidth
                    label="Name"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    error={!!formErrors.name}
                    helperText={formErrors.name}
                    sx={{ mb: 2 }}
                  />
                  <FormControl fullWidth error={!!formErrors.type} sx={{ mb: 2 }}>
                    <InputLabel>Type</InputLabel>
                    <Select
                      value={formData.type}
                      label="Type"
                      onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                    >
                      <MenuItem value="ASN">ASN</MenuItem>
                      <MenuItem value="ORDER">ORDER</MenuItem>
                      <MenuItem value="XML">XML</MenuItem>
                      <MenuItem value="JSON">JSON</MenuItem>
                      <MenuItem value="CSV">CSV</MenuItem>
                      <MenuItem value="EDI">EDI</MenuItem>
                    </Select>
                    {formErrors.type && (
                      <Typography color="error" variant="caption">
                        {formErrors.type}
                      </Typography>
                    )}
                  </FormControl>
                  <TextField
                    fullWidth
                    label="Description"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    multiline
                    rows={3}
                    sx={{ mb: 2 }}
                  />
                  <TextField
                    fullWidth
                    label="Root Element"
                    value={formData.rootElement}
                    onChange={(e) => setFormData({ ...formData, rootElement: e.target.value })}
                    sx={{ mb: 2 }}
                  />
                  <TextField
                    fullWidth
                    label="Namespace"
                    value={formData.namespace}
                    onChange={(e) => setFormData({ ...formData, namespace: e.target.value })}
                    sx={{ mb: 2 }}
                  />
                  <TextField
                    fullWidth
                    label="Schema Path"
                    value={formData.schemaPath}
                    onChange={(e) => setFormData({ ...formData, schemaPath: e.target.value })}
                    sx={{ mb: 2 }}
                  />
                  <FormControl fullWidth sx={{ mb: 2 }}>
                    <InputLabel>Status</InputLabel>
                    <Select
                      value={formData.status}
                      label="Status"
                      onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                    >
                      <MenuItem value="ACTIVE">Active</MenuItem>
                      <MenuItem value="INACTIVE">Inactive</MenuItem>
                    </Select>
                  </FormControl>
                  <TextField
                    fullWidth
                    type="number"
                    label="Priority"
                    value={formData.priority}
                    onChange={(e) => setFormData({ ...formData, priority: parseInt(e.target.value) || 0 })}
                    sx={{ mb: 2 }}
                  />
                </Box>
              </DialogContent>
              <DialogActions>
                <Button onClick={handleCloseDialog}>Cancel</Button>
                <Button type="submit" variant="contained">
                  {editingInterface ? 'Update' : 'Create'}
                </Button>
              </DialogActions>
            </form>
          </Dialog>

          {/* Delete Confirmation Dialog */}
          <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
            <DialogTitle>Confirm Delete</DialogTitle>
            <DialogContent>
              <Typography>
                Are you sure you want to delete the interface "{interfaceToDelete?.name}"?
                This action cannot be undone.
              </Typography>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setDeleteDialogOpen(false)}>Cancel</Button>
              <Button onClick={handleDeleteConfirm} color="error" variant="contained">
                Delete
              </Button>
            </DialogActions>
          </Dialog>
        </>
      )}
    </Box>
  );
};

export default InterfaceManagementPage; 