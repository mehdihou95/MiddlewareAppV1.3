import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  Typography,
  Container,
  Box,
  IconButton,
  Paper,
} from '@mui/material';
import {
  AdminPanelSettings,
  Brush,
  Transform,
} from '@mui/icons-material';

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const isActive = (path: string) => location.pathname === path;

  const navigationItems = [
    { path: '/admin', icon: <AdminPanelSettings />, label: 'Admin' },
    { path: '/ux', icon: <Brush />, label: 'UX' },
    { path: '/transformation', icon: <Transform />, label: 'Transformation' },
  ];

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            XML Processor
          </Typography>
          <Box sx={{ display: 'flex', gap: 2 }}>
            {navigationItems.map((item) => (
              <IconButton
                key={item.path}
                color={isActive(item.path) ? 'secondary' : 'inherit'}
                onClick={() => navigate(item.path)}
                sx={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                }}
              >
                {item.icon}
                <Typography variant="caption">{item.label}</Typography>
              </IconButton>
            ))}
          </Box>
        </Toolbar>
      </AppBar>
      <Container component="main" sx={{ mt: 4, mb: 4, flexGrow: 1 }}>
        <Paper elevation={3} sx={{ p: 3 }}>
          {children}
        </Paper>
      </Container>
    </Box>
  );
};

export default Layout; 