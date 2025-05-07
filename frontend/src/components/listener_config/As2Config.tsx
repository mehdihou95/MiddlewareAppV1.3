import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Grid,
  FormControlLabel,
  Switch,
  MenuItem,
  Snackbar,
  Alert,
  CircularProgress,
} from '@mui/material';
import { useClientInterface } from '../../context/ClientInterfaceContext';
import { as2ConfigService } from '../../services/listener_config/as2ConfigService';
import { As2Config as As2ConfigType } from '../../services/listener_config/types';

interface SnackbarState {
  open: boolean;
  message: string;
  severity: 'success' | 'error' | 'info' | 'warning';
}

const ENCRYPTION_ALGORITHMS = [
  'AES128_CBC',
  'AES192_CBC',
  'AES256_CBC',
  'DES_EDE3_CBC'
];

const SIGNATURE_ALGORITHMS = [
  'SHA1WITHRSA',
  'SHA256WITHRSA',
  'SHA384WITHRSA',
  'SHA512WITHRSA'
];

const MDN_MODES = [
  'SYNC',
  'ASYNC'
];

const MDN_DIGEST_ALGORITHMS = [
  'SHA1',
  'SHA256',
  'SHA384',
  'SHA512'
];

const As2Config: React.FC = () => {
  const { selectedClient, selectedInterface } = useClientInterface();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState<Omit<As2ConfigType, 'id' | 'client' | 'interfaceConfig'>>({
    partnerId: '',
    localId: '',
    apiName: 'server',
    encryptionAlgorithm: 'AES256_CBC',
    signatureAlgorithm: 'SHA256WITHRSA',
    compression: true,
    mdnMode: 'SYNC',
    mdnDigestAlgorithm: 'SHA256',
    mdnUrl: '',
    encryptMessage: true,
    signMessage: true,
    requestMdn: true,
    isActive: true
  });
  const [snackbar, setSnackbar] = useState<SnackbarState>({
    open: false,
    message: '',
    severity: 'info'
  });

  useEffect(() => {
    if (selectedClient && selectedInterface) {
      loadConfiguration();
    }
  }, [selectedClient, selectedInterface]);

  const loadConfiguration = async () => {
    if (!selectedClient || !selectedInterface) return;

    try {
      setLoading(true);
      const config = await as2ConfigService.getConfigurationByClientAndInterface(
        selectedClient.id,
        selectedInterface.id
      );

      if (config) {
        const { id, client, interfaceConfig, ...configData } = config;
        setFormData(configData);
      }
    } catch (error) {
      setSnackbar({
        open: true,
        message: error instanceof Error ? error.message : 'Failed to load AS2 configuration',
        severity: 'error'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSwitchChange = (name: keyof As2ConfigType) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData(prev => ({
      ...prev,
      [name]: e.target.checked
    }));
  };

  const handleSubmit = async () => {
    if (!selectedClient || !selectedInterface) {
      setSnackbar({
        open: true,
        message: 'Please select a client and interface first',
        severity: 'error'
      });
      return;
    }

    try {
      setLoading(true);
      const configToSave = {
        ...formData,
        client: selectedClient,
        interfaceConfig: selectedInterface
      };

      await as2ConfigService.createConfiguration(configToSave);
      
      setSnackbar({
        open: true,
        message: 'AS2 configuration saved successfully',
        severity: 'success'
      });
    } catch (error) {
      setSnackbar({
        open: true,
        message: error instanceof Error ? error.message : 'Failed to save AS2 configuration',
        severity: 'error'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleCloseSnackbar = () => {
    setSnackbar(prev => ({ ...prev, open: false }));
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box p={3}>
      <Paper elevation={2}>
        <Box p={3}>
          <Typography variant="h5" gutterBottom>
            AS2 Configuration
          </Typography>
          
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Partner ID"
                name="partnerId"
                value={formData.partnerId}
                onChange={handleInputChange}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Local ID"
                name="localId"
                value={formData.localId}
                onChange={handleInputChange}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                select
                label="Encryption Algorithm"
                name="encryptionAlgorithm"
                value={formData.encryptionAlgorithm}
                onChange={handleInputChange}
                required
              >
                {ENCRYPTION_ALGORITHMS.map((algo) => (
                  <MenuItem key={algo} value={algo}>
                    {algo}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                select
                label="Signature Algorithm"
                name="signatureAlgorithm"
                value={formData.signatureAlgorithm}
                onChange={handleInputChange}
                required
              >
                {SIGNATURE_ALGORITHMS.map((algo) => (
                  <MenuItem key={algo} value={algo}>
                    {algo}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                select
                label="MDN Mode"
                name="mdnMode"
                value={formData.mdnMode}
                onChange={handleInputChange}
                required
              >
                {MDN_MODES.map((mode) => (
                  <MenuItem key={mode} value={mode}>
                    {mode}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                select
                label="MDN Digest Algorithm"
                name="mdnDigestAlgorithm"
                value={formData.mdnDigestAlgorithm}
                onChange={handleInputChange}
                required
              >
                {MDN_DIGEST_ALGORITHMS.map((algo) => (
                  <MenuItem key={algo} value={algo}>
                    {algo}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="MDN URL (Optional)"
                name="mdnUrl"
                value={formData.mdnUrl}
                onChange={handleInputChange}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.compression}
                    onChange={handleSwitchChange('compression')}
                    name="compression"
                  />
                }
                label="Enable Compression"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.encryptMessage}
                    onChange={handleSwitchChange('encryptMessage')}
                    name="encryptMessage"
                  />
                }
                label="Encrypt Message"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.signMessage}
                    onChange={handleSwitchChange('signMessage')}
                    name="signMessage"
                  />
                }
                label="Sign Message"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.requestMdn}
                    onChange={handleSwitchChange('requestMdn')}
                    name="requestMdn"
                  />
                }
                label="Request MDN"
              />
            </Grid>
            <Grid item xs={12}>
              <Button
                variant="contained"
                color="primary"
                onClick={handleSubmit}
                disabled={!selectedClient || !selectedInterface}
              >
                Save Configuration
              </Button>
            </Grid>
          </Grid>
        </Box>
      </Paper>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
      >
        <Alert onClose={handleCloseSnackbar} severity={snackbar.severity}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default As2Config; 