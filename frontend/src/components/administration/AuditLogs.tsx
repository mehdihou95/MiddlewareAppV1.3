import React, { useState, useEffect, useCallback } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  TablePagination,
  TextField,
  MenuItem,
  Grid,
  Box,
  Typography,
  Alert,
  IconButton,
  Tooltip,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import auditLogService, { AuditLogQueryParams } from '../../services/administration/auditLogService';
import { AuditLog } from '../../services/administration/types';

const REFRESH_INTERVAL = 10000; // Refresh every 10 seconds

const AuditLogs: React.FC = () => {
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [sortBy, setSortBy] = useState('createdAt');
  const [direction, setDirection] = useState<'ASC' | 'DESC'>('DESC');
  const [username, setUsername] = useState('');
  const [clientId, setClientId] = useState('');
  const [action, setAction] = useState('');
  const [status, setStatus] = useState('');
  const [startDate, setStartDate] = useState<Date | null>(null);
  const [endDate, setEndDate] = useState<Date | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchAuditLogs = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const params: AuditLogQueryParams = {
        page,
        size: rowsPerPage,
        sortBy,
        direction,
      };

      let response;
      if (username) {
        response = await auditLogService.getAuditLogsByUsername(username, params);
      } else if (clientId) {
        response = await auditLogService.getAuditLogsByClientId(parseInt(clientId), params);
      } else if (action) {
        response = await auditLogService.getAuditLogsByAction(action, params);
      } else if (status) {
        response = await auditLogService.getAuditLogsByResponseStatus(parseInt(status), params);
      } else if (startDate && endDate) {
        response = await auditLogService.getAuditLogsByDateRange(
          startDate.toISOString(),
          endDate.toISOString(),
          params
        );
      } else {
        response = await auditLogService.getAllAuditLogs(params);
      }

      setAuditLogs(response.content);
      setTotalElements(response.totalElements);
    } catch (error) {
      console.error('Error fetching audit logs:', error);
      setError('Failed to fetch audit logs. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage, sortBy, direction, username, clientId, action, status, startDate, endDate]);

  useEffect(() => {
    fetchAuditLogs();
    
    // Set up automatic refresh
    const intervalId = setInterval(fetchAuditLogs, REFRESH_INTERVAL);
    
    // Cleanup interval on component unmount
    return () => clearInterval(intervalId);
  }, [fetchAuditLogs]);

  const handleRefresh = () => {
    fetchAuditLogs();
  };

  const handleChangePage = (event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const handleSortChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSortBy(event.target.value);
  };

  const handleDirectionChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setDirection(event.target.value as 'ASC' | 'DESC');
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">
          Audit Logs
        </Typography>
        <Tooltip title="Refresh logs">
          <IconButton onClick={handleRefresh} disabled={loading}>
            <RefreshIcon />
          </IconButton>
        </Tooltip>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <TextField
            fullWidth
            label="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <TextField
            fullWidth
            label="Client ID"
            value={clientId}
            onChange={(e) => setClientId(e.target.value)}
            type="number"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <TextField
            fullWidth
            label="Action"
            value={action}
            onChange={(e) => setAction(e.target.value)}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <TextField
            fullWidth
            label="Status"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            type="number"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <LocalizationProvider dateAdapter={AdapterDateFns}>
            <DatePicker
              label="Start Date"
              value={startDate}
              onChange={setStartDate}
              slotProps={{ textField: { fullWidth: true } }}
            />
          </LocalizationProvider>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <LocalizationProvider dateAdapter={AdapterDateFns}>
            <DatePicker
              label="End Date"
              value={endDate}
              onChange={setEndDate}
              slotProps={{ textField: { fullWidth: true } }}
            />
          </LocalizationProvider>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <TextField
            fullWidth
            label="Sort By"
            value={sortBy}
            onChange={handleSortChange}
            select
          >
            <MenuItem value="createdAt">Created At</MenuItem>
            <MenuItem value="action">Action</MenuItem>
            <MenuItem value="username">Username</MenuItem>
            <MenuItem value="clientId">Client ID</MenuItem>
            <MenuItem value="executionTime">Execution Time</MenuItem>
          </TextField>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <TextField
            fullWidth
            label="Direction"
            value={direction}
            onChange={handleDirectionChange}
            select
          >
            <MenuItem value="ASC">Ascending</MenuItem>
            <MenuItem value="DESC">Descending</MenuItem>
          </TextField>
        </Grid>
      </Grid>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Action</TableCell>
              <TableCell>Username</TableCell>
              <TableCell>Client ID</TableCell>
              <TableCell>Details</TableCell>
              <TableCell>IP Address</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Created At</TableCell>
              <TableCell>Execution Time (ms)</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {auditLogs.map((log) => (
              <TableRow key={log.id}>
                <TableCell>{log.id}</TableCell>
                <TableCell>{log.action}</TableCell>
                <TableCell>{log.username}</TableCell>
                <TableCell>{log.clientId || 'N/A'}</TableCell>
                <TableCell>{log.details}</TableCell>
                <TableCell>{log.ipAddress}</TableCell>
                <TableCell>{log.responseStatus}</TableCell>
                <TableCell>{new Date(log.createdAt).toLocaleString()}</TableCell>
                <TableCell>{log.executionTime}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <TablePagination
        component="div"
        count={totalElements}
        page={page}
        onPageChange={handleChangePage}
        rowsPerPage={rowsPerPage}
        onRowsPerPageChange={handleChangeRowsPerPage}
        rowsPerPageOptions={[5, 10, 25, 50]}
      />
    </Box>
  );
};

export default AuditLogs; 