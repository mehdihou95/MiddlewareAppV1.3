import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { SnackbarProvider } from 'notistack';

import theme from './theme';
import { AuthProvider } from './context/AuthContext';
import { ClientInterfaceProvider } from './context/ClientInterfaceContext';

// Core components
import Navigation from './components/core/Navigation';
import Login from './components/core/Login';
import PrivateRoute from './components/core/PrivateRoute';

// Pages
import HomePage from './pages/HomePage';
import InboundConfigLayout from './layouts/InboundConfigLayout';
import ListenerConfigLayout from './layouts/ListenerConfigLayout';
import AdminLayout from './layouts/AdminLayout';
import TransformPage from './pages/inbound_config/TransformPage';
import UploadPage from './pages/inbound_config/UploadPage';
import ClientManagementPage from './pages/administration/ClientManagementPage';
import InterfaceManagementPage from './pages/administration/InterfaceManagementPage';
import AdminPage from './pages/administration/AdminPage';
import ProcessedFiles from './components/inbound_config/ProcessedFiles';
import SftpConfigPage from './pages/listener_config/SftpConfigPage';

// Administration components
import UserManagement from './components/administration/UserManagement';
import AuditLogs from './components/administration/AuditLogs';

// Listener config components
import As2Config from './components/listener_config/As2Config';
import ApiConfig from './components/listener_config/ApiConfig';

const MainLayout = () => {
  return (
    <>
      <Navigation />
      <Outlet />
    </>
  );
};

const App: React.FC = () => {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <SnackbarProvider maxSnack={3}>
        <BrowserRouter>
          <AuthProvider>
            <ClientInterfaceProvider>
              <Routes>
                {/* Public route */}
                <Route path="/login" element={<Login />} />
                
                {/* Protected routes */}
                <Route element={
                  <PrivateRoute>
                    <MainLayout />
                  </PrivateRoute>
                }>
                  {/* Home */}
                  <Route index element={<HomePage />} />
                  
                  {/* Inbound Configuration */}
                  <Route path="inbound-config" element={<InboundConfigLayout />}>
                    <Route index element={<UploadPage />} />
                    <Route path="upload" element={<UploadPage />} />
                    <Route path="processed" element={<ProcessedFiles />} />
                    <Route path="transform" element={<TransformPage />} />
                  </Route>
                  
                  {/* Listener Configuration */}
                  <Route path="listener-config" element={<ListenerConfigLayout />}>
                    <Route path="sftp" element={<SftpConfigPage />} />
                    <Route path="as2" element={<As2Config />} />
                    <Route path="api" element={<ApiConfig />} />
                    <Route index element={<Navigate to="sftp" replace />} />
                  </Route>
                  
                  {/* Administration */}
                  <Route path="administration" element={<AdminLayout />}>
                    <Route index element={<AdminPage />} />
                    <Route path="clients" element={<ClientManagementPage />} />
                    <Route path="interfaces" element={<InterfaceManagementPage />} />
                    <Route path="users" element={<UserManagement />} />
                    <Route path="audit-logs" element={<AuditLogs />} />
                  </Route>
                </Route>
                
                {/* Catch all unmatched routes */}
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </ClientInterfaceProvider>
          </AuthProvider>
        </BrowserRouter>
      </SnackbarProvider>
    </ThemeProvider>
  );
};

export default App; 