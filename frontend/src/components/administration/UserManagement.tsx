import React, { useState, useEffect } from 'react';
import {
    Box,
    Button,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    IconButton,
    Typography,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TablePagination,
    Paper,
    Alert,
    CircularProgress,
    Chip,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    FormControlLabel,
    Switch,
    Tooltip,
    Snackbar
} from '@mui/material';
import {
    Add as AddIcon,
    Edit as EditIcon,
    Delete as DeleteIcon,
    Lock as LockIcon,
    LockOpen as LockOpenIcon,
    Refresh as RefreshIcon
} from '@mui/icons-material';
import { userService } from '../../services/administration/userService';
import { User } from '../../services/administration/types';
import { handleApiError, isValidationError, getValidationErrors, ApiError } from '../../utils/errorHandler';

interface UserFormData {
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    password?: string;
    roles: string[];
    enabled: boolean;
    createdAt?: string;
    updatedAt?: string;
    accountLocked: boolean;
}

interface UserFormErrors {
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    password?: string;
}

const UserManagement: React.FC = () => {
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [openDialog, setOpenDialog] = useState<boolean>(false);
    const [editingUser, setEditingUser] = useState<User | null>(null);
    const [formData, setFormData] = useState<UserFormData>({
        username: '',
        email: '',
        firstName: '',
        lastName: '',
        roles: [],
        enabled: true,
        accountLocked: false
    });
    const [formErrors, setFormErrors] = useState<UserFormErrors>({
        username: '',
        email: '',
        firstName: '',
        lastName: ''
    });
    const [page, setPage] = useState<number>(0);
    const [rowsPerPage, setRowsPerPage] = useState<number>(10);
    const [totalElements, setTotalElements] = useState(0);
    const [searchTerm, setSearchTerm] = useState('');
    const [enabledFilter, setEnabledFilter] = useState<boolean | null>(null);
    const [snackbar, setSnackbar] = useState<{
        open: boolean;
        message: string;
        severity: 'success' | 'error' | 'info' | 'warning';
    }>({
        open: false,
        message: '',
        severity: 'info'
    });

    const fetchUsers = async () => {
        setLoading(true);
        setError(null);
        try {
            let response;
            if (searchTerm) {
                response = await userService.searchUsers(searchTerm, page, rowsPerPage);
            } else if (enabledFilter !== null) {
                response = await userService.getUsersByStatus(enabledFilter, page, rowsPerPage);
            } else {
                response = await userService.getAllUsers(page, rowsPerPage);
            }
            setUsers(response.content);
            setTotalElements(response.totalElements);
        } catch (error) {
            const apiError = handleApiError(error) as ApiError;
            setError(apiError.message);
            showSnackbar(apiError.message, 'error');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, [page, rowsPerPage, searchTerm, enabledFilter]);

    const handleChangePage = (event: unknown, newPage: number) => {
        setPage(newPage);
    };

    const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

    const handleOpenDialog = (user?: User) => {
        if (user) {
            setEditingUser(user);
            setFormData(user);
        } else {
            setEditingUser(null);
            setFormData({
                username: '',
                email: '',
                firstName: '',
                lastName: '',
                roles: [],
                enabled: true,
                accountLocked: false
            });
        }
        setOpenDialog(true);
    };

    const handleCloseDialog = () => {
        setOpenDialog(false);
        setEditingUser(null);
        setFormData({
            username: '',
            email: '',
            firstName: '',
            lastName: '',
            roles: [],
            enabled: true,
            accountLocked: false
        });
    };

    const handleInputChange = (event: React.ChangeEvent<HTMLInputElement | { name?: string; value: unknown }>) => {
        const { name, value, checked } = event.target as any;
        setFormData(prev => ({
            ...prev,
            [name as string]: name === 'enabled' ? checked : value
        }));
    };

    const handleSubmit = async () => {
        try {
            // Validate form data
            if (!formData.username || !formData.email || !formData.firstName || !formData.lastName) {
                showSnackbar('Please fill in all required fields', 'error');
                return;
            }

            // Ensure password is provided for new users
            if (!editingUser && !formData.password) {
                showSnackbar('Password is required for new users', 'error');
                return;
            }

            // Prepare user data with proper role format and required fields
            const userData = {
                ...formData,
                roles: formData.roles.length > 0 ? formData.roles : ['ROLE_USER'], // Default to ROLE_USER if no role selected
                accountLocked: false, // Default value for new users
                createdAt: new Date().toISOString(), // Set current date for new users
                updatedAt: new Date().toISOString() // Set current date
            };

            console.log('Submitting user data:', { ...userData, password: '***' });

            if (editingUser) {
                await userService.updateUser(editingUser.id, userData);
                showSnackbar('User updated successfully', 'success');
            } else {
                await userService.createUser(userData);
                showSnackbar('User created successfully', 'success');
            }
            handleCloseDialog();
            fetchUsers();
        } catch (error) {
            console.error('Error submitting user:', error);
            if (isValidationError(error)) {
                const validationErrors = getValidationErrors(error);
                validationErrors.forEach(error => showSnackbar(error, 'error'));
            } else {
                const apiError = handleApiError(error) as ApiError;
                showSnackbar(apiError.message, 'error');
            }
        }
    };

    const handleDelete = async (id: number) => {
        if (window.confirm('Are you sure you want to delete this user?')) {
            try {
                await userService.deleteUser(id);
                showSnackbar('User deleted successfully', 'success');
                fetchUsers();
            } catch (error) {
                const apiError = handleApiError(error) as ApiError;
                showSnackbar(apiError.message, 'error');
            }
        }
    };

    const handleUnlockAccount = async (id: number) => {
        try {
            await userService.unlockAccount(id);
            showSnackbar('Account unlocked successfully', 'success');
            fetchUsers();
        } catch (error) {
            const apiError = handleApiError(error) as ApiError;
            showSnackbar(apiError.message, 'error');
        }
    };

    const showSnackbar = (message: string, severity: 'success' | 'error') => {
        setSnackbar({ open: true, message, severity });
    };

    const handleCloseSnackbar = () => {
        setSnackbar(prev => ({ ...prev, open: false }));
    };

    return (
        <Box sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <Typography variant="h4">
                    User Management
                </Typography>
                <Box sx={{ display: 'flex', gap: 2 }}>
                    <Button
                        variant="contained"
                        startIcon={<AddIcon />}
                        onClick={() => handleOpenDialog()}
                        disabled={loading}
                    >
                        Add User
                    </Button>
                    <Tooltip title="Refresh users">
                        <IconButton onClick={fetchUsers} disabled={loading}>
                            <RefreshIcon />
                        </IconButton>
                    </Tooltip>
                </Box>
            </Box>

            {error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                    {error}
                </Alert>
            )}

            <Box sx={{ mb: 2 }}>
                <TextField
                    label="Search Users"
                    variant="outlined"
                    size="small"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    sx={{ mr: 2 }}
                    disabled={loading}
                />
                <FormControlLabel
                    control={
                        <Switch
                            checked={enabledFilter === true}
                            onChange={(e) => setEnabledFilter(e.target.checked ? true : null)}
                            disabled={loading}
                        />
                    }
                    label="Show Enabled Only"
                />
                <Button
                    variant="outlined"
                    size="small"
                    onClick={() => {
                        setSearchTerm('');
                        setEnabledFilter(null);
                    }}
                    disabled={!searchTerm && enabledFilter === null || loading}
                >
                    Reset Filters
                </Button>
            </Box>

            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>Username</TableCell>
                            <TableCell>Email</TableCell>
                            <TableCell>Name</TableCell>
                            <TableCell>Status</TableCell>
                            <TableCell>Account Status</TableCell>
                            <TableCell>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {loading ? (
                            <TableRow>
                                <TableCell colSpan={6} align="center">
                                    Loading users...
                                </TableCell>
                            </TableRow>
                        ) : users.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={6} align="center">
                                    No users found
                                </TableCell>
                            </TableRow>
                        ) : (
                            users.map((user) => (
                                <TableRow key={user.id}>
                                    <TableCell>{user.username}</TableCell>
                                    <TableCell>{user.email}</TableCell>
                                    <TableCell>{`${user.firstName} ${user.lastName}`}</TableCell>
                                    <TableCell>
                                        <Chip
                                            label={user.enabled ? 'Enabled' : 'Disabled'}
                                            color={user.enabled ? 'success' : 'error'}
                                        />
                                    </TableCell>
                                    <TableCell>
                                        {user.accountLocked ? (
                                            <Tooltip title="Account is locked">
                                                <LockIcon color="error" />
                                            </Tooltip>
                                        ) : (
                                            <Tooltip title="Account is active">
                                                <LockOpenIcon color="success" />
                                            </Tooltip>
                                        )}
                                    </TableCell>
                                    <TableCell>
                                        <IconButton onClick={() => handleOpenDialog(user)}>
                                            <EditIcon />
                                        </IconButton>
                                        <IconButton onClick={() => handleDelete(user.id)}>
                                            <DeleteIcon />
                                        </IconButton>
                                        {user.accountLocked && (
                                            <IconButton onClick={() => handleUnlockAccount(user.id)}>
                                                <LockOpenIcon />
                                            </IconButton>
                                        )}
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
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
                disabled={loading}
            />

            <Dialog open={openDialog} onClose={handleCloseDialog}>
                <DialogTitle>{editingUser ? 'Edit User' : 'Add User'}</DialogTitle>
                <DialogContent>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
                        <TextField
                            label="Username"
                            name="username"
                            value={formData.username || ''}
                            onChange={handleInputChange}
                            fullWidth
                        />
                        <TextField
                            label="Email"
                            name="email"
                            type="email"
                            value={formData.email || ''}
                            onChange={handleInputChange}
                            fullWidth
                        />
                        <TextField
                            label="First Name"
                            name="firstName"
                            value={formData.firstName || ''}
                            onChange={handleInputChange}
                            fullWidth
                        />
                        <TextField
                            label="Last Name"
                            name="lastName"
                            value={formData.lastName || ''}
                            onChange={handleInputChange}
                            fullWidth
                        />
                        {!editingUser && (
                            <TextField
                                label="Password"
                                name="password"
                                type="password"
                                value={formData.password || ''}
                                onChange={handleInputChange}
                                fullWidth
                            />
                        )}
                        <FormControl fullWidth>
                            <InputLabel id="role-select-label">Role</InputLabel>
                            <Select
                                labelId="role-select-label"
                                name="roles"
                                value={formData.roles[0] || ''}
                                onChange={(e) => setFormData(prev => ({
                                    ...prev,
                                    roles: [e.target.value as string]
                                }))}
                                label="Role"
                            >
                                <MenuItem value="ROLE_USER">User</MenuItem>
                                <MenuItem value="ROLE_ADMIN">Admin</MenuItem>
                            </Select>
                        </FormControl>
                        <FormControlLabel
                            control={
                                <Switch
                                    checked={formData.enabled}
                                    onChange={handleInputChange}
                                    name="enabled"
                                />
                            }
                            label="Enabled"
                        />
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCloseDialog}>Cancel</Button>
                    <Button onClick={handleSubmit} variant="contained">
                        {editingUser ? 'Update' : 'Create'}
                    </Button>
                </DialogActions>
            </Dialog>

            <Snackbar
                open={snackbar.open}
                autoHideDuration={6000}
                onClose={handleCloseSnackbar}
            >
                <Alert
                    onClose={handleCloseSnackbar}
                    severity={snackbar.severity}
                    sx={{ width: '100%' }}
                >
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default UserManagement; 