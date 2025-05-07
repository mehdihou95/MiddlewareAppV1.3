import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Grid,
  IconButton,
  InputAdornment,
  Switch,
  FormControlLabel,
  Divider,
  Alert,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import SearchIcon from '@mui/icons-material/Search';
import { useClientInterface } from '../../context/ClientInterfaceContext';
import { sftpConfigService } from '../../services/listener_config/sftpConfigService';
import { SftpConfig, SftpConfigCreate } from '../../services/listener_config/types';
import { useSnackbar } from 'notistack';

const SftpConfigPage: React.FC = () => {
  const { selectedClient, selectedInterface } = useClientInterface();
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(false);
  const [configurations, setConfigurations] = useState<SftpConfig[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [formData, setFormData] = useState<Omit<SftpConfigCreate, 'clientId' | 'interfaceId'>>({
    host: '',
    port: 22,
    username: '',
    password: '',
    privateKeyPath: '',
    privateKeyPassphrase: '',
    remoteDirectory: '',
    monitoredDirectories: [],
    filePattern: '*.xml',
    pollingInterval: 60,
    connectionTimeout: 30000,
    retryAttempts: 3,
    retryDelay: 5000,
    is_active: true
  });
  const [newDirectory, setNewDirectory] = useState('');

  useEffect(() => {
    if (selectedClient && selectedInterface) {
      loadConfigurations();
    }
  }, [selectedClient, selectedInterface]);

  const loadConfigurations = async () => {
    if (!selectedClient || !selectedInterface) return;

    try {
      setLoading(true);
      const configs = await sftpConfigService.getAllConfigurations();
      setConfigurations(configs);
    } catch (error) {
      enqueueSnackbar(
        error instanceof Error ? error.message : 'Failed to load SFTP configurations',
        { variant: 'error' }
      );
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : 
              (name === 'port' || name === 'pollingInterval' || 
               name === 'connectionTimeout' || name === 'retryAttempts' || 
               name === 'retryDelay') ? Number(value) : value
    }));
  };

  const handleAddDirectory = () => {
    if (newDirectory && !formData.monitoredDirectories.includes(newDirectory)) {
      setFormData(prev => ({
        ...prev,
        monitoredDirectories: [...prev.monitoredDirectories, newDirectory]
      }));
      setNewDirectory('');
    }
  };

  const handleRemoveDirectory = (directory: string) => {
    setFormData(prev => ({
      ...prev,
      monitoredDirectories: prev.monitoredDirectories.filter(d => d !== directory)
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedClient || !selectedInterface) {
      enqueueSnackbar('Please select a client and interface', { variant: 'warning' });
      return;
    }

    try {
      setLoading(true);
      await sftpConfigService.createConfiguration({
        ...formData,
        clientId: selectedClient.id,
        interfaceId: selectedInterface.id
      });
      enqueueSnackbar('SFTP configuration created successfully', { variant: 'success' });
      loadConfigurations();
      // Reset form
      setFormData({
        host: '',
        port: 22,
        username: '',
        password: '',
        privateKeyPath: '',
        privateKeyPassphrase: '',
        remoteDirectory: '',
        monitoredDirectories: [],
        filePattern: '*.xml',
        pollingInterval: 60,
        connectionTimeout: 30000,
        retryAttempts: 3,
        retryDelay: 5000,
        is_active: true
      });
    } catch (error) {
      enqueueSnackbar(
        error instanceof Error ? error.message : 'Failed to create SFTP configuration',
        { variant: 'error' }
      );
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (config: SftpConfig) => {
    if (!config.id) return;

    try {
      await sftpConfigService.deleteConfiguration(config.id);
      enqueueSnackbar('SFTP configuration deleted successfully', { variant: 'success' });
      loadConfigurations();
    } catch (error) {
      enqueueSnackbar(
        error instanceof Error ? error.message : 'Failed to delete SFTP configuration',
        { variant: 'error' }
      );
    }
  };

  const filteredConfigurations = configurations.filter(config => 
    config.host.toLowerCase().includes(searchTerm.toLowerCase()) ||
    config.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
    config.remoteDirectory.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (!selectedClient || !selectedInterface) {
    return (
      <Alert severity="info" sx={{ mt: 2 }}>
        Please select a client and interface to manage SFTP configurations.
      </Alert>
    );
  }

  return (
    <Box>
      {/* Configuration Form */}
      <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          Create SFTP Configuration
        </Typography>
        <form onSubmit={handleSubmit}>
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Host"
                name="host"
                value={formData.host}
                onChange={handleInputChange}
                required
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Port"
                name="port"
                type="number"
                value={formData.port}
                onChange={handleInputChange}
                required
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Username"
                name="username"
                value={formData.username}
                onChange={handleInputChange}
                required
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Password"
                name="password"
                type="password"
                value={formData.password}
                onChange={handleInputChange}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Private Key Path"
                name="privateKeyPath"
                value={formData.privateKeyPath}
                onChange={handleInputChange}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Private Key Passphrase"
                name="privateKeyPassphrase"
                type="password"
                value={formData.privateKeyPassphrase}
                onChange={handleInputChange}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Remote Directory"
                name="remoteDirectory"
                value={formData.remoteDirectory}
                onChange={handleInputChange}
                required
                margin="normal"
              />
            </Grid>
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                <TextField
                  fullWidth
                  label="Add Monitored Directory"
                  value={newDirectory}
                  onChange={(e) => setNewDirectory(e.target.value)}
                />
                <IconButton
                  color="primary"
                  onClick={handleAddDirectory}
                  disabled={!newDirectory}
                >
                  <AddIcon />
                </IconButton>
              </Box>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {formData.monitoredDirectories.map((dir, index) => (
                  <Chip
                    key={index}
                    label={dir}
                    onDelete={() => handleRemoveDirectory(dir)}
                  />
                ))}
              </Box>
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="File Pattern"
                name="filePattern"
                value={formData.filePattern}
                onChange={handleInputChange}
                required
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Polling Interval (seconds)"
                name="pollingInterval"
                type="number"
                value={formData.pollingInterval}
                onChange={handleInputChange}
                required
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Connection Timeout (ms)"
                name="connectionTimeout"
                type="number"
                value={formData.connectionTimeout}
                onChange={handleInputChange}
                required
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Retry Attempts"
                name="retryAttempts"
                type="number"
                value={formData.retryAttempts}
                onChange={handleInputChange}
                required
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Retry Delay (ms)"
                name="retryDelay"
                type="number"
                value={formData.retryDelay}
                onChange={handleInputChange}
                required
                margin="normal"
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.is_active}
                    onChange={handleInputChange}
                    name="is_active"
                  />
                }
                label="Active"
              />
            </Grid>
            <Grid item xs={12}>
              <Button
                type="submit"
                variant="contained"
                color="primary"
                disabled={loading}
              >
                {loading ? <CircularProgress size={24} /> : 'Create Configuration'}
              </Button>
            </Grid>
          </Grid>
        </form>
      </Paper>

      {/* Configurations List */}
      <Paper elevation={2} sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">
            Existing Configurations
          </Typography>
          <TextField
            size="small"
            placeholder="Search configurations..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
          />
        </Box>
        <Divider sx={{ mb: 2 }} />
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
            <CircularProgress />
          </Box>
        ) : filteredConfigurations.length === 0 ? (
          <Alert severity="info">No SFTP configurations found.</Alert>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Host</TableCell>
                  <TableCell>Username</TableCell>
                  <TableCell>Remote Directory</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredConfigurations.map((config) => (
                  <TableRow key={config.id}>
                    <TableCell>{config.host}</TableCell>
                    <TableCell>{config.username}</TableCell>
                    <TableCell>{config.remoteDirectory}</TableCell>
                    <TableCell>
                      <Chip
                        label={config.is_active ? 'Active' : 'Inactive'}
                        color={config.is_active ? 'success' : 'default'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      <IconButton
                        color="error"
                        onClick={() => handleDelete(config)}
                      >
                        <DeleteIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>
    </Box>
  );
};

export default SftpConfigPage; 