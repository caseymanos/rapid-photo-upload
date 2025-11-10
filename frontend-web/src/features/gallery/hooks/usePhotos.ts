import { useState, useEffect, useRef, useCallback } from 'react';
import { photoApi } from '@/shared/api/endpoints';
import { PaginatedPhotosResponse, PhotoResponse } from '@/shared/types';
import { usePhotoCache } from '../context/PhotoCacheContext';

const PAGE_SIZE = 30;

export const usePhotos = () => {
  const { cache, setCache, clearCache, isStale } = usePhotoCache();
  const [photos, setPhotos] = useState<PhotoResponse[]>(cache?.photos || []);
  const [totalPhotos, setTotalPhotos] = useState<number>(cache?.total ?? cache?.photos.length ?? 0);
  const [page, setPage] = useState<number>(cache?.lastFetchedPage ?? -1);
  const [hasMore, setHasMore] = useState<boolean>(cache?.hasNext ?? true);
  const [isLoading, setIsLoading] = useState<boolean>(!cache || isStale());
  const [isLoadingMore, setIsLoadingMore] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const hasInitializedRef = useRef(false);

  const persistCache = useCallback(
    (updatedPhotos: PhotoResponse[], response: PaginatedPhotosResponse) => {
      setCache({
        photos: updatedPhotos,
        total: response.totalElements,
        lastFetchedPage: response.page,
        hasNext: response.hasNext,
      });
    },
    [setCache]
  );

  const fetchPhotos = useCallback(
    async ({ pageToLoad = 0, append = false }: { pageToLoad?: number; append?: boolean } = {}) => {
      if (append) {
        setIsLoadingMore(true);
      } else {
        setIsLoading(true);
      }
      setError(null);

      try {
        const response = await photoApi.getPhotos({
          page: pageToLoad,
          size: PAGE_SIZE,
          includeDownloadUrl: false,
        });

        const newItems = response.data.items || [];

        setPhotos((prev) => {
          const nextPhotos = append ? [...prev, ...newItems] : newItems;
          persistCache(nextPhotos, response.data);
          return nextPhotos;
        });

        setTotalPhotos(response.data.totalElements || 0);
        setPage(response.data.page || 0);
        setHasMore(response.data.hasNext || false);
      } catch (err: any) {
        const message = err.response?.data?.message || 'Failed to load photos';
        setError(message);
        throw new Error(message);
      } finally {
        if (append) {
          setIsLoadingMore(false);
        } else {
          setIsLoading(false);
        }
      }
    },
    [persistCache]
  );

  useEffect(() => {
    if (cache && !isStale()) {
      setPhotos(cache.photos);
      setTotalPhotos(cache.total ?? cache.photos.length);
      setPage(cache.lastFetchedPage ?? Math.max(0, Math.ceil(cache.photos.length / PAGE_SIZE) - 1));
      setHasMore(cache.hasNext ?? true);
      setIsLoading(false);
      hasInitializedRef.current = true;
      return;
    }

    if (hasInitializedRef.current) {
      return;
    }

    hasInitializedRef.current = true;
    fetchPhotos();
  }, [cache, fetchPhotos, isStale]);

  const loadMore = useCallback(async () => {
    if (!hasMore || isLoadingMore) {
      return;
    }
    await fetchPhotos({ pageToLoad: page + 1, append: true });
  }, [fetchPhotos, hasMore, isLoadingMore, page]);

  const refetch = useCallback(async () => {
    clearCache();
    await fetchPhotos({ pageToLoad: 0, append: false });
  }, [clearCache, fetchPhotos]);

  const updatePhotoMetadata = useCallback(
    async (photoId: string, tags?: string[], metadata?: any) => {
      try {
        await photoApi.updatePhotoMetadata(photoId, { tags, metadata });
        await refetch();
      } catch (err: any) {
        throw new Error(err.response?.data?.message || 'Failed to update photo');
      }
    },
    [refetch]
  );

  const deletePhoto = useCallback(
    async (photoId: string) => {
      try {
        await photoApi.deletePhoto(photoId);
        await refetch();
      } catch (err: any) {
        throw new Error(err.response?.data?.message || 'Failed to delete photo');
      }
    },
    [refetch]
  );

  const deleteAllPhotos = useCallback(async () => {
    try {
      await photoApi.deleteAllPhotos();
      await refetch();
    } catch (err: any) {
      throw new Error(err.response?.data?.message || 'Failed to delete photos');
    }
  }, [refetch]);

  const deleteSelectedPhotos = useCallback(
    async (photoIds: string[]) => {
      if (!photoIds.length) {
        return;
      }
      try {
        await photoApi.deletePhotos(photoIds);
        await refetch();
      } catch (err: any) {
        throw new Error(err.response?.data?.message || 'Failed to delete selected photos');
      }
    },
    [refetch]
  );

  return {
    photos,
    totalPhotos,
    isLoading,
    isLoadingMore,
    error,
    hasMore,
    loadMore,
    refetch,
    updatePhotoMetadata,
    deletePhoto,
    deleteAllPhotos,
    deleteSelectedPhotos,
  };
};
