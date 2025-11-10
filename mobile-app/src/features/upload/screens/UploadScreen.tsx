import React, { useState, useCallback, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { PhotoPicker } from '../components/PhotoPicker';
import { UploadProgressList } from '../components/UploadProgressList';
import { UploadSummary } from '../components/UploadSummary';
import { useUploadManager } from '../hooks/useUploadManager';
import { useAuthStore } from '../../auth/store/authStore';
import { uploadApi } from '../../../shared/api/endpoints';
import { withRetry } from '../../../shared/utils/retryUtil';

export const UploadScreen: React.FC = () => {
  const [showSummary, setShowSummary] = useState(false);
  const [completedUploads, setCompletedUploads] = useState<any[]>([]);
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);
  const uploadsRef = useRef<any[]>([]);
  const computeMetricsRef = useRef<(() => any) | null>(null);
  const statsRef = useRef<any>(null);

  const handleAllComplete = useCallback(async () => {
    console.log('All uploads complete!');
    // Save completed uploads for summary
    setCompletedUploads([...uploadsRef.current]);
    setShowSummary(true);

    // Submit performance metrics when all uploads complete
    if (currentSessionId && statsRef.current?.completed > 0 && computeMetricsRef.current) {
      try {
        const metrics = computeMetricsRef.current();
        if (metrics) {
          // Use retry logic with exponential backoff
          await withRetry(
            () => uploadApi.updateSessionMetrics(currentSessionId, metrics),
            {
              maxRetries: 3,
              initialDelayMs: 1000,
              maxDelayMs: 8000,
              onRetry: (attempt, error) => {
                if (__DEV__) {
                  console.warn(
                    `[Performance] Metrics submission attempt ${attempt} failed:`,
                    error.message
                  );
                }
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
  }, [currentSessionId]);

  const { uploads, addFiles, cancelUpload, retryUpload, clearCompleted, stats, computePerformanceMetrics } = useUploadManager({
    concurrency: 10,
    sessionId: currentSessionId,
    onAllComplete: handleAllComplete,
  });

  // Keep refs updated with latest values
  computeMetricsRef.current = computePerformanceMetrics;
  statsRef.current = stats;

  // Keep ref updated with latest uploads
  uploadsRef.current = uploads;

  const { logout, email } = useAuthStore();

  const handlePhotosSelected = useCallback(async (photos: Array<{
    uri: string;
    filename: string;
    fileSize: number;
    mimeType: string;
  }>) => {
    // Create upload session if not already created
    if (!currentSessionId && photos.length > 0) {
      try {
        const response = await uploadApi.createSession({
          expectedPhotoCount: photos.length,
        });
        setCurrentSessionId(response.data.sessionId);
        console.log('[Session] Created upload session:', response.data.sessionId);
      } catch (error) {
        console.error('[Session] Failed to create session:', error);
        // Continue with upload even if session creation fails
      }
    }

    addFiles(photos);
  }, [currentSessionId, addFiles]);

  const handleCloseSummary = () => {
    setShowSummary(false);
    clearCompleted();
    setCompletedUploads([]);
    // Clear session for next batch
    setCurrentSessionId(null);
  };

  const handleRetryFailed = () => {
    setShowSummary(false);
    completedUploads
      .filter(u => u.status === 'failed')
      .forEach(u => retryUpload(u.id));
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <View>
          <Text style={styles.title}>Upload Photos</Text>
          {email && <Text style={styles.email}>{email}</Text>}
          {stats.total > 0 && (
            <Text style={styles.stats}>
              {stats.completed}/{stats.total} completed
              {stats.failed > 0 && ` • ${stats.failed} failed`}
            </Text>
          )}
        </View>
        <TouchableOpacity onPress={logout} style={styles.logoutButton}>
          <Text style={styles.logoutText}>Logout</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.pickerContainer}>
        <PhotoPicker onPhotosSelected={handlePhotosSelected} />
      </View>

      {uploads.length > 0 && (
        <View style={styles.progressContainer}>
          <Text style={styles.sectionTitle}>Upload Progress</Text>
          <UploadProgressList
            uploads={uploads}
            onCancel={cancelUpload}
            onRetry={retryUpload}
          />
        </View>
      )}

      {uploads.length === 0 && (
        <View style={styles.emptyState}>
          <Text style={styles.emptyText}>Select photos to start uploading</Text>
        </View>
      )}

      {showSummary && completedUploads.length > 0 && (
        <UploadSummary
          uploads={completedUploads}
          onClose={handleCloseSummary}
          onRetryFailed={handleRetryFailed}
        />
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9fafb',
  },
  header: {
    padding: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1f2937',
  },
  email: {
    fontSize: 12,
    color: '#6b7280',
    marginTop: 2,
  },
  stats: {
    fontSize: 14,
    color: '#6b7280',
    marginTop: 4,
  },
  logoutButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: '#ef4444',
    borderRadius: 6,
  },
  logoutText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  pickerContainer: {
    padding: 16,
  },
  progressContainer: {
    flex: 1,
    padding: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1f2937',
    marginBottom: 12,
  },
  emptyState: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyText: {
    fontSize: 16,
    color: '#9ca3af',
  },
});
