import axios from 'axios';
import { tokenService } from './tokenService';

interface ErrorResponse {
    type: string;
    message: string;
    details?: string[];
}

interface AxiosErrorResponse {
    code?: string;
    message?: string;
}

export const handleError = (error: unknown): ErrorResponse => {
    // Type guard for Axios error
    const isAxiosError = (error: unknown): error is any => {
        return typeof error === 'object' && error !== null && 'isAxiosError' in error;
    };

    if (isAxiosError(error)) {
        const response = error.response?.data as AxiosErrorResponse;

        if (response?.code === 'AUTH_001') {
            // Handle expired token errors
            return {
                type: 'TOKEN_EXPIRED',
                message: 'Your session has expired. Please log in again.'
            };
        } else if (response?.code === 'AUTH_002') {
            // Handle invalid token errors
            tokenService.clearTokens();
            return {
                type: 'INVALID_TOKEN',
                message: 'Your session has expired. Please log in again.'
            };
        } else if (error.response?.status === 401) {
            // Handle other unauthorized errors
            tokenService.clearTokens();
            return {
                type: 'UNAUTHORIZED',
                message: 'You are not authorized to perform this action'
            };
        } else if (error.response?.status === 403) {
            return {
                type: 'FORBIDDEN',
                message: 'You do not have permission to perform this action'
            };
        } else if (error.response?.status === 404) {
            return {
                type: 'NOT_FOUND',
                message: 'The requested resource was not found'
            };
        } else if (response?.message) {
            return {
                type: 'API_ERROR',
                message: response.message
            };
        }
    }

    // Handle network errors
    if (error instanceof Error) {
        return {
            type: 'NETWORK_ERROR',
            message: error.message
        };
    }

    // Handle unknown errors
    return {
        type: 'UNKNOWN_ERROR',
        message: 'An unexpected error occurred'
    };
}; 