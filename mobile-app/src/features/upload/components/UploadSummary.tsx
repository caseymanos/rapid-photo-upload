import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { UploadItem } from '../../../shared/types';

interface UploadSummaryProps {
  uploads: UploadItem[];
  onClose: () => void;
  onRetryFailed?: () => void;
}

export const UploadSummary: React.FC<UploadSummaryProps> = ({
  uploads,
  onClose,
  onRetryFailed,
}) => {
  const stats = React.useMemo(() => {
    const completed = uploads.filter(u => u.status === 'completed');
    const failed = uploads.filter(u => u.status === 'failed');

    const totalSize = uploads.reduce((sum, u) => sum + u.fileSize, 0);
    const uploadedSize = completed.reduce((sum, u) => sum + u.fileSize, 0);

    const times = completed
      .filter(u => u.startTime && u.endTime)
      .map(u => u.endTime! - u.startTime!);

    const totalTime = times.length > 0 ? Math.max(...times) : 0;
    const avgTime = times.length > 0 ? times.reduce((a, b) => a + b, 0) / times.length : 0;

    const throughputMbps = totalTime > 0
      ? ((uploadedSize * 8) / (totalTime / 1000) / 1000000).toFixed(2)
      : '0';

    return {
      total: uploads.length,
      completed: completed.length,
      failed: failed.length,
      totalSize,
      uploadedSize,
      totalTime,
      avgTime,
      throughputMbps,
      successRate: ((completed.length / uploads.length) * 100).toFixed(1),
    };
  }, [uploads]);

  const formatSize = (bytes: number) => {
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }
    return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
  };

  const formatTime = (ms: number) => {
    if (ms < 1000) {
      return `${ms}ms`;
    }
    if (ms < 60000) {
      return `${(ms / 1000).toFixed(1)}s`;
    }
    return `${Math.floor(ms / 60000)}m ${Math.floor((ms % 60000) / 1000)}s`;
  };

  return (
    <View style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>Upload Complete!</Text>

        <View style={styles.statsGrid}>
          {/* Success Rate */}
          <View style={styles.statCard}>
            <Text style={styles.statValue}>{stats.successRate}%</Text>
            <Text style={styles.statLabel}>Success Rate</Text>
          </View>

          {/* Total Photos */}
          <View style={styles.statCard}>
            <Text style={styles.statValue}>{stats.completed}/{stats.total}</Text>
            <Text style={styles.statLabel}>Photos Uploaded</Text>
          </View>

          {/* Total Size */}
          <View style={styles.statCard}>
            <Text style={styles.statValue}>{formatSize(stats.uploadedSize)}</Text>
            <Text style={styles.statLabel}>Data Uploaded</Text>
          </View>

          {/* Total Time */}
          <View style={styles.statCard}>
            <Text style={styles.statValue}>{formatTime(stats.totalTime)}</Text>
            <Text style={styles.statLabel}>Total Time</Text>
          </View>

          {/* Average Time */}
          <View style={styles.statCard}>
            <Text style={styles.statValue}>{formatTime(stats.avgTime)}</Text>
            <Text style={styles.statLabel}>Avg per Photo</Text>
          </View>

          {/* Throughput */}
          <View style={styles.statCard}>
            <Text style={styles.statValue}>{stats.throughputMbps} Mbps</Text>
            <Text style={styles.statLabel}>Throughput</Text>
          </View>
        </View>

        {stats.failed > 0 && (
          <View style={styles.failedSection}>
            <Text style={styles.failedText}>
              ⚠️ {stats.failed} photo{stats.failed > 1 ? 's' : ''} failed to upload
            </Text>
            {onRetryFailed && (
              <TouchableOpacity onPress={onRetryFailed} style={styles.retryButton}>
                <Text style={styles.retryButtonText}>Retry Failed</Text>
              </TouchableOpacity>
            )}
          </View>
        )}

        {/* Details Section */}
        <View style={styles.detailsSection}>
          <Text style={styles.detailsTitle}>Details</Text>
          <ScrollView style={styles.detailsList} showsVerticalScrollIndicator={false}>
            {uploads.map((upload) => (
              <View key={upload.id} style={styles.detailItem}>
                <View style={styles.detailItemHeader}>
                  <Text style={styles.detailItemName} numberOfLines={1}>
                    {upload.status === 'completed' ? '✓' : '✗'} {upload.filename}
                  </Text>
                  <Text style={styles.detailItemSize}>
                    {formatSize(upload.fileSize)}
                  </Text>
                </View>
                {upload.status === 'completed' && upload.startTime && upload.endTime && (
                  <Text style={styles.detailItemTime}>
                    {formatTime(upload.endTime - upload.startTime)}
                  </Text>
                )}
                {upload.status === 'failed' && upload.error && (
                  <Text style={styles.detailItemError} numberOfLines={2}>
                    {upload.error}
                  </Text>
                )}
              </View>
            ))}
          </ScrollView>
        </View>

        <TouchableOpacity onPress={onClose} style={styles.closeButton}>
          <Text style={styles.closeButtonText}>Done</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  content: {
    backgroundColor: '#fff',
    borderRadius: 16,
    padding: 20,
    width: '100%',
    maxHeight: '80%',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1f2937',
    textAlign: 'center',
    marginBottom: 20,
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    marginBottom: 20,
  },
  statCard: {
    width: '48%',
    backgroundColor: '#f3f4f6',
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
    alignItems: 'center',
  },
  statValue: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#3b82f6',
    marginBottom: 4,
  },
  statLabel: {
    fontSize: 12,
    color: '#6b7280',
    textAlign: 'center',
  },
  failedSection: {
    backgroundColor: '#fef2f2',
    borderWidth: 1,
    borderColor: '#fecaca',
    borderRadius: 8,
    padding: 12,
    marginBottom: 16,
    alignItems: 'center',
  },
  failedText: {
    fontSize: 14,
    color: '#ef4444',
    fontWeight: '600',
    marginBottom: 8,
  },
  retryButton: {
    backgroundColor: '#ef4444',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 6,
  },
  retryButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  detailsSection: {
    marginBottom: 16,
    maxHeight: 200,
  },
  detailsTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 12,
  },
  detailsList: {
    maxHeight: 180,
  },
  detailItem: {
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  detailItemHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  detailItemName: {
    fontSize: 14,
    color: '#1f2937',
    flex: 1,
    marginRight: 8,
  },
  detailItemSize: {
    fontSize: 12,
    color: '#6b7280',
  },
  detailItemTime: {
    fontSize: 12,
    color: '#10b981',
    marginTop: 2,
  },
  detailItemError: {
    fontSize: 11,
    color: '#ef4444',
    marginTop: 2,
  },
  closeButton: {
    backgroundColor: '#3b82f6',
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
  },
  closeButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
