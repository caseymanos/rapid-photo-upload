import { useState, useCallback, useRef, useEffect } from 'react';
import { uploadService } from '../services/uploadService';
import { UploadItem } from '../../../shared/types';

interface UseUploadManagerOptions {
  concurrency?: number;
  onAllComplete?: () => void;
}

export const useUploadManager = (options: UseUploadManagerOptions = {}) => {
  const { concurrency = 10, onAllComplete } = options;

  const [uploads, setUploads] = useState<Map<string, UploadItem>>(new Map());
  const [activeUploads, setActiveUploads] = useState(0);
  const queueRef = useRef<string[]>([]);
  const abortControllersRef = useRef<Map<string, AbortController>>(new Map());
  const isProcessingRef = useRef(false);

  /**
   * Add files to upload queue (mobile version uses uri, filename, fileSize, mimeType)
   */
  const addFiles = useCallback((
    files: Array<{ uri: string; filename: string; fileSize: number; mimeType: string }>
  ) => {
    const newUploads = new Map(uploads);
    const newIds: string[] = [];

    files.forEach((file) => {
      // Validate file
      const validation = uploadService.validateFile(file.fileSize, file.mimeType);
      if (!validation.valid) {
        const id = Date.now().toString() + Math.random().toString(36).substring(2);
        newUploads.set(id, {
          id,
          uri: file.uri,
          filename: file.filename,
          fileSize: file.fileSize,
          mimeType: file.mimeType,
          status: 'failed',
          progress: 0,
          error: validation.error,
        });
        return;
      }

      const id = Date.now().toString() + Math.random().toString(36).substring(2);
      newUploads.set(id, {
        id,
        uri: file.uri,
        filename: file.filename,
        fileSize: file.fileSize,
        mimeType: file.mimeType,
        status: 'pending',
        progress: 0,
        startTime: Date.now(),
      });
      queueRef.current.push(id);
      newIds.push(id);
    });

    setUploads(newUploads);

    // Start processing queue
    setTimeout(() => processQueue(), 0);

    return newIds;
  }, [uploads]);

  /**
   * Process upload queue with concurrency control
   */
  const processQueue = useCallback(async () => {
    // Prevent multiple simultaneous processing loops
    if (isProcessingRef.current) return;
    isProcessingRef.current = true;

    while (queueRef.current.length > 0 && activeUploads < concurrency) {
      const uploadId = queueRef.current.shift();
      if (!uploadId) continue;

      const uploadItem = uploads.get(uploadId);
      if (!uploadItem) continue;

      setActiveUploads((prev) => prev + 1);

      // Create abort controller for this upload
      const abortController = new AbortController();
      abortControllersRef.current.set(uploadId, abortController);

      // Update status to uploading
      updateUploadStatus(uploadId, 'uploading');

      // Start upload (don't await here to allow concurrent uploads)
      uploadFile(uploadId, uploadItem, abortController.signal);
    }

    isProcessingRef.current = false;
  }, [uploads, activeUploads, concurrency]);

  /**
   * Upload individual file
   */
  const uploadFile = async (uploadId: string, uploadItem: UploadItem, signal: AbortSignal) => {
    try {
      const result = await uploadService.uploadFile(
        uploadItem.uri,
        uploadItem.filename,
        uploadItem.fileSize,
        uploadItem.mimeType,
        {
          onProgress: (progress) => {
            updateUploadProgress(uploadId, progress);
          },
          signal,
        }
      );

      // Mark as completed with photo info
      setUploads((prev) => {
        const newUploads = new Map(prev);
        const upload = newUploads.get(uploadId);
        if (upload) {
          newUploads.set(uploadId, {
            ...upload,
            status: 'completed',
            photoId: result.photoId,
            uploadId: result.uploadId,
            endTime: Date.now(),
          });
        }
        return newUploads;
      });
    } catch (error: any) {
      if (error.name === 'AbortError') {
        updateUploadStatus(uploadId, 'failed', 'Upload cancelled');
      } else {
        updateUploadStatus(uploadId, 'failed', error.message || 'Upload failed');
      }
    } finally {
      // Cleanup
      abortControllersRef.current.delete(uploadId);
      setActiveUploads((prev) => prev - 1);

      // Continue processing queue
      setTimeout(() => processQueue(), 0);
    }
  };

  /**
   * Update upload status
   */
  const updateUploadStatus = useCallback(
    (id: string, status: UploadItem['status'], error?: string) => {
      setUploads((prev) => {
        const newUploads = new Map(prev);
        const upload = newUploads.get(id);
        if (upload) {
          newUploads.set(id, { ...upload, status, error });
        }
        return newUploads;
      });
    },
    []
  );

  /**
   * Update upload progress
   */
  const updateUploadProgress = useCallback((id: string, progress: number) => {
    setUploads((prev) => {
      const newUploads = new Map(prev);
      const upload = newUploads.get(id);
      if (upload) {
        newUploads.set(id, { ...upload, progress });
      }
      return newUploads;
    });
  }, []);

  /**
   * Cancel specific upload
   */
  const cancelUpload = useCallback((id: string) => {
    const controller = abortControllersRef.current.get(id);
    if (controller) {
      controller.abort();
    }
  }, []);

  /**
   * Retry failed upload
   */
  const retryUpload = useCallback((id: string) => {
    const upload = uploads.get(id);
    if (!upload || upload.status !== 'failed') return;

    // Reset upload state
    setUploads((prev) => {
      const newUploads = new Map(prev);
      newUploads.set(id, {
        ...upload,
        status: 'pending',
        progress: 0,
        error: undefined,
        startTime: Date.now(),
      });
      return newUploads;
    });

    // Add back to queue
    queueRef.current.push(id);
    setTimeout(() => processQueue(), 0);
  }, [uploads, processQueue]);

  /**
   * Clear completed uploads
   */
  const clearCompleted = useCallback(() => {
    setUploads((prev) => {
      const newUploads = new Map(prev);
      Array.from(newUploads.entries()).forEach(([id, upload]) => {
        if (upload.status === 'completed') {
          newUploads.delete(id);
        }
      });
      return newUploads;
    });
  }, []);

  /**
   * Clear all uploads
   */
  const clearAll = useCallback(() => {
    // Cancel all active uploads
    abortControllersRef.current.forEach((controller) => controller.abort());
    abortControllersRef.current.clear();
    queueRef.current = [];
    setUploads(new Map());
    setActiveUploads(0);
  }, []);

  /**
   * Check if all uploads are complete
   */
  useEffect(() => {
    const uploadsList = Array.from(uploads.values());
    if (uploadsList.length === 0) return;

    const allComplete = uploadsList.every(
      (upload) => upload.status === 'completed' || upload.status === 'failed'
    );

    if (allComplete && activeUploads === 0 && onAllComplete) {
      onAllComplete();
    }
  }, [uploads, activeUploads, onAllComplete]);

  // Compute statistics
  const stats = Array.from(uploads.values()).reduce(
    (acc, upload) => {
      acc.total++;
      if (upload.status === 'completed') acc.completed++;
      if (upload.status === 'failed') acc.failed++;
      if (upload.status === 'uploading') acc.uploading++;
      if (upload.status === 'pending') acc.pending++;
      return acc;
    },
    { total: 0, completed: 0, failed: 0, uploading: 0, pending: 0 }
  );

  return {
    uploads: Array.from(uploads.values()),
    addFiles,
    cancelUpload,
    retryUpload,
    clearCompleted,
    clearAll,
    activeUploads,
    stats,
  };
};
