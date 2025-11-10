import { useState, useCallback, useRef, useEffect } from 'react';
import { uploadService } from '../services/uploadService';
import { UploadItem } from '@/shared/types';

interface UseUploadManagerOptions {
  concurrency?: number;
  onAllComplete?: () => void;
  sessionId?: string | null;
}

export const useUploadManager = (options: UseUploadManagerOptions = {}) => {
  const { concurrency = 10, onAllComplete, sessionId } = options;

  const [uploads, setUploads] = useState<Map<string, UploadItem>>(new Map());
  const [activeUploads, setActiveUploads] = useState(0);
  const queueRef = useRef<string[]>([]);
  const abortControllersRef = useRef<Map<string, AbortController>>(new Map());
  const isProcessingRef = useRef(false);

  /**
   * Add files to upload queue
   */
  const addFiles = useCallback((files: File[]) => {
    const newUploads = new Map(uploads);
    const newIds: string[] = [];

    files.forEach((file) => {
      // Validate file
      const validation = uploadService.validateFile(file);
      if (!validation.valid) {
        const id = crypto.randomUUID();
        newUploads.set(id, {
          id,
          file,
          status: 'failed',
          progress: 0,
          error: validation.error,
        });
        return;
      }

      const id = crypto.randomUUID();
      newUploads.set(id, {
        id,
        file,
        status: 'pending',
        progress: 0,
        startTime: Date.now(),
        fileSize: file.size,
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
   * Properly leverages concurrency by starting multiple uploads in parallel
   */
  const processQueue = useCallback(async () => {
    // Prevent multiple simultaneous processing loops
    if (isProcessingRef.current) return;
    isProcessingRef.current = true;

    try {
      // Start multiple uploads up to concurrency limit
      while (queueRef.current.length > 0 && activeUploads < concurrency) {
        const uploadId = queueRef.current.shift();
        if (!uploadId) continue;

        setUploads((currentUploads) => {
          const uploadItem = currentUploads.get(uploadId);
          if (!uploadItem) return currentUploads;

          setActiveUploads((prev) => prev + 1);

          // Create abort controller for this upload
          const abortController = new AbortController();
          abortControllersRef.current.set(uploadId, abortController);

          // Update status to uploading
          const newUploads = new Map(currentUploads);
          const upload = newUploads.get(uploadId);
          if (upload) {
            newUploads.set(uploadId, { ...upload, status: 'uploading' });
          }

          // Start upload (don't await here to allow concurrent uploads)
          uploadFile(uploadId, uploadItem.file, abortController.signal);

          return newUploads;
        });
      }
    } finally {
      isProcessingRef.current = false;
    }
  }, [activeUploads, concurrency]);

  /**
   * Upload individual file
   */
  const uploadFile = async (uploadId: string, file: File, signal: AbortSignal) => {
    try {
      await uploadService.uploadFile(file, sessionId || undefined, {
        onProgress: (progress) => {
          updateUploadProgress(uploadId, progress);
        },
        signal,
      });

      // Mark as completed
      updateUploadStatus(uploadId, 'completed');
      setUploads((prev) => {
        const newUploads = new Map(prev);
        const upload = newUploads.get(uploadId);
        if (upload) {
          const endTime = Date.now();
          const uploadDurationMs = upload.startTime ? endTime - upload.startTime : undefined;
          newUploads.set(uploadId, {
            ...upload,
            endTime,
            fileSize: file.size,
            uploadDurationMs
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
   * Clear all uploads
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
   * Clear all uploads (completed, failed, cancelled)
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

  /**
   * Compute performance metrics from completed uploads
   */
  const computePerformanceMetrics = useCallback(() => {
    const completedUploads = Array.from(uploads.values()).filter(
      (u) => u.status === 'completed' && u.uploadDurationMs && u.fileSize
    );

    if (completedUploads.length === 0) {
      return null;
    }

    const totalBytes = completedUploads.reduce((sum, u) => sum + (u.fileSize || 0), 0);
    const durations = completedUploads.map((u) => u.uploadDurationMs!);
    const avgDurationMs = Math.round(durations.reduce((a, b) => a + b, 0) / durations.length);
    const minDurationMs = Math.min(...durations);
    const maxDurationMs = Math.max(...durations);

    // Calculate average throughput in Mbps
    const throughputs = completedUploads.map((u) => {
      const durationSeconds = u.uploadDurationMs! / 1000;
      const megabits = (u.fileSize! * 8) / 1_000_000;
      return megabits / durationSeconds;
    });
    const avgThroughputMbps = throughputs.reduce((a, b) => a + b, 0) / throughputs.length;

    return {
      totalBytesUploaded: totalBytes,
      avgUploadDurationMs: avgDurationMs,
      avgThroughputMbps: Math.round(avgThroughputMbps * 100) / 100,
      minUploadDurationMs: minDurationMs,
      maxUploadDurationMs: maxDurationMs,
    };
  }, [uploads]);

  return {
    uploads: Array.from(uploads.values()),
    addFiles,
    cancelUpload,
    retryUpload,
    clearCompleted,
    clearAll,
    activeUploads,
    stats,
    computePerformanceMetrics,
  };
};
