import {
  createContext,
  useContext,
  useState,
  useCallback,
  ReactNode,
  useEffect,
  useRef,
} from 'react';
import { PhotoResponse } from '@/shared/types';
import { photoApi } from '@/shared/api/endpoints';
import { useAuthStore } from '@/features/auth/store/authStore';

interface PhotoCache {
  photos: PhotoResponse[];
  total: number;
  lastFetchedPage: number;
  hasNext: boolean;
  timestamp: number;
}

interface CachePayload {
  photos: PhotoResponse[];
  total: number;
  lastFetchedPage: number;
  hasNext: boolean;
}

interface PhotoCacheContextType {
  cache: PhotoCache | null;
  setCache: (payload: CachePayload) => void;
  clearCache: () => void;
  isStale: () => boolean;
}

const PhotoCacheContext = createContext<PhotoCacheContextType | null>(null);

const CACHE_TTL_MS = 30_000; // Cache stays fresh for 30 seconds

export const PhotoCacheProvider = ({ children }: { children: ReactNode }) => {
  const [cache, setCacheState] = useState<PhotoCache | null>(null);
  const hasPrefetchedRef = useRef(false);
  const { isAuthenticated } = useAuthStore();

  const setCache = useCallback(({ photos, total, lastFetchedPage, hasNext }: CachePayload) => {
    setCacheState({
      photos,
      total,
      lastFetchedPage,
      hasNext,
      timestamp: Date.now(),
    });
  }, []);

  const clearCache = useCallback(() => {
    setCacheState(null);
  }, []);

  const isStale = useCallback(() => {
    if (!cache) {
      return true;
    }
    return Date.now() - cache.timestamp > CACHE_TTL_MS;
  }, [cache]);

  useEffect(() => {
    if (!isAuthenticated) {
      hasPrefetchedRef.current = false;
      clearCache();
      return;
    }

    if (hasPrefetchedRef.current || (cache && !isStale())) {
      hasPrefetchedRef.current = true;
      return;
    }

    let cancelled = false;

    const preload = async () => {
      try {
        const start = performance.now();
        const response = await photoApi.getPhotos({ page: 0, size: 30, includeDownloadUrl: false });
        if (cancelled) return;
        const items = response.data.items || [];
        setCache({
          photos: items,
          total: response.data.totalElements || 0,
          lastFetchedPage: response.data.page || 0,
          hasNext: response.data.hasNext || false,
        });
        const duration = performance.now() - start;
        console.log(
          `[Performance] Prefetched ${items.length} of ${response.data.totalElements || 0} photos on app load in ${duration.toFixed(
            2
          )}ms`
        );
      } catch (error) {
        if (!cancelled) {
          console.debug('Initial gallery preload failed (non-blocking)', error);
        }
      } finally {
        if (!cancelled) {
          hasPrefetchedRef.current = true;
        }
      }
    };

    preload();

    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, cache, isStale, setCache, clearCache]);

  return (
    <PhotoCacheContext.Provider value={{ cache, setCache, clearCache, isStale }}>
      {children}
    </PhotoCacheContext.Provider>
  );
};

export const usePhotoCache = () => {
  const context = useContext(PhotoCacheContext);
  if (!context) {
    throw new Error('usePhotoCache must be used within PhotoCacheProvider');
  }
  return context;
};
