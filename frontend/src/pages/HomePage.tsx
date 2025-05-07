import React from 'react';
import { Box, Button, Container, Typography, Grid } from '@mui/material';
import { useNavigate } from 'react-router-dom';

const HomePage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, textAlign: 'center' }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Welcome to Middleware Management
        </Typography>
        
        <Grid container spacing={4} sx={{ mt: 4 }}>
          <Grid item xs={12} md={4}>
            <Button
              variant="contained"
              fullWidth
              size="large"
              onClick={() => navigate('/inbound-config')}
              sx={{ height: '120px' }}
            >
              INBOUND CONFIGURATION
            </Button>
          </Grid>
          
          <Grid item xs={12} md={4}>
            <Button
              variant="contained"
              fullWidth
              size="large"
              onClick={() => navigate('/listener-config')}
              sx={{ height: '120px' }}
            >
              LISTENER CONFIGURATION
            </Button>
          </Grid>
          
          <Grid item xs={12} md={4}>
            <Button
              variant="contained"
              fullWidth
              size="large"
              onClick={() => navigate('/administration')}
              sx={{ height: '120px' }}
            >
              ADMINISTRATION
            </Button>
          </Grid>
        </Grid>
      </Box>
    </Container>
  );
};

export default HomePage; 