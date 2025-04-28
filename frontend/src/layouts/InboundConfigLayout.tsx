import React from 'react';
import { Box, Container, Typography, Tabs, Tab, Paper } from '@mui/material';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import TransformIcon from '@mui/icons-material/Transform';
import HistoryIcon from '@mui/icons-material/History';

const InboundConfigLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const getCurrentTab = () => {
    const path = location.pathname;
    if (path.includes('/upload')) return 0;
    if (path.includes('/processed')) return 1;
    if (path.includes('/transform')) return 2;
    return 0;
  };

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    switch (newValue) {
      case 0:
        navigate('/inbound-config/upload');
        break;
      case 1:
        navigate('/inbound-config/processed');
        break;
      case 2:
        navigate('/inbound-config/transform');
        break;
    }
  };

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Inbound Configuration
        </Typography>
        
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
              icon={<UploadFileIcon />} 
              label="Upload Files" 
              iconPosition="start"
              sx={{ flexDirection: 'row', gap: 1 }}
            />
            <Tab 
              icon={<HistoryIcon />} 
              label="Processed Files" 
              iconPosition="start"
              sx={{ flexDirection: 'row', gap: 1 }}
            />
            <Tab 
              icon={<TransformIcon />} 
              label="Transform" 
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

export default InboundConfigLayout; 