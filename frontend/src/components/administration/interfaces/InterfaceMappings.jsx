import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
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
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControlLabel,
  Switch,
  Grid,
  Tooltip,
  Chip,
  Alert
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  ArrowUpward as ArrowUpwardIcon,
  ArrowDownward as ArrowDownwardIcon,
  Save as SaveIcon,
  Validation as ValidationIcon
} from '@mui/icons-material';
import { interfaceService } from '../../services/interfaceService';
import { useSnackbar } from 'notistack';

const InterfaceMappings = () => {
  const { id } = useParams();
  const [interfaceData, setInterfaceData] = useState(null);
  const [mappings, setMappings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [selectedMapping, setSelectedMapping] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    xmlPath: '',
    databaseField: '',
    transformation: '',
    isRequired: false,
    defaultValue: '',
    priority: 0
  });
  const [validationResult, setValidationResult] = useState(null);
  const { enqueueSnackbar } = useSnackbar();

  useEffect(() => {
    loadInterfaceData();
    loadMappings();
  }, [id]);

  const loadInterfaceData = async () => {
    try {
      const data = await interfaceService.getInterfaceById(id);
      setInterfaceData(data);
    } catch (err) {
      setError('Failed to load interface data: ' + err.message);
      enqueueSnackbar('Failed to load interface data', { variant: 'error' });
    }
  };

  const loadMappings = async () => {
    try {
      const data = await interfaceService.getInterfaceMappings(id);
      setMappings(data);
    } catch (err) {
      setError('Failed to load mappings: ' + err.message);
      enqueueSnackbar('Failed to load mappings', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (mapping = null) => {
    if (mapping) {
      setFormData(mapping);
      setSelectedMapping(mapping);
    } else {
      setFormData({
        name: '',
        xmlPath: '',
        databaseField: '',
        transformation: '',
        isRequired: false,
        defaultValue: '',
        priority: 0
      });
      setSelectedMapping(null);
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setSelectedMapping(null);
    setFormData({
      name: '',
      xmlPath: '',
      databaseField: '',
      transformation: '',
      isRequired: false,
      defaultValue: '',
      priority: 0
    });
  };

  const handleInputChange = (e) => {
    const { name, value, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'isRequired' ? checked : value
    }));
  };

  const handleSubmit = async () => {
    try {
      const updatedMappings = [...mappings];
      if (selectedMapping) {
        const index = updatedMappings.findIndex(m => m.id === selectedMapping.id);
        updatedMappings[index] = { 
          ...formData, 
          id: selectedMapping.id,
          interfaceEntity: { id: parseInt(id) },
          client: { id: interfaceData.client.id }
        };
      } else {
        updatedMappings.push({ 
          ...formData, 
          id: Date.now(),
          interfaceEntity: { id: parseInt(id) },
          client: { id: interfaceData.client.id }
        });
      }
      
      // Ensure all mappings have proper relationships set
      const preparedMappings = updatedMappings.map(mapping => ({
        ...mapping,
        interfaceEntity: { id: parseInt(id) },
        client: { id: interfaceData.client.id },
        isActive: true,
        xmlPath: mapping.xmlPath || mapping.sourceField,
        databaseField: mapping.databaseField || mapping.targetField,
        sourceField: mapping.sourceField || mapping.xmlPath,
        targetField: mapping.targetField || mapping.databaseField
      }));

      await interfaceService.updateInterfaceMappings(id, preparedMappings);
      setMappings(preparedMappings);
      handleCloseDialog();
      enqueueSnackbar('Mapping saved successfully', { variant: 'success' });
    } catch (err) {
      enqueueSnackbar('Failed to save mapping: ' + err.message, { variant: 'error' });
    }
  };

  const handleDelete = async (mappingId) => {
    if (window.confirm('Are you sure you want to delete this mapping?')) {
      try {
        const updatedMappings = mappings.filter(m => m.id !== mappingId);
        await interfaceService.updateInterfaceMappings(id, updatedMappings);
        setMappings(updatedMappings);
        enqueueSnackbar('Mapping deleted successfully', { variant: 'success' });
      } catch (err) {
        enqueueSnackbar('Failed to delete mapping: ' + err.message, { variant: 'error' });
      }
    }
  };

  const handleMoveMapping = async (index, direction) => {
    const newMappings = [...mappings];
    const newIndex = direction === 'up' ? index - 1 : index + 1;
    
    if (newIndex >= 0 && newIndex < newMappings.length) {
      const temp = newMappings[index];
      newMappings[index] = newMappings[newIndex];
      newMappings[newIndex] = temp;
      
      // Update priorities
      newMappings.forEach((mapping, i) => {
        mapping.priority = i;
      });
      
      try {
        await interfaceService.updateInterfaceMappings(id, newMappings);
        setMappings(newMappings);
      } catch (err) {
        enqueueSnackbar('Failed to update mapping order: ' + err.message, { variant: 'error' });
      }
    }
  };

  const handleValidate = async () => {
    try {
      const result = await interfaceService.validateInterface(id);
      setValidationResult(result);
      if (result.valid) {
        enqueueSnackbar('Interface validation successful', { variant: 'success' });
      } else {
        enqueueSnackbar('Interface validation failed: ' + result.message, { variant: 'error' });
      }
    } catch (err) {
      enqueueSnackbar('Failed to validate interface: ' + err.message, { variant: 'error' });
    }
  };

  if (loading) return <Typography>Loading mappings...</Typography>;
  if (error) return <Typography color="error">{error}</Typography>;

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">
          Mapping Rules for {interfaceData?.name}
        </Typography>
        <Box>
          <Tooltip title="Validate Interface">
            <IconButton onClick={handleValidate} color="primary">
              <ValidationIcon />
            </IconButton>
          </Tooltip>
          <Button
            variant="contained"
            color="primary"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
          >
            Add Mapping Rule
          </Button>
        </Box>
      </Box>

      {validationResult && (
        <Alert severity={validationResult.valid ? "success" : "error"} sx={{ mb: 2 }}>
          {validationResult.message}
        </Alert>
      )}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Priority</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>XML Path</TableCell>
              <TableCell>Database Field</TableCell>
              <TableCell>Required</TableCell>
              <TableCell>Default Value</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {mappings.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  No mapping rules found. Add your first mapping rule to get started.
                </TableCell>
              </TableRow>
            ) : (
              mappings.map((mapping, index) => (
                <TableRow key={mapping.id}>
                  <TableCell>
                    <Box display="flex" alignItems="center">
                      {index > 0 && (
                        <IconButton
                          size="small"
                          onClick={() => handleMoveMapping(index, 'up')}
                        >
                          <ArrowUpwardIcon />
                        </IconButton>
                      )}
                      {index < mappings.length - 1 && (
                        <IconButton
                          size="small"
                          onClick={() => handleMoveMapping(index, 'down')}
                        >
                          <ArrowDownwardIcon />
                        </IconButton>
                      )}
                      {mapping.priority}
                    </Box>
                  </TableCell>
                  <TableCell>{mapping.name}</TableCell>
                  <TableCell>{mapping.xmlPath}</TableCell>
                  <TableCell>{mapping.databaseField}</TableCell>
                  <TableCell>
                    <Chip
                      label={mapping.isRequired ? "Required" : "Optional"}
                      color={mapping.isRequired ? "error" : "default"}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{mapping.defaultValue || '-'}</TableCell>
                  <TableCell>
                    <IconButton onClick={() => handleOpenDialog(mapping)}>
                      <EditIcon />
                    </IconButton>
                    <IconButton onClick={() => handleDelete(mapping.id)} color="error">
                      <DeleteIcon />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {selectedMapping ? 'Edit Mapping Rule' : 'Create New Mapping Rule'}
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
              label="XML Path"
              name="xmlPath"
              value={formData.xmlPath}
              onChange={handleInputChange}
              fullWidth
              required
            />
            <TextField
              label="Database Field"
              name="databaseField"
              value={formData.databaseField}
              onChange={handleInputChange}
              fullWidth
              required
            />
            <TextField
              label="Transformation"
              name="transformation"
              value={formData.transformation}
              onChange={handleInputChange}
              fullWidth
              helperText="Optional transformation rule (e.g., date format, number format)"
            />
            <TextField
              label="Default Value"
              name="defaultValue"
              value={formData.defaultValue}
              onChange={handleInputChange}
              fullWidth
              helperText="Value to use if XML path is not found"
            />
            <FormControlLabel
              control={
                <Switch
                  name="isRequired"
                  checked={formData.isRequired}
                  onChange={handleInputChange}
                />
              }
              label="Required Field"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button
            onClick={handleSubmit}
            variant="contained"
            color="primary"
            startIcon={<SaveIcon />}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default InterfaceMappings; 