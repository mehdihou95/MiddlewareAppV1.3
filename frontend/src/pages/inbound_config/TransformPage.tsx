import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  List,
  ListItem,
  ListItemText,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Snackbar,
  Alert,
  CircularProgress,
  FormControlLabel,
  Switch,
  Grid,
  IconButton,
  Tooltip,
  Chip,
  Divider,
  Card,
  CardContent,
  FormControl,
  InputLabel,
  Select,
  MenuItem
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import SearchIcon from '@mui/icons-material/Search';
import DeleteIcon from '@mui/icons-material/Delete';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import InfoIcon from '@mui/icons-material/Info';
import { useClientInterface } from '../../context/ClientInterfaceContext';
import ClientInterfaceSelector from '../../components/core/ClientInterfaceSelector';
import { XsdElement, MappingRule, DatabaseField } from '../../services/inbound_config/types';
import { xsdService, XsdNamespace } from '../../services/inbound_config/xsdService';
import { mappingRuleService } from '../../services/inbound_config/mappingRuleService';
import XmlElementTree from '../../components/inbound_config/XmlElementTree';
import DatabaseFieldTree from '../../components/inbound_config/DatabaseFieldTree';

interface SnackbarState {
  open: boolean;
  message: string;
  severity: 'success' | 'error' | 'info' | 'warning';
}

const TransformPage: React.FC = () => {
  const { selectedClient, selectedInterface } = useClientInterface();
  const [xsdElements, setXsdElements] = useState<XsdElement[]>([]);
  const [dbFields, setDbFields] = useState<DatabaseField[]>([]);
  const [selectedXsdElement, setSelectedXsdElement] = useState<XsdElement | null>(null);
  const [selectedDbField, setSelectedDbField] = useState('');
  const [mappingRules, setMappingRules] = useState<MappingRule[]>([]);
  const [openDialog, setOpenDialog] = useState(false);
  const [newMapping, setNewMapping] = useState<Omit<MappingRule, 'id'>>({
    clientId: 0,
    interfaceId: 0,
    name: '',
    xmlPath: '',
    databaseField: '',
    xsdElement: '',
    tableName: '',
    dataType: '',
    isAttribute: false,
    description: '',
    required: false,
    defaultValue: '',
    priority: 0,
    transformationRule: '',
    isActive: true,
    sourceField: '',
    targetField: '',
    targetLevel: 'HEADER'
  });
  const [snackbar, setSnackbar] = useState<SnackbarState>({
    open: false,
    message: '',
    severity: 'info'
  });
  const [loadingStates, setLoadingStates] = useState({
    xsdStructure: false,
    mappingRules: false,
    databaseFields: false
  });
  const [mappingSearchTerm, setMappingSearchTerm] = useState('');
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileAnalysisLoading, setFileAnalysisLoading] = useState(false);

  const setLoadingState = (key: keyof typeof loadingStates, value: boolean) => {
    setLoadingStates(prev => ({ ...prev, [key]: value }));
  };

  const isLoading = Object.values(loadingStates).some(Boolean);

  useEffect(() => {
    if (selectedClient && selectedInterface) {
      loadXsdStructure();
      loadMappingRules();
      loadDatabaseFields();
    }
  }, [selectedClient, selectedInterface]);

  const loadXsdStructure = async () => {
    if (!selectedInterface) return;
    
    try {
      setLoadingState('xsdStructure', true);
      const elements = await xsdService.getXsdStructureById(selectedInterface.id);
      setXsdElements(elements);
    } catch (error) {
      setSnackbar({
        open: true,
        message: error instanceof Error ? error.message : 'Failed to load XSD structure',
        severity: 'error'
      });
    } finally {
      setLoadingState('xsdStructure', false);
    }
  };

  const loadDatabaseFields = async () => {
    if (!selectedClient || !selectedInterface) return;

    try {
      setLoadingState('databaseFields', true);
      const fields = await xsdService.getDatabaseFields(selectedClient.id, selectedInterface.id);
      setDbFields(fields);
    } catch (error) {
      setSnackbar({
        open: true,
        message: error instanceof Error ? error.message : 'Failed to load database fields',
        severity: 'error'
      });
    } finally {
      setLoadingState('databaseFields', false);
    }
  };

  const loadMappingRules = async () => {
    if (!selectedClient || !selectedInterface) return;

    try {
      setLoadingState('mappingRules', true);
      console.debug('Loading mapping rules for client:', selectedClient.id, 'interface:', selectedInterface.id);
      const response = await mappingRuleService.getAllMappingRules(
        0, // page
        100, // size
        'name', // sortBy
        'asc', // direction
        selectedClient.id,
        selectedInterface.id,
        true // isActive
      );
      console.debug('Mapping rules loaded:', response.content);
      setMappingRules(response.content);
    } catch (error) {
      console.error('Failed to load mapping rules:', error);
      setSnackbar({
        open: true,
        message: error instanceof Error ? error.message : 'Failed to load mapping rules',
        severity: 'error'
      });
    } finally {
      setLoadingState('mappingRules', false);
    }
  };

  const handleXsdElementClick = (element: XsdElement, fullPath: string) => {
    setSelectedXsdElement({
      ...element,
      name: fullPath
    });
  };

  const handleDbFieldClick = (field: string) => {
    setSelectedDbField(field);
    if (selectedXsdElement && selectedClient && selectedInterface) {
      const isAttribute = false;
      const [tableName, fieldName] = field.split('.');
      const dbField = dbFields.find(f => f.field === field);
      
      setNewMapping({
        clientId: selectedClient.id,
        interfaceId: selectedInterface.id,
        xmlPath: selectedXsdElement.name,
        databaseField: fieldName,
        xsdElement: selectedXsdElement.name.split('/').pop() || '',
        tableName: tableName,
        dataType: dbField?.type || 'String',
        isAttribute: isAttribute,
        description: `Map ${selectedXsdElement.name} to ${fieldName}`,
        name: `${selectedXsdElement.name.split('/').pop()}_to_${fieldName}`,
        required: false,
        defaultValue: '',
        priority: 0,
        transformationRule: '',
        isActive: true,
        sourceField: selectedXsdElement.name,
        targetField: fieldName,
        targetLevel: tableName === 'ASN_HEADERS' ? 'HEADER' : 'LINE'
      });
      setOpenDialog(true);
    }
  };

  const validateMappingRule = (rule: MappingRule): string | null => {
    // Get the interface type from the selected interface
    const interfaceType = selectedInterface?.type;
    
    // Validate target level and table name based on interface type
    if (interfaceType === 'ORDER') {
      if (rule.targetLevel === 'HEADER' && rule.tableName !== 'ORDER_HEADERS') {
        return 'Header level mappings must use ORDER_HEADERS table';
      }
      if (rule.targetLevel === 'LINE' && rule.tableName !== 'ORDER_LINES') {
        return 'Line level mappings must use ORDER_LINES table';
      }
    } else if (interfaceType === 'ASN') {
      if (rule.targetLevel === 'HEADER' && rule.tableName !== 'ASN_HEADERS') {
        return 'Header level mappings must use ASN_HEADERS table';
      }
      if (rule.targetLevel === 'LINE' && rule.tableName !== 'ASN_LINES') {
        return 'Line level mappings must use ASN_LINES table';
      }
    }

    return null;
  };

  const handleSaveMapping = async () => {
    if (!selectedClient || !selectedInterface) {
      setSnackbar({
        open: true,
        message: 'Please select a client and interface first',
        severity: 'error'
      });
      return;
    }

    try {
      if (!newMapping.xmlPath || !newMapping.databaseField) {
        setSnackbar({
          open: true,
          message: 'Please select both XML element and database field',
          severity: 'error'
        });
        return;
      }

      // Validate the mapping rule
      const validationError = validateMappingRule(newMapping);
      if (validationError) {
        setSnackbar({
          open: true,
          message: validationError,
          severity: 'error'
        });
        return;
      }

      const savedRule = await mappingRuleService.createMappingRule(newMapping);
      setMappingRules([...mappingRules, savedRule]);
      setOpenDialog(false);
      setSelectedXsdElement(null);
      setSelectedDbField('');
      setSnackbar({
        open: true,
        message: 'Mapping rule saved successfully',
        severity: 'success'
      });
    } catch (error) {
      setSnackbar({
        open: true,
        message: error instanceof Error ? error.message : 'Failed to save mapping rule',
        severity: 'error'
      });
    }
  };

  const handleDeleteMapping = async (id: number) => {
    try {
      await mappingRuleService.deleteMappingRule(id);
      setMappingRules(mappingRules.filter(rule => rule.id !== id));
      setSnackbar({
        open: true,
        message: 'Mapping rule deleted successfully',
        severity: 'success'
      });
    } catch (error) {
      setSnackbar({
        open: true,
        message: error instanceof Error ? error.message : 'Failed to delete mapping rule',
        severity: 'error'
      });
    }
  };

  const handleRefreshXsd = async () => {
    if (selectedInterface?.id) {
      try {
        setSnackbar({
          open: true,
          message: 'Refreshing XSD structure...',
          severity: 'info'
        });
        await loadXsdStructure();
        setSnackbar({
          open: true,
          message: 'XSD structure refreshed successfully',
          severity: 'success'
        });
      } catch (error) {
        setSnackbar({
          open: true,
          message: error instanceof Error ? error.message : 'Failed to refresh XSD structure',
          severity: 'error'
        });
      }
    }
  };

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files.length > 0) {
      setSelectedFile(event.target.files[0]);
    }
  };

  const handleAnalyzeXsdFile = async () => {
    if (!selectedFile) {
      setSnackbar({
        open: true,
        message: 'Please select a file first',
        severity: 'error'
      });
      return;
    }

    try {
      setFileAnalysisLoading(true);
      const result = await xsdService.analyzeXsdFile(selectedFile);
      
      // Show analysis results
      setSnackbar({
        open: true,
        message: `XSD analysis complete. Root element: ${result.rootElement}`,
        severity: 'success'
      });
      
      // Update the UI with the analysis results
      setXsdElements(result.structure);
      
      // Close the dialog
      setUploadDialogOpen(false);
    } catch (error) {
      setSnackbar({
        open: true,
        message: error instanceof Error ? error.message : 'Failed to analyze XSD file',
        severity: 'error'
      });
    } finally {
      setFileAnalysisLoading(false);
    }
  };

  const filteredMappingRules = mappingRules.filter(rule => {
    if (!mappingSearchTerm) return true;
    const searchTerm = mappingSearchTerm.toLowerCase();
    return (
      rule.name?.toLowerCase().includes(searchTerm) ||
      rule.xmlPath.toLowerCase().includes(searchTerm) ||
      rule.databaseField.toLowerCase().includes(searchTerm) ||
      rule.description?.toLowerCase().includes(searchTerm) ||
      rule.tableName?.toLowerCase().includes(searchTerm)
    );
  });

  const renderMappingRule = (rule: MappingRule) => (
    <ListItem
      key={rule.id}
      sx={{
        borderBottom: '1px solid',
        borderColor: 'divider',
        '&:hover': {
          backgroundColor: 'action.hover',
        },
      }}
    >
      <Grid container spacing={2} alignItems="center">
        <Grid item xs={1}>
          <Typography variant="body2" color="text.secondary">
            {rule.priority || 0}
          </Typography>
        </Grid>
        <Grid item xs={3}>
          <Tooltip title={rule.xmlPath}>
            <Typography variant="body2" noWrap>
              {rule.xmlPath.replace(/\*\[local-name\(\)='/g, '').replace(/'\]/g, '')}
            </Typography>
          </Tooltip>
        </Grid>
        <Grid item xs={3}>
          <Typography variant="body2" noWrap title={`${rule.tableName}.${rule.databaseField}`}>
            {rule.tableName}.{rule.databaseField}
          </Typography>
        </Grid>
        <Grid item xs={3}>
          <Typography variant="body2" color="text.secondary" noWrap title={rule.description || ''}>
            {rule.description || 'No description'}
          </Typography>
        </Grid>
        <Grid item xs={2}>
          <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
            {rule.required && (
              <Tooltip title="Required Field">
                <Box component="span" sx={{ 
                  width: 8, 
                  height: 8, 
                  borderRadius: '50%', 
                  bgcolor: 'error.main',
                  display: 'inline-block'
                }} />
              </Tooltip>
            )}
            {!rule.isActive && (
              <Tooltip title="Inactive">
                <Box component="span" sx={{ 
                  width: 8, 
                  height: 8, 
                  borderRadius: '50%', 
                  bgcolor: 'text.disabled',
                  display: 'inline-block'
                }} />
              </Tooltip>
            )}
            <IconButton
              size="small"
              onClick={() => handleDeleteMapping(rule.id!)}
              color="error"
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Box>
        </Grid>
      </Grid>
    </ListItem>
  );

  const handleCloseSnackbar = () => {
    setSnackbar(prev => ({ ...prev, open: false }));
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', p: 3, gap: 2 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">XML to Database Mapping</Typography>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button
            variant="outlined"
            color="primary"
            onClick={() => setUploadDialogOpen(true)}
            startIcon={<UploadFileIcon />}
          >
            Upload XSD
          </Button>
          <Button
            variant="outlined"
            color="primary"
            onClick={handleRefreshXsd}
            startIcon={<RefreshIcon />}
            disabled={!selectedClient || !selectedInterface}
          >
            Refresh XSD
          </Button>
        </Box>
      </Box>

      <ClientInterfaceSelector required />

      {!selectedClient || !selectedInterface ? (
        <Alert severity="info">
          Please select a client and interface to view and manage mapping rules
        </Alert>
      ) : isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <Paper sx={{ 
                p: 2, 
                height: '60vh',
                display: 'flex',
                flexDirection: 'column'
              }}>
                <Typography variant="h6" gutterBottom>XML Structure</Typography>
                <Box sx={{ flex: 1, overflow: 'hidden' }}>
                  <XmlElementTree
                    elements={xsdElements}
                    onElementClick={handleXsdElementClick}
                    selectedElement={selectedXsdElement}
                  />
                </Box>
              </Paper>
            </Grid>
            
            <Grid item xs={6}>
              <Paper sx={{ 
                p: 2, 
                height: '60vh',
                display: 'flex',
                flexDirection: 'column'
              }}>
                <Typography variant="h6" gutterBottom>Database Fields</Typography>
                <Box sx={{ flex: 1, overflow: 'hidden' }}>
                  <DatabaseFieldTree
                    fields={dbFields}
                    onFieldClick={(field) => handleDbFieldClick(field.field)}
                    selectedField={selectedDbField}
                    interfaceType={selectedInterface?.type}
                  />
                </Box>
              </Paper>
            </Grid>
          </Grid>

          <Paper sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6">Mapping Rules</Typography>
              <TextField
                size="small"
                placeholder="Search mappings..."
                value={mappingSearchTerm}
                onChange={(e) => setMappingSearchTerm(e.target.value)}
                sx={{ width: 300 }}
                InputProps={{
                  startAdornment: <SearchIcon color="action" sx={{ mr: 1 }} />,
                }}
              />
            </Box>
            <List sx={{ maxHeight: '30vh', overflow: 'auto' }}>
              {filteredMappingRules.length === 0 ? (
                <ListItem>
                  <ListItemText 
                    primary={
                      mappingSearchTerm 
                        ? "No mapping rules found matching your search"
                        : "No mapping rules defined yet"
                    } 
                  />
                </ListItem>
              ) : (
                filteredMappingRules.map((rule: MappingRule) => renderMappingRule(rule))
              )}
            </List>
          </Paper>
        </Box>
      )}

      {/* Create Mapping Dialog */}
      <Dialog 
        open={openDialog} 
        onClose={() => setOpenDialog(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Create Mapping Rule</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Name"
                value={newMapping.name}
                onChange={(e) => setNewMapping({...newMapping, name: e.target.value})}
                required
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="XML Path"
                value={newMapping.xmlPath}
                onChange={(e) => setNewMapping({
                  ...newMapping, 
                  xmlPath: e.target.value,
                  sourceField: e.target.value // Support for new field names
                })}
                required
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Database Field"
                value={newMapping.databaseField}
                disabled
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Description"
                value={newMapping.description}
                onChange={(e) => setNewMapping({...newMapping, description: e.target.value})}
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Default Value"
                value={newMapping.defaultValue || ''}
                onChange={(e) => setNewMapping({...newMapping, defaultValue: e.target.value})}
                helperText="Value to use if XML path is not found"
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                label="Priority"
                type="number"
                value={newMapping.priority || 0}
                onChange={(e) => setNewMapping({...newMapping, priority: parseInt(e.target.value) || 0})}
                helperText="Processing order priority"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Transformation Rule"
                value={newMapping.transformationRule || ''}
                onChange={(e) => setNewMapping({...newMapping, transformationRule: e.target.value})}
                helperText="Optional transformation to apply (e.g., 'date', 'uppercase', 'number')"
              />
            </Grid>
            <Grid item xs={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={newMapping.required || false}
                    onChange={(e) => setNewMapping({...newMapping, required: e.target.checked})}
                  />
                }
                label="Required Field"
              />
            </Grid>
            <Grid item xs={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={newMapping.isActive}
                    onChange={(e) => setNewMapping({...newMapping, isActive: e.target.checked})}
                  />
                }
                label="Active"
              />
            </Grid>
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel>Target Level</InputLabel>
                <Select
                  value={newMapping.targetLevel || 'HEADER'}
                  onChange={(e) => {
                    const targetLevel = e.target.value as 'HEADER' | 'LINE';
                    const tableName = selectedInterface?.type === 'ASN' 
                      ? (targetLevel === 'HEADER' ? 'ASN_HEADERS' : 'ASN_LINES')
                      : (targetLevel === 'HEADER' ? 'ORDER_HEADERS' : 'ORDER_LINES');
                    
                    setNewMapping({
                      ...newMapping,
                      targetLevel,
                      tableName
                    });
                  }}
                  label="Target Level"
                >
                  <MenuItem value="HEADER">Header</MenuItem>
                  <MenuItem value="LINE">Line</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Table Name"
                value={newMapping.tableName || (selectedInterface?.type === 'ASN' ? 'ASN_HEADERS' : 'ORDER_HEADERS')}
                disabled
                helperText={`Automatically set based on target level for ${selectedInterface?.type || 'ORDER'} flow`}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleSaveMapping} variant="contained" color="primary">
            Save
          </Button>
        </DialogActions>
      </Dialog>

      {/* Upload XSD Dialog */}
      <Dialog
        open={uploadDialogOpen}
        onClose={() => setUploadDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Upload XSD File</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2, mb: 2 }}>
            <Alert severity="info" sx={{ mb: 2 }}>
              <Typography variant="body2">
                Upload an XSD file to analyze its structure and namespaces. This will help you create mapping rules for XML files that conform to this schema.
              </Typography>
            </Alert>
            <input
              accept=".xsd"
              style={{ display: 'none' }}
              id="xsd-file-upload"
              type="file"
              onChange={handleFileUpload}
            />
            <label htmlFor="xsd-file-upload">
              <Button
                variant="outlined"
                component="span"
                startIcon={<UploadFileIcon />}
                sx={{ mb: 2 }}
              >
                Select XSD File
              </Button>
            </label>
            {selectedFile && (
              <Typography variant="body2" sx={{ mt: 1 }}>
                Selected file: {selectedFile.name}
              </Typography>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUploadDialogOpen(false)}>Cancel</Button>
          <Button 
            onClick={handleAnalyzeXsdFile} 
            variant="contained" 
            color="primary"
            disabled={!selectedFile || fileAnalysisLoading}
          >
            {fileAnalysisLoading ? <CircularProgress size={24} /> : 'Analyze'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar 
        open={snackbar.open} 
        autoHideDuration={6000} 
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={handleCloseSnackbar} severity={snackbar.severity}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default TransformPage;
