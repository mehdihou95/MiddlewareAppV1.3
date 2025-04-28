import React, { useState } from 'react';
import { Link as RouterLink, useNavigate, useLocation } from 'react-router-dom';
import { 
  AppBar, 
  Toolbar, 
  Typography, 
  Button, 
  Box, 
  Divider, 
  IconButton, 
  Menu, 
  MenuItem, 
  Chip,
  Avatar
} from '@mui/material';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import BrushIcon from '@mui/icons-material/Brush';
import TransformIcon from '@mui/icons-material/Transform';
import AccountCircle from '@mui/icons-material/AccountCircle';
import BusinessIcon from '@mui/icons-material/Business';
import SettingsEthernetIcon from '@mui/icons-material/SettingsEthernet';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import { useAuth } from '../../context/AuthContext';
import { useClientInterface } from '../../context/ClientInterfaceContext';

const Navigation = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { 
    clients, 
    interfaces, 
    selectedClient, 
    selectedInterface,
    setSelectedClient,
    setSelectedInterface
  } = useClientInterface();
  
  const [clientMenuAnchor, setClientMenuAnchor] = useState<null | HTMLElement>(null);
  const [interfaceMenuAnchor, setInterfaceMenuAnchor] = useState<null | HTMLElement>(null);
  const [userMenuAnchor, setUserMenuAnchor] = useState<null | HTMLElement>(null);

  const handleClientMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setClientMenuAnchor(event.currentTarget);
  };

  const handleClientMenuClose = () => {
    setClientMenuAnchor(null);
  };

  const handleInterfaceMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setInterfaceMenuAnchor(event.currentTarget);
  };

  const handleInterfaceMenuClose = () => {
    setInterfaceMenuAnchor(null);
  };

  const handleUserMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setUserMenuAnchor(event.currentTarget);
  };

  const handleUserMenuClose = () => {
    setUserMenuAnchor(null);
  };

  const handleClientSelect = (client: any) => {
    setSelectedClient(client);
    handleClientMenuClose();
  };

  const handleInterfaceSelect = (interfaceObj: any) => {
    setSelectedInterface(interfaceObj);
    handleInterfaceMenuClose();
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login');
    handleUserMenuClose();
  };

  const isActive = (path: string) => {
    return location.pathname.startsWith(path);
  };

  if (!user?.authenticated) {
    return (
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            XML Processor
          </Typography>
        </Toolbar>
      </AppBar>
    );
  }

  return (
    <AppBar position="static">
      <Toolbar>
        <Typography 
          variant="h6" 
          component="div" 
          sx={{ 
            flexGrow: 1, 
            cursor: 'pointer' 
          }}
          onClick={() => navigate('/')}
        >
          XML Processor
        </Typography>
        
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {/* Main Navigation */}
          {user.roles.includes('ADMIN') && (
            <Button
              color="inherit"
              onClick={() => navigate('/administration')}
              sx={{
                borderBottom: isActive('/administration') ? '2px solid white' : 'none',
                borderRadius: 0,
                px: 2
              }}
              startIcon={<AdminPanelSettingsIcon />}
            >
              Administration
            </Button>
          )}
          
          <Button
            color="inherit"
            onClick={() => navigate('/inbound-config')}
            sx={{
              borderBottom: isActive('/inbound-config') ? '2px solid white' : 'none',
              borderRadius: 0,
              px: 2
            }}
            startIcon={<TransformIcon />}
          >
            Inbound Config
          </Button>
          
          <Button
            color="inherit"
            onClick={() => navigate('/listener-config')}
            sx={{
              borderBottom: isActive('/listener-config') ? '2px solid white' : 'none',
              borderRadius: 0,
              px: 2
            }}
            startIcon={<BrushIcon />}
          >
            Listener Config
          </Button>

          <Divider orientation="vertical" flexItem sx={{ mx: 2, bgcolor: 'white' }} />

          {/* Client/Interface Selection */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Chip
              icon={<BusinessIcon />}
              label={selectedClient ? selectedClient.name : "Select Client"}
              onClick={handleClientMenuOpen}
              color={selectedClient ? "primary" : "default"}
              variant="outlined"
              sx={{ bgcolor: 'rgba(255,255,255,0.1)' }}
              deleteIcon={<KeyboardArrowDownIcon />}
              onDelete={handleClientMenuOpen}
            />
            
            <Menu
              anchorEl={clientMenuAnchor}
              open={Boolean(clientMenuAnchor)}
              onClose={handleClientMenuClose}
            >
              {!clients || clients.length === 0 ? (
                <MenuItem disabled>No clients available</MenuItem>
              ) : (
                clients.map(client => (
                  <MenuItem 
                    key={client.id} 
                    onClick={() => handleClientSelect(client)}
                    selected={selectedClient?.id === client.id}
                  >
                    {client.name}
                  </MenuItem>
                ))
              )}
            </Menu>

            {selectedClient && (
              <>
                <Chip
                  icon={<SettingsEthernetIcon />}
                  label={selectedInterface ? selectedInterface.name : "Select Interface"}
                  onClick={handleInterfaceMenuOpen}
                  color={selectedInterface ? "primary" : "default"}
                  variant="outlined"
                  sx={{ bgcolor: 'rgba(255,255,255,0.1)' }}
                  deleteIcon={<KeyboardArrowDownIcon />}
                  onDelete={handleInterfaceMenuOpen}
                />
                
                <Menu
                  anchorEl={interfaceMenuAnchor}
                  open={Boolean(interfaceMenuAnchor)}
                  onClose={handleInterfaceMenuClose}
                >
                  {interfaces.length === 0 ? (
                    <MenuItem disabled>No interfaces available</MenuItem>
                  ) : (
                    interfaces.map(interfaceObj => (
                      <MenuItem 
                        key={interfaceObj.id} 
                        onClick={() => handleInterfaceSelect(interfaceObj)}
                        selected={selectedInterface?.id === interfaceObj.id}
                      >
                        {interfaceObj.name}
                      </MenuItem>
                    ))
                  )}
                </Menu>
              </>
            )}
          </Box>

          {/* User Menu */}
          <Box sx={{ ml: 2 }}>
            <IconButton 
              color="inherit"
              onClick={handleUserMenuOpen}
              size="small"
              sx={{ ml: 2 }}
            >
              <Avatar sx={{ width: 32, height: 32 }}>
                <AccountCircle />
              </Avatar>
            </IconButton>
            <Menu
              anchorEl={userMenuAnchor}
              open={Boolean(userMenuAnchor)}
              onClose={handleUserMenuClose}
              onClick={handleUserMenuClose}
            >
              <MenuItem disabled>
                <Typography variant="body2" color="textSecondary">
                  Signed in as {user.username}
                </Typography>
              </MenuItem>
              <Divider />
              {user.roles.includes('ADMIN') && (
                <>
                  <MenuItem onClick={() => navigate('/administration/clients')}>
                    Client Management
                  </MenuItem>
                  <MenuItem onClick={() => navigate('/administration/interfaces')}>
                    Interface Management
                  </MenuItem>
                  <MenuItem onClick={() => navigate('/administration/users')}>
                    User Management
                  </MenuItem>
                  <MenuItem onClick={() => navigate('/administration/audit-logs')}>
                    Audit Logs
                  </MenuItem>
                </>
              )}
              <Divider />
              <MenuItem onClick={handleLogout}>
                Logout
              </MenuItem>
            </Menu>
          </Box>
        </Box>
      </Toolbar>
    </AppBar>
  );
};

export default Navigation; 