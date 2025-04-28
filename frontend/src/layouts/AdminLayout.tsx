import React from 'react';
import { Box, Container, Typography } from '@mui/material';
import { Outlet } from 'react-router-dom';

const AdminLayout: React.FC = () => {
  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Administration
        </Typography>
        <Box sx={{ mt: 3 }}>
          <Outlet />
        </Box>
      </Box>
    </Container>
  );
};

export default AdminLayout; 