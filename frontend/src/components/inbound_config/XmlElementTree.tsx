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
  Tooltip,
  Chip,
  Typography,
  Alert
} from '@mui/material';
import { 
  ExpandLess, 
  ExpandMore, 
  Search as SearchIcon,
  UnfoldLess as CollapseAllIcon,
  UnfoldMore as ExpandAllIcon,
  Label as LabelIcon,
  Info as InfoIcon
} from '@mui/icons-material';
import { XsdElement } from '../../services/inbound_config/types';
import { XsdNamespace } from '../../services/inbound_config/xsdService';

interface XmlElementTreeProps {
  elements: XsdElement[];
  onElementClick: (element: XsdElement, fullPath: string) => void;
  selectedElement?: XsdElement | null;
  namespaces?: XsdNamespace[];
}

interface XmlElementNodeProps {
  element: XsdElement;
  onElementClick: (element: XsdElement, fullPath: string) => void;
  selectedElement?: XsdElement | null;
  level?: number;
  searchTerm?: string;
  expandedNodes: Set<string>;
  toggleNode: (nodePath: string) => void;
  parentPath?: string;
  namespaces?: XsdNamespace[];
}

const XmlElementNode: React.FC<XmlElementNodeProps> = ({ 
  element, 
  onElementClick, 
  selectedElement,
  level = 0,
  searchTerm = '',
  expandedNodes,
  toggleNode,
  parentPath = '',
  namespaces = []
}) => {
  const hasChildren = element.elements && element.elements.length > 0;
  const isSelected = selectedElement?.name === element.name && 
                     selectedElement?.type === element.type;
  const nodePath = `${level}-${element.name}-${element.type || ''}`;
  const isExpanded = expandedNodes.has(nodePath);
  
  // Auto-expand parent nodes when children match search
  useEffect(() => {
    if (searchTerm && hasChildren && matchesSearch(element)) {
      toggleNode(nodePath);
    }
  }, [searchTerm, hasChildren, element, toggleNode, nodePath]);

  // Build the full XML path, handling namespaces appropriately
  const getFullPath = () => {
    let elementName = element.name;
    if (elementName.includes('.')) {
      elementName = elementName.split('.').pop() || elementName;
    }
    const elementPath = `*[local-name()='${elementName}']`;
    if (!parentPath) {
      return `/${elementPath}`;
    }
    return `${parentPath}/${elementPath}`;
  };
  
  const currentPath = getFullPath();

  const handleClick = () => {
    if (hasChildren) {
      toggleNode(nodePath);
    }
    onElementClick(element, currentPath);
  };

  const getElementInfo = () => {
    const info = [element.name];
    if (element.type && element.type !== 'compositor') {
      info.push(`(${element.type})`);
    }
    if (element.maxOccurs === 'unbounded') {
      info.push('[*]');
    } else if (element.minOccurs === '0' && element.maxOccurs === '1') {
      info.push('[?]');
    } else if (element.minOccurs && element.maxOccurs) {
      info.push(`[${element.minOccurs}..${element.maxOccurs}]`);
    }
    return info.join(' ');
  };

  // Improved search logic that checks children recursively
  const matchesSearch = (el: XsdElement): boolean => {
    if (!searchTerm) return true;
    
    const termLower = searchTerm.toLowerCase();
    
    // Check if current element matches
    const currentMatches = 
      el.name.toLowerCase().includes(termLower) ||
      currentPath.toLowerCase().includes(termLower) ||
      (el.type && el.type.toLowerCase().includes(termLower));

    // If current element matches, no need to check children
    if (currentMatches) return true;

    // Check children recursively
    if (el.elements) {
      return el.elements.some(child => matchesSearch(child));
    }

    return false;
  };

  // If there's a search term and neither this element nor its children match, don't render
  if (searchTerm && !matchesSearch(element)) {
    return null;
  }

  // Skip rendering compositor elements directly, but process their children
  if (element.type === 'compositor') {
    return (
      <Collapse in={true} timeout="auto" unmountOnExit>
        <List disablePadding>
          {element.elements?.map((child, index) => (
            <XmlElementNode
              key={`${child.name}-${child.type || ''}-${index}`}
              element={child}
              onElementClick={onElementClick}
              selectedElement={selectedElement}
              level={level}
              searchTerm={searchTerm}
              expandedNodes={expandedNodes}
              toggleNode={toggleNode}
              parentPath={parentPath}
              namespaces={namespaces}
            />
          ))}
        </List>
      </Collapse>
    );
  }

  // Skip rendering schema element directly
  if (element.type === 'schema') {
    return (
      <Collapse in={true} timeout="auto" unmountOnExit>
        <List disablePadding>
          {element.elements?.map((child, index) => (
            <XmlElementNode
              key={`${child.name}-${child.type || ''}-${index}`}
              element={child}
              onElementClick={onElementClick}
              selectedElement={selectedElement}
              level={level}
              searchTerm={searchTerm}
              expandedNodes={expandedNodes}
              toggleNode={toggleNode}
              parentPath={parentPath}
              namespaces={namespaces}
            />
          ))}
        </List>
      </Collapse>
    );
  }

  return (
    <>
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
                <span>{getElementInfo()}</span>
                {element.namespace && (
                  <Tooltip title={`Namespace: ${element.namespace}`}>
                    <Chip 
                      icon={<LabelIcon fontSize="small" />} 
                      label={element.prefix || "default"} 
                      size="small" 
                      variant="outlined" 
                      color="primary"
                    />
                  </Tooltip>
                )}
                {element.documentation && (
                  <Tooltip title={element.documentation}>
                    <InfoIcon fontSize="small" color="info" />
                  </Tooltip>
                )}
              </Box>
            }
            secondary={currentPath}
            sx={{
              '.MuiListItemText-primary': {
                fontWeight: hasChildren ? 'bold' : 'normal',
              },
              '.MuiListItemText-secondary': {
                fontSize: '0.75rem',
                color: 'text.secondary',
              },
            }}
          />
          {hasChildren && (
            <Box component="span" sx={{ ml: 1 }}>
              {isExpanded ? <ExpandLess /> : <ExpandMore />}
            </Box>
          )}
        </ListItemButton>
      </ListItem>
      {hasChildren && isExpanded && (
        <Collapse in={true} timeout="auto" unmountOnExit>
          <List disablePadding>
            {element.elements?.map((child, index) => (
              <XmlElementNode
                key={`${child.name}-${child.type || ''}-${index}`}
                element={child}
                onElementClick={onElementClick}
                selectedElement={selectedElement}
                level={level + 1}
                searchTerm={searchTerm}
                expandedNodes={expandedNodes}
                toggleNode={toggleNode}
                parentPath={currentPath}
                namespaces={namespaces}
              />
            ))}
          </List>
        </Collapse>
      )}
    </>
  );
};

const XmlElementTree: React.FC<XmlElementTreeProps> = ({ 
  elements, 
  onElementClick,
  selectedElement,
  namespaces = []
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [expandedNodes, setExpandedNodes] = useState<Set<string>>(new Set());
  const [hasNamespaces, setHasNamespaces] = useState(false);
  const [hasElements, setHasElements] = useState(true);
  const [isInitialized, setIsInitialized] = useState(false);

  // Initialize expanded nodes only once when the component mounts
  useEffect(() => {
    if (!isInitialized && elements && elements.length > 0) {
      const firstLevelNodes = new Set<string>();
      elements.forEach((el) => {
        firstLevelNodes.add(`0-${el.name}-${el.type || ''}`);
      });
      setExpandedNodes(firstLevelNodes);
      setIsInitialized(true);
    }
  }, [elements, isInitialized]);

  // Separate effect for handling element and namespace changes
  useEffect(() => {
    setHasNamespaces(namespaces && namespaces.length > 0);
    setHasElements(elements && elements.length > 0);
  }, [elements, namespaces]);

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
    const addAllNodes = (els: XsdElement[], level: number) => {
      els.forEach(el => {
        allNodes.add(`${level}-${el.name}-${el.type || ''}`);
        if (el.elements) {
          addAllNodes(el.elements, level + 1);
        }
      });
    };
    addAllNodes(elements, 0);
    setExpandedNodes(allNodes);
  };

  const collapseAll = () => {
    setExpandedNodes(new Set());
  };

  // Process elements to handle special cases
  const processedElements = elements.filter(el => el.type !== 'mapping_rule');

  return (
    <Box>
      <Box sx={{ p: 1, display: 'flex', gap: 1, alignItems: 'center' }}>
        <TextField
          size="small"
          placeholder="Search XML elements..."
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
      
      {hasNamespaces && (
        <Box sx={{ p: 1, mb: 1, bgcolor: 'background.paper', borderRadius: 1 }}>
          <Typography variant="subtitle2" gutterBottom>
            Namespaces:
          </Typography>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
            {namespaces.map((ns, index) => (
              <Chip 
                key={index}
                label={`${ns.prefix === '_default_' ? 'default' : ns.prefix}: ${ns.uri}`}
                size="small"
                variant="outlined"
                color="primary"
              />
            ))}
          </Box>
        </Box>
      )}
      
      {!hasElements && (
        <Alert severity="info" sx={{ m: 1 }}>
          No XML elements found in the XSD structure. Please check if the XSD file is valid and contains element definitions.
        </Alert>
      )}
      
      <List sx={{ 
        maxHeight: 'calc(60vh - 56px)', // Account for search bar height
        overflow: 'auto',
        '& .MuiListItem-root': {
          borderBottom: '1px solid',
          borderColor: 'divider',
        },
        '& .MuiListItem-root:last-child': {
          borderBottom: 'none',
        },
      }}>
        {processedElements.map((element, index) => (
          <XmlElementNode
            key={`${element.name}-${element.type || ''}-${index}`}
            element={element}
            onElementClick={onElementClick}
            selectedElement={selectedElement}
            searchTerm={searchTerm}
            expandedNodes={expandedNodes}
            toggleNode={toggleNode}
            namespaces={namespaces}
          />
        ))}
      </List>
    </Box>
  );
};

export default XmlElementTree;
