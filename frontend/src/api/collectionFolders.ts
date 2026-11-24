import { del, get, post, put } from './client';
import type { PageResponse } from '@/types/api';

export interface CollectionFolderDto {
  folderId: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

const path = '/api/user/collection-folders';

export const listCollectionFolders = (page: number, size: number, signal?: AbortSignal) =>
  get<PageResponse<CollectionFolderDto>>(path, { signal, query: { page, size, sort: 'createdAt,desc' } });

export const createCollectionFolder = (name: string) => post<CollectionFolderDto>(path, { name });

export const renameCollectionFolder = (folderId: string, name: string) =>
  put<CollectionFolderDto>(`${path}/${encodeURIComponent(folderId)}`, { name });

export const deleteCollectionFolder = (folderId: string) => del(`${path}/${encodeURIComponent(folderId)}`);
