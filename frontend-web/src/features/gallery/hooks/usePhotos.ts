import { useState, useEffect } from 'react';
import { photoApi } from '@/shared/api/endpoints';
import { PhotoResponse } from '@/shared/types';

export const usePhotos = () => {
  const [photos, setPhotos] = useState<PhotoResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPhotos = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await photoApi.getPhotos();
      setPhotos(response.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load photos');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchPhotos();
  }, []);

  const updatePhotoMetadata = async (
    photoId: string,
    tags?: string[],
    metadata?: any
  ) => {
    try {
      await photoApi.updatePhotoMetadata(photoId, { tags, metadata });
      await fetchPhotos(); // Refresh photos
    } catch (err: any) {
      throw new Error(err.response?.data?.message || 'Failed to update photo');
    }
  };

  return {
    photos,
    isLoading,
    error,
    refetch: fetchPhotos,
    updatePhotoMetadata,
  };
};
