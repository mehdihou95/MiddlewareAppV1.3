import { PageResponse, Sort } from '../core/types';

export interface User {
  id: number;
  username: string;
  password?: string;
  email: string;
  firstName: string;
  lastName: string;
  enabled: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
  lastLogin?: string;
  failedLoginAttempts?: number;
  accountLocked: boolean;
  passwordResetToken?: string;
  passwordResetExpiry?: string;
}

export interface AuditLog {
  id: number;
  action: string;
  username: string;
  clientId?: number;
  details: string;
  ipAddress: string;
  userAgent?: string;
  requestMethod?: string;
  requestUrl?: string;
  requestParams?: string;
  responseStatus?: number;
  errorMessage?: string;
  createdAt: string;
  executionTime?: number;
}

export interface Client {
  id: number;
  name: string;
  code: string;
  description?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface ClientInput {
  name: string;
  code: string;
  description?: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Interface {
  id: number;
  name: string;
  type: string;
  description?: string;
  schemaPath?: string;
  rootElement: string;
  namespace?: string;
  client: {
    id: number;
  };
  isActive: boolean;
  priority: number;
  status?: string;
  configuration?: {
    xsdPath?: string;
  };
  createdAt?: string;
  updatedAt?: string;
}

export interface InterfaceMapping {
  id?: number;
  name: string;
  xmlPath: string;
  databaseField: string;
  transformation?: string;
  transformationRule?: string;
  isRequired: boolean;
  isAttribute: boolean;
  dataType: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
  priority?: number;
  isActive?: boolean;
  client?: {
    id: number;
  };
  interfaceEntity?: {
    id: number;
  };
  sourceField?: string;
  targetField?: string;
} 