import { Client, Interface } from '../administration/types';

// Placeholder for future listener configuration types
export interface BaseConnectorConfig {
  id: number;
  name: string;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SftpConfig {
  id?: number;
  clientId: number;
  interfaceId: number;
  host: string;
  port: number;
  username: string;
  password?: string;
  privateKeyPath?: string;
  privateKeyPassphrase?: string;
  remoteDirectory: string;
  monitoredDirectories: string[];
  filePattern: string;
  pollingInterval: number;
  connectionTimeout: number;
  retryAttempts: number;
  retryDelay: number;
  is_active: boolean;
}

// Type for creating/updating SFTP configurations
export interface SftpConfigCreate extends Omit<SftpConfig, 'id'> {}

export interface As2Config {
  id?: number;
  client: Client;
  interfaceConfig: Interface;
  partnerId: string;
  localId: string;
  apiName: string;
  encryptionAlgorithm: string;
  signatureAlgorithm: string;
  compression: boolean;
  mdnMode: string;
  mdnDigestAlgorithm: string;
  mdnUrl?: string;
  encryptMessage: boolean;
  signMessage: boolean;
  requestMdn: boolean;
  isActive: boolean;
}

export interface ApiConfig extends BaseConnectorConfig {
  endpoint: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  headers: Record<string, string>;
  authentication: {
    type: 'Basic' | 'Bearer' | 'OAuth2';
    credentials?: string;
  };
} 