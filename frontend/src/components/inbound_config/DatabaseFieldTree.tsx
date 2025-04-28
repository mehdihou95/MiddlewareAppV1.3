import React, { useState, useCallback, useEffect } from 'react';
import { 
  List, 
  ListItem, 
  ListItemText, 
  ListItemButton, 
  Collapse,
  TextField,
  Box,
  IconButton,
  Tooltip
} from '@mui/material';
import { 
  ExpandLess, 
  ExpandMore, 
  Search as SearchIcon,
  UnfoldLess as CollapseAllIcon,
  UnfoldMore as ExpandAllIcon
} from '@mui/icons-material';
import { DatabaseField } from '../../services/inbound_config/types';

interface DatabaseFieldTreeProps {
  fields: DatabaseField[];
  onFieldClick: (field: DatabaseField) => void;
  selectedField?: string;
  interfaceType?: string;
}

interface DatabaseFieldNodeProps {
  field: DatabaseField;
  onFieldClick: (field: DatabaseField) => void;
  selectedField?: string;
  level?: number;
  searchTerm?: string;
  expandedNodes: Set<string>;
  toggleNode: (nodePath: string) => void;
}

const DatabaseFieldNode: React.FC<DatabaseFieldNodeProps> = ({ 
  field, 
  onFieldClick, 
  selectedField,
  level = 0,
  searchTerm = '',
  expandedNodes,
  toggleNode
}) => {
  const tableName = field.field.split('.')[0];
  const fieldName = field.field.split('.')[1];
  const isSelected = selectedField === field.field;

  const handleClick = () => {
    onFieldClick(field);
  };

  // Improved search logic that checks recursively
  const matchesSearch = (field: DatabaseField): boolean => {
    if (!searchTerm) return true;
    
    const termLower = searchTerm.toLowerCase();
    
    // Check if current field matches
    return field.field.toLowerCase().includes(termLower) ||
           field.type.toLowerCase().includes(termLower);
  };

  // If there's a search term and this field doesn't match, don't render
  if (searchTerm && !matchesSearch(field)) {
    return null;
  }

  return (
    <ListItem 
      disablePadding 
      sx={{ pl: level * 2 }}
    >
      <ListItemButton 
        onClick={handleClick}
        selected={isSelected}
        sx={{ 
          borderRadius: 1,
          '&.Mui-selected': {
            backgroundColor: 'primary.light',
            '&:hover': {
              backgroundColor: 'primary.light',
            },
          },
        }}
      >
        <ListItemText 
          primary={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <span>{fieldName}</span>
            </Box>
          }
          secondary={`${tableName} (${field.type})`}
          sx={{
            '.MuiListItemText-primary': {
              fontWeight: 'normal',
            },
            '.MuiListItemText-secondary': {
              fontSize: '0.75rem',
              color: 'text.secondary',
            },
          }}
        />
      </ListItemButton>
    </ListItem>
  );
};

const DatabaseFieldTree: React.FC<DatabaseFieldTreeProps> = ({ 
  fields, 
  onFieldClick,
  selectedField,
  interfaceType 
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [expandedNodes, setExpandedNodes] = useState<Set<string>>(new Set());

  // Filter fields based on interface type
  const filteredFields = React.useMemo(() => {
    if (!interfaceType) return fields;
    
    // Filter fields based on table name prefix
    return fields.filter(field => {
      const tableName = field.field.split('.')[0];
      return tableName.startsWith(interfaceType.toUpperCase());
    });
  }, [fields, interfaceType]);

  const toggleNode = useCallback((nodePath: string) => {
    setExpandedNodes(prev => {
      const next = new Set(prev);
      if (next.has(nodePath)) {
        next.delete(nodePath);
      } else {
        next.add(nodePath);
      }
      return next;
    });
  }, []);

  const expandAll = () => {
    const allNodes = new Set<string>();
    // Add all table nodes
    const tables = new Set(fields.map(field => field.field.split('.')[0]));
    tables.forEach(table => {
      allNodes.add(`table-${table}`);
    });
    setExpandedNodes(allNodes);
  };

  const collapseAll = () => {
    setExpandedNodes(new Set());
  };

  // Group fields by table using filteredFields instead of fields
  const groupedFields = filteredFields.reduce((acc, field) => {
    const tableName = field.field.split('.')[0];
    if (!acc[tableName]) {
      acc[tableName] = [];
    }
    acc[tableName].push(field);
    return acc;
  }, {} as { [key: string]: DatabaseField[] });

  return (
    <Box>
      <Box sx={{ p: 1, display: 'flex', gap: 1, alignItems: 'center' }}>
        <TextField
          size="small"
          placeholder="Search database fields..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          InputProps={{
            startAdornment: <SearchIcon color="action" sx={{ mr: 1 }} />,
          }}
          sx={{ flex: 1 }}
        />
        <Tooltip title="Expand All">
          <IconButton onClick={expandAll} size="small">
            <ExpandAllIcon />
          </IconButton>
        </Tooltip>
        <Tooltip title="Collapse All">
          <IconButton onClick={collapseAll} size="small">
            <CollapseAllIcon />
          </IconButton>
        </Tooltip>
      </Box>

      <List sx={{ 
        maxHeight: 'calc(60vh - 56px)',
        overflow: 'auto',
        '& .MuiListItem-root': {
          borderBottom: '1px solid',
          borderColor: 'divider',
        },
        '& .MuiListItem-root:last-child': {
          borderBottom: 'none',
        },
      }}>
        {Object.entries(groupedFields).map(([tableName, tableFields]) => {
          const tableNodePath = `table-${tableName}`;
          const isTableExpanded = expandedNodes.has(tableNodePath);
          
          // If searching, check if any fields in this table match
          const hasMatchingFields = searchTerm ? tableFields.some(field => 
            field.field.toLowerCase().includes(searchTerm.toLowerCase()) ||
            field.type.toLowerCase().includes(searchTerm.toLowerCase())
          ) : true;

          if (!hasMatchingFields && searchTerm) {
            return null;
          }

          return (
            <div key={tableName}>
              <ListItem 
                disablePadding
                sx={{ 
                  bgcolor: 'background.paper',
                  '&:hover': {
                    bgcolor: 'action.hover',
                  },
                }}
              >
                <ListItemButton onClick={() => toggleNode(tableNodePath)}>
                  <ListItemText 
                    primary={tableName}
                    sx={{ 
                      '.MuiListItemText-primary': {
                        fontWeight: 'bold',
                        color: 'text.primary'
                      }
                    }}
                  />
                  {isTableExpanded ? <ExpandLess /> : <ExpandMore />}
                </ListItemButton>
              </ListItem>
              <Collapse in={isTableExpanded} timeout="auto" unmountOnExit>
                <List disablePadding>
                  {tableFields.map((field, index) => (
                    <DatabaseFieldNode
                      key={`${field.field}-${index}`}
                      field={field}
                      onFieldClick={onFieldClick}
                      selectedField={selectedField}
                      level={1}
                      searchTerm={searchTerm}
                      expandedNodes={expandedNodes}
                      toggleNode={toggleNode}
                    />
                  ))}
                </List>
              </Collapse>
            </div>
          );
        })}
      </List>
    </Box>
  );
};

export default DatabaseFieldTree; 