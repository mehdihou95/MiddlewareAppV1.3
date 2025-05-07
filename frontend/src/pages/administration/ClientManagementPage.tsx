import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  TextField,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  TableSortLabel,
  Chip,
  IconButton,
  Tooltip,
  Alert,
  CircularProgress
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { useClientInterface } from '../../context/ClientInterfaceContext';
import { useAuth } from '../../context/AuthContext';
import ClientDialog from '../../components/administration/ClientDialog';
import ConfirmDialog from '../../components/core/ConfirmDialog';
import { Client } from '../../services/administration/types';
import { clientService } from '../../services/administration/clientService';

type Order = 'asc' | 'desc';

const ClientManagementPage: React.FC = () => {
  const { 
    clients, 
    loading, 
    error, 
    refreshClients, 
    setError,
    setClients 
  } = useClientInterface();
  
  const { isAuthenticated, hasRole } = useAuth();
  const canManageClients = hasRole('ADMIN');

  const [openDialog, setOpenDialog] = useState(false);
  const [editingClient, setEditingClient] = useState<Client | null>(null);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [clientToDelete, setClientToDelete] = useState<Client | null>(null);
  const [filter, setFilter] = useState('');
  const [orderBy, setOrderBy] = useState<keyof Client>('name');
  const [order, setOrder] = useState<Order>('asc');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalCount, setTotalCount] = useState(0);

  useEffect(() => {
    if (isAuthenticated) {
      refreshClients(page, rowsPerPage, orderBy, order);
    }
  }, [refreshClients, page, rowsPerPage, orderBy, order, isAuthenticated]);

  const handleOpenDialog = (client?: Client) => {
    if (client) {
      setEditingClient(client);
    } else {
      setEditingClient(null);
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingClient(null);
  };

  const handleSubmit = async (clientData: Partial<Client>) => {
    try {
      const clientInput = {
        name: clientData.name!,
        code: clientData.code!,
        description: clientData.description || '',
        status: clientData.status || 'ACTIVE'
      };

      if (editingClient) {
        await clientService.updateClient(editingClient.id, clientInput);
      } else {
        await clientService.createClient(clientInput);
      }
      handleCloseDialog();
      await refreshClients();
    } catch (error) {
      console.error('Failed to save client:', error);
      setError('Failed to save client. Please try again.');
    }
  };

  const handleDeleteClick = (client: Client) => {
    setClientToDelete(client);
    setDeleteConfirmOpen(true);
  };

  const handleDeleteCancel = () => {
    setDeleteConfirmOpen(false);
    setClientToDelete(null);
  };

  const handleDeleteConfirm = async () => {
    if (!clientToDelete) return;
    
    try {
      await clientService.deleteClient(clientToDelete.id);
      setDeleteConfirmOpen(false);
      setClientToDelete(null);
      await refreshClients();
    } catch (error) {
      console.error('Failed to delete client:', error);
      setError('Failed to delete client. Please try again.');
    }
  };

  const handleRequestSort = (property: keyof Client) => {
    const isAsc = orderBy === property && order === 'asc';
    setOrder(isAsc ? 'desc' : 'asc');
    setOrderBy(property);
  };

  const handleChangePage = (event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  if (!isAuthenticated) {
    return (
      <Box p={3}>
        <Alert severity="warning">Please log in to view clients.</Alert>
      </Box>
    );
  }

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box p={3}>
        <Alert severity="error">{error}</Alert>
      </Box>
    );
  }

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" component="h1">
          Client Management
        </Typography>
        {canManageClients && (
          <Button
            variant="contained"
            color="primary"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
          >
            Add Client
          </Button>
        )}
      </Box>

      <TextField
        fullWidth
        variant="outlined"
        placeholder="Filter clients..."
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        sx={{ mb: 3 }}
      />

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'name'}
                  direction={orderBy === 'name' ? order : 'asc'}
                  onClick={() => handleRequestSort('name')}
                >
                  Name
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'code'}
                  direction={orderBy === 'code' ? order : 'asc'}
                  onClick={() => handleRequestSort('code')}
                >
                  Code
                </TableSortLabel>
              </TableCell>
              <TableCell>Description</TableCell>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'status'}
                  direction={orderBy === 'status' ? order : 'asc'}
                  onClick={() => handleRequestSort('status')}
                >
                  Status
                </TableSortLabel>
              </TableCell>
              {canManageClients && <TableCell>Actions</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {(!clients || clients.length === 0) ? (
              <TableRow>
                <TableCell colSpan={canManageClients ? 5 : 4} align="center">
                  <Typography sx={{ py: 2 }}>
                    No clients found
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              clients.map((client) => (
                <TableRow key={client.id}>
                  <TableCell>{client.name}</TableCell>
                  <TableCell>{client.code}</TableCell>
                  <TableCell>{client.description || 'N/A'}</TableCell>
                  <TableCell>
                    <Chip
                      label={client.status}
                      color={client.status === 'ACTIVE' ? 'success' : 'default'}
                      size="small"
                    />
                  </TableCell>
                  {canManageClients && (
                    <TableCell>
                      <Tooltip title="Edit">
                        <IconButton onClick={() => handleOpenDialog(client)}>
                          <EditIcon />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete">
                        <IconButton onClick={() => handleDeleteClick(client)} color="error">
                          <DeleteIcon />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <ClientDialog
        open={openDialog}
        onClose={handleCloseDialog}
        onSubmit={handleSubmit}
        client={editingClient}
      />

      <ConfirmDialog
        open={deleteConfirmOpen}
        onClose={handleDeleteCancel}
        onConfirm={handleDeleteConfirm}
        title="Delete Client"
        content={`Are you sure you want to delete ${clientToDelete?.name}?`}
      />
    </Box>
  );
};

export default ClientManagementPage; 