export interface PageResponse<T> {
  content: T[];
  pageable: {
    sort: Sort;
    pageNumber: number;
    pageSize: number;
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  sort: Sort;
  numberOfElements: number;
  size: number;
  number: number;
  empty: boolean;
}

export interface Sort {
  sorted: boolean;
  unsorted: boolean;
  empty: boolean;
}

// API Error Types
export interface ErrorResponse {
  code: string;
  message: string;
  details?: string | string[];
  timestamp?: string;
  path?: string;
  status?: number;
}

export interface ValidationError {
  field: string;
  message: string;
}

export interface ApiError extends ErrorResponse {
  validationErrors?: ValidationError[];
}

// API Response Type
export interface ApiResponse<T> {
  status: number;
  error?: string;
  message?: string;
  timestamp: string;
  details?: string[];
  data?: T;
} 