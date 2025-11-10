import { useCallback, useEffect, useState } from 'react';
import { photoApi } from '@/shared/api/endpoints';

const DOWNLOAD_URL_CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour
const downloadUrlCache = new Map<string, { url: string; cachedAt: number }>();

const getCachedUrl = (photoId: string) => {
  const cached = downloadUrlCache.get(photoId);
  if (!cached) {
    return undefined;
  }
  if (Date.now() - cached.cachedAt > DOWNLOAD_URL_CACHE_TTL_MS) {
    downloadUrlCache.delete(photoId);
    return undefined;
  }
  return cached.url;
};

const cacheUrl = (photoId: string, url: string) => {
  downloadUrlCache.set(photoId, { url, cachedAt: Date.now() });
};

export const usePhotoDownloadUrl = (
  photoId: string,
  initialUrl?: string
) => {
  const cachedUrl = initialUrl || getCachedUrl(photoId);
  const [url, setUrl] = useState<string | undefined>(cachedUrl);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchUrl = useCallback(
    async (forceRefresh = false) => {
      if (!forceRefresh) {
        const cached = getCachedUrl(photoId);
        if (cached) {
          setUrl(cached);
          return cached;
        }
        if (url) {
          return url;
        }
      }

      if (isLoading) {
        return url;
      }

      setIsLoading(true);
      setError(null);
      try {
        const response = await photoApi.getPhotoDownloadUrl(photoId);
        const nextUrl = response.data.downloadUrl;
        setUrl(nextUrl);
        cacheUrl(photoId, nextUrl);
        return nextUrl;
      } catch (err: any) {
        const message = err.message || 'Failed to load photo';
        setError(message);
        throw new Error(message);
      } finally {
        setIsLoading(false);
      }
    },
    [photoId, url, isLoading]
  );

  useEffect(() => {
    if (initialUrl) {
      setUrl(initialUrl);
      cacheUrl(photoId, initialUrl);
    }
  }, [initialUrl, photoId]);

  return { url, isLoading, error, fetchUrl };
};
