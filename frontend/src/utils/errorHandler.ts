import axios, { AxiosError } from 'axios';
import { ApiError, ErrorResponse, ValidationError } from '../services/core/types';

export type { ApiError };

export const handleApiError = (error: unknown): ApiError => {
    if (axios.isAxiosError(error)) {
        const axiosError = error as AxiosError<ErrorResponse>;
        const errorResponse = axiosError.response?.data;

        if (errorResponse) {
            const apiError: ApiError = {
                code: errorResponse.code || 'UNKNOWN_ERROR',
                message: errorResponse.message || 'An unexpected error occurred',
                details: errorResponse.details,
                timestamp: errorResponse.timestamp,
                path: errorResponse.path,
                status: axiosError.response?.status,
            };

            // Handle validation errors if present
            if (axiosError.response?.status === 400 && errorResponse.details) {
                const validationErrors = Array.isArray(errorResponse.details)
                    ? errorResponse.details.map((detail: string) => {
                        const [field, message] = detail.split(': ');
                        return { field, message };
                    })
                    : [];
                apiError.validationErrors = validationErrors;
            }

            return apiError;
        }
    }

    // Handle non-Axios errors
    const unknownError: ApiError = {
        code: 'UNKNOWN_ERROR',
        message: error instanceof Error ? error.message : 'An unknown error occurred',
    };

    return unknownError;
};

export const isApiError = (error: unknown): error is ApiError => {
    return typeof error === 'object' && error !== null && 'message' in error && 'code' in error;
};

export const getErrorMessage = (error: unknown): string => {
    if (error instanceof Error) {
        return error.message;
    }
    if (typeof error === 'string') {
        return error;
    }
    return 'An unexpected error occurred';
};

export const isAuthenticationError = (error: unknown): boolean => {
    if (isApiError(error)) {
        return error.status === 401 || error.status === 403;
    }

    // Type guard for Axios error
    const isAxiosError = (error: unknown): error is AxiosError => {
        return typeof error === 'object' && error !== null && 'isAxiosError' in error;
    };

    if (isAxiosError(error)) {
        return error.response?.status === 401 || error.response?.status === 403;
    }
    return false;
};

export const handleApiErrorOld = (error: unknown, setError: (message: string) => void) => {
    if (error && typeof error === 'object' && 'response' in error) {
        const response = (error as any).response?.data;

        if (response?.validationErrors) {
            // Handle validation errors
            const errorMessages = response.validationErrors.join(', ');
            setError(errorMessages);
        } else if (response?.message) {
            // Handle specific error messages from the server
            setError(response.message);
        } else {
            // Handle generic error messages
            switch ((error as any).response?.status) {
                case 400:
                    setError('Invalid request. Please check your input.');
                    break;
                case 401:
                    setError('Unauthorized. Please log in again.');
                    break;
                case 403:
                    setError('Access denied. You do not have permission to perform this action.');
                    break;
                case 404:
                    setError('Resource not found.');
                    break;
                case 500:
                    setError('Server error. Please try again later.');
                    break;
                default:
                    setError('An unexpected error occurred.');
            }
        }
    } else {
        setError('An unexpected error occurred.');
    }
};

export const isValidationError = (error: unknown): boolean => {
    if (error && typeof error === 'object' && 'response' in error) {
        const response = (error as any).response?.data;
        return response?.validationErrors !== undefined;
    }
    return false;
};

export const getValidationErrors = (error: unknown): string[] => {
    if (error && typeof error === 'object' && 'response' in error) {
        const response = (error as any).response?.data;
        return response?.validationErrors || [];
    }
    return [];
};

export const handleValidationErrors = (error: any, setFormErrors: (errors: any) => void) => {
  if (error.response?.status === 400 && error.response?.data?.message) {
    const errorMessage = error.response.data.message;
    const errors: Record<string, string> = {};
    
    // Parse the error message to identify error fields
    if (errorMessage.includes('name')) {
      errors.name = 'Name error: ' + errorMessage;
    }
    if (errorMessage.includes('code')) {
      errors.code = 'Code error: ' + errorMessage;
    }
    if (errorMessage.includes('status')) {
      errors.status = 'Status error: ' + errorMessage;
    }
    
    setFormErrors(errors);
    return true;
  }
  return false;
}; 