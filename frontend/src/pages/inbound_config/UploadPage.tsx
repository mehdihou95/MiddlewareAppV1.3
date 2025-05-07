import React from 'react';
import { Box, Typography, Alert } from '@mui/material';
import FileUpload from '../../components/inbound_config/FileUpload';
import ClientInterfaceSelector from '../../components/core/ClientInterfaceSelector';
import { useClientInterface } from '../../context/ClientInterfaceContext';
import { useSnackbar } from 'notistack';
import { ProcessedFile } from '../../services/inbound_config/types';

const UploadPage: React.FC = () => {
  const { selectedClient, selectedInterface } = useClientInterface();
  const { enqueueSnackbar } = useSnackbar();

  const handleUploadSuccess = (processedFile: ProcessedFile) => {
    enqueueSnackbar('File uploaded successfully', { variant: 'success' });
  };

  const handleUploadError = (error: string) => {
    enqueueSnackbar(error, { variant: 'error' });
  };

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto', p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Upload XML File
      </Typography>

      <ClientInterfaceSelector required />

      {!selectedClient || !selectedInterface ? (
        <Alert severity="info" sx={{ mt: 2 }}>
          Please select both a client and an interface to upload a file
        </Alert>
      ) : (
        <Box sx={{ mt: 3 }}>
          <FileUpload
            clientId={selectedClient.id}
            interfaceId={selectedInterface.id}
            interfaceType={selectedInterface.type}
            onUploadSuccess={handleUploadSuccess}
            onUploadError={handleUploadError}
          />
        </Box>
      )}
    </Box>
  );
};

export default UploadPage; 