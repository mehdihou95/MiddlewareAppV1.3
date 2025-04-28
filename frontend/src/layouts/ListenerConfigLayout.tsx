import React from 'react';
import { Box, Container, Typography, Tabs, Tab, Paper } from '@mui/material';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import StorageIcon from '@mui/icons-material/Storage';
import ApiIcon from '@mui/icons-material/Api';
import SwapHorizIcon from '@mui/icons-material/SwapHoriz';
import ClientInterfaceSelector from '../components/core/ClientInterfaceSelector';

const ListenerConfigLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const getCurrentTab = () => {
    const path = location.pathname;
    if (path.includes('/sftp')) return 0;
    if (path.includes('/as2')) return 1;
    if (path.includes('/api')) return 2;
    return 0;
  };

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    switch (newValue) {
      case 0:
        navigate('/listener-config/sftp');
        break;
      case 1:
        navigate('/listener-config/as2');
        break;
      case 2:
        navigate('/listener-config/api');
        break;
    }
  };

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Listener Configuration
        </Typography>

        <ClientInterfaceSelector required />
        
        <Paper sx={{ mt: 3, mb: 3 }}>
          <Tabs 
            value={getCurrentTab()} 
            onChange={handleTabChange}
            variant="fullWidth"
            sx={{
              borderBottom: 1,
              borderColor: 'divider',
              '& .MuiTab-root': {
                minHeight: '64px',
                fontSize: '1rem'
              }
            }}
          >
            <Tab 
              icon={<StorageIcon />} 
              label="SFTP Configuration" 
              iconPosition="start"
              sx={{ flexDirection: 'row', gap: 1 }}
            />
            <Tab 
              icon={<SwapHorizIcon />} 
              label="AS2 Configuration" 
              iconPosition="start"
              sx={{ flexDirection: 'row', gap: 1 }}
            />
            <Tab 
              icon={<ApiIcon />} 
              label="API Configuration" 
              iconPosition="start"
              sx={{ flexDirection: 'row', gap: 1 }}
            />
          </Tabs>
        </Paper>

        <Box sx={{ mt: 3 }}>
          <Outlet />
        </Box>
      </Box>
    </Container>
  );
};

export default ListenerConfigLayout; 