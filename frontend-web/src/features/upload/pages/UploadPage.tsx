import { useUploadManager } from '../hooks/useUploadManager';
import { UploadZone } from '../components/UploadZone';
import { UploadProgressList } from '../components/UploadProgressList';
import { useNavigate } from 'react-router-dom';
import { useEffect, useState, useCallback } from 'react';
import { photoApi, uploadApi } from '@/shared/api/endpoints';
import { usePhotoCache } from '../../gallery/context/PhotoCacheContext';
import { withRetry } from '@/shared/utils/retryUtil';

export const UploadPage = () => {
  const navigate = useNavigate();
  const { setCache } = usePhotoCache();
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);
  
  const {
    uploads,
    addFiles,
    cancelUpload,
    retryUpload,
    clearCompleted,
    stats,
    computePerformanceMetrics,
  } = useUploadManager({
    concurrency: 10,
    sessionId: currentSessionId,
    onAllComplete: async () => {
      // Submit performance metrics when all uploads complete
      if (currentSessionId && stats.completed > 0) {
        try {
          const metrics = computePerformanceMetrics();
          if (metrics) {
            // Use retry logic with exponential backoff
            await withRetry(
              () => uploadApi.updateSessionMetrics(currentSessionId, metrics),
              {
                maxRetries: 3,
                initialDelayMs: 1000,
                maxDelayMs: 8000,
                onRetry: (attempt, error) => {
                  console.warn(
                    `[Performance] Metrics submission attempt ${attempt} failed:`,
                    error.message
                  );
                },
              }
            );
            console.log('[Performance] Metrics submitted successfully for session:', currentSessionId);
          }
        } catch (error) {
          console.error('[Performance] All retry attempts failed for metrics submission:', error);
          // Consider queuing for later retry or showing user notification
        }
      }
    },
  });

  // Preload gallery photos while uploading for faster navigation
  useEffect(() => {
    if (stats.uploading > 0 || stats.pending > 0) {
      // Start preloading gallery data in the background
      const preloadGallery = async () => {
        try {
          const startTime = performance.now();
          const response = await photoApi.getPhotos({ includeDownloadUrl: false });
          setCache({
            photos: response.data.items,
            total: response.data.totalElements,
            lastFetchedPage: response.data.page,
            hasNext: response.data.hasNext,
          }); // Store in cache for instant gallery load
          const duration = performance.now() - startTime;
          console.log(`[Performance] Gallery preloaded and cached in ${duration.toFixed(2)}ms`);
        } catch (error) {
          // Silent failure - preloading is an optimization
          console.debug('Gallery preload failed:', error);
        }
      };
      
      // Start preloading after a short delay to avoid interfering with uploads
      const preloadTimer = setTimeout(preloadGallery, 1000);
      return () => clearTimeout(preloadTimer);
    }
  }, [stats.uploading, stats.pending, setCache]);

  // Auto-clear session after 1 hour of inactivity
  useEffect(() => {
    if (!currentSessionId) return;
    
    // Reset timer whenever upload activity changes
    const timeoutId = setTimeout(() => {
      console.log('[Session] Session expired due to inactivity (1 hour)');
      setCurrentSessionId(null);
    }, 3600000); // 1 hour
    
    return () => clearTimeout(timeoutId);
  }, [currentSessionId, stats.uploading, stats.pending]);

  const handleFilesSelected = useCallback(async (files: File[]) => {
    // Create upload session if not already created
    if (!currentSessionId && files.length > 0) {
      try {
        const response = await uploadApi.createSession({
          expectedPhotoCount: files.length,
        });
        setCurrentSessionId(response.data.sessionId);
        console.log('[Session] Created upload session:', response.data.sessionId);
      } catch (error) {
        console.error('[Session] Failed to create session:', error);
        // Continue with upload even if session creation fails
      }
    }
    addFiles(files);
  }, [currentSessionId, addFiles]);

  const handleStartNewSession = useCallback(() => {
    setCurrentSessionId(null);
    console.log('[Session] Manually cleared session - ready for new batch');
  }, []);

  const isUploading = stats.uploading > 0 || stats.pending > 0;

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Upload Photos</h1>
              <p className="mt-2 text-sm text-gray-600">
                Upload up to 100 photos simultaneously with real-time progress tracking
              </p>
              {currentSessionId && (
                <div className="mt-3 flex items-center gap-3">
                  <div className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-800">
                    <svg
                      className="w-4 h-4 mr-2"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path
                        fillRule="evenodd"
                        d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z"
                        clipRule="evenodd"
                      />
                    </svg>
                    Session Active: {currentSessionId.substring(0, 8)}...
                  </div>
                  {!isUploading && (
                    <button
                      onClick={handleStartNewSession}
                      className="text-sm text-gray-600 hover:text-gray-900 underline"
                      title="Start a new upload session for the next batch"
                    >
                      Start New Session
                    </button>
                  )}
                </div>
              )}
            </div>
            <button
              onClick={() => navigate('/gallery')}
              className="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
            >
              View Gallery
            </button>
          </div>
        </div>

        {/* Upload Zone */}
        <div className="mb-8">
          <UploadZone onFilesSelected={handleFilesSelected} disabled={isUploading} />
        </div>

        {/* Progress Information */}
        {stats.total > 0 && (
          <div className="mb-6">
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-6">
                  <div>
                    <p className="text-sm font-medium text-gray-500">Total Files</p>
                    <p className="text-2xl font-bold text-gray-900">{stats.total}</p>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-gray-500">Completed</p>
                    <p className="text-2xl font-bold text-green-600">{stats.completed}</p>
                  </div>
                  {stats.uploading > 0 && (
                    <div>
                      <p className="text-sm font-medium text-gray-500">Uploading</p>
                      <p className="text-2xl font-bold text-primary-600">{stats.uploading}</p>
                    </div>
                  )}
                  {stats.failed > 0 && (
                    <div>
                      <p className="text-sm font-medium text-gray-500">Failed</p>
                      <p className="text-2xl font-bold text-red-600">{stats.failed}</p>
                    </div>
                  )}
                </div>

                {stats.completed === stats.total && stats.total > 0 && (
                  <button
                    onClick={() => navigate('/gallery')}
                    className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                  >
                    View in Gallery
                  </button>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Upload Progress List */}
        <UploadProgressList
          uploads={uploads}
          onCancel={cancelUpload}
          onRetry={retryUpload}
          onClearCompleted={clearCompleted}
          stats={stats}
        />
      </div>
    </div>
  );
};
