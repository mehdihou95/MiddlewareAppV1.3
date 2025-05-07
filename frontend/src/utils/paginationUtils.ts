export interface PaginationParams {
  page: number;
  size: number;
  sort: string;
  direction: string;
  filter?: string;
}

export const createPaginationParams = (params: PaginationParams): URLSearchParams => {
  const searchParams = new URLSearchParams();
  searchParams.append('page', params.page.toString());
  searchParams.append('size', params.size.toString());
  searchParams.append('sort', params.sort);
  searchParams.append('direction', params.direction);
  
  if (params.filter) {
    searchParams.append('filter', params.filter);
  }
  
  return searchParams;
};

export const getDefaultPaginationParams = (): PaginationParams => ({
  page: 0,
  size: 10,
  sort: 'name',
  direction: 'asc'
}); 