import React from 'react';
import { Typography, Grid, Paper } from '@mui/material';

const AdminPage = () => {
  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Typography variant="h4" gutterBottom>
          Admin Panel
        </Typography>
      </Grid>
      <Grid item xs={12}>
        <Paper sx={{ p: 3 }}>
          <Typography variant="body1">
            User management functionality will be implemented here.
          </Typography>
        </Paper>
      </Grid>
    </Grid>
  );
};

export default AdminPage; 