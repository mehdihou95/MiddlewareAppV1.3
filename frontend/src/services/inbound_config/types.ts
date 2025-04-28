import { Interface, InterfaceMapping } from '../administration/types';
import { PageResponse } from '../core/types';

export interface MappingRule {
  id?: number;
  clientId: number;
  interfaceId: number;
  name: string;
  xmlPath: string;
  databaseField: string;
  xsdElement: string;
  tableName: string;
  dataType: string;
  isAttribute: boolean;
  description: string;
  sourceField?: string;
  targetField?: string;
  transformationType?: string;
  transformationRule?: string;
  required?: boolean;
  defaultValue?: string;
  priority?: number;
  isActive?: boolean;
  isDefault?: boolean;
  validationRule?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProcessedFile {
  id: number;
  fileName: string;
  status: string;
  errorMessage?: string;
  interfaceEntity: Interface;
  processedData?: Record<string, any>;
  processedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface XsdElement {
  name: string;
  type?: string;
  typeNamespace?: string;
  namespace?: string;
  prefix?: string;
  hasComplexType?: boolean;
  hasSimpleType?: boolean;
  elements?: XsdElement[];
  compositor?: 'sequence' | 'choice' | 'all';
  minOccurs?: string;
  maxOccurs?: string;
  attributes?: { [key: string]: string };
  documentation?: string;
  baseType?: string;
  facets?: Array<{ type: string; value: string }>;
  imports?: Array<{ namespace: string; schemaLocation: string }>;
  path?: string;
}

export interface DatabaseField {
  field: string;
  type: string;
  table: string;
  required: boolean;
  description?: string;
} 