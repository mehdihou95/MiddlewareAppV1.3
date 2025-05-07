import React from 'react';
import { Box, Typography, Paper } from '@mui/material';

export interface ApiConfigProps {}

const ApiConfig: React.FC<ApiConfigProps> = () => {
  return (
    <Box p={3}>
      <Paper elevation={2}>
        <Box p={3}>
          <Typography variant="h5" gutterBottom>
            API Configuration
          </Typography>
          <Typography variant="body1">
            Configure your API listener settings here.
          </Typography>
        </Box>
      </Paper>
    </Box>
  );
};

export default ApiConfig; 