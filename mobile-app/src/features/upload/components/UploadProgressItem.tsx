import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { UploadItem } from '../../../shared/types';

interface Props {
  upload: UploadItem;
  onCancel?: () => void;
  onRetry?: () => void;
}

export const UploadProgressItem: React.FC<Props> = ({ upload, onCancel, onRetry }) => {
  const getStatusColor = () => {
    switch (upload.status) {
      case 'completed': return '#10b981';
      case 'failed': return '#ef4444';
      case 'uploading': return '#3b82f6';
      default: return '#6b7280';
    }
  };

  const getStatusIcon = () => {
    switch (upload.status) {
      case 'completed': return '✓';
      case 'failed': return '✗';
      case 'uploading': return '↑';
      default: return '⋯';
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.filename} numberOfLines={1}>{upload.filename}</Text>
        <View style={[styles.statusBadge, { backgroundColor: getStatusColor() }]}>
          <Text style={styles.statusIcon}>{getStatusIcon()}</Text>
        </View>
      </View>

      {upload.status === 'uploading' && (
        <View style={styles.progressContainer}>
          <View style={styles.progressBar}>
            <View style={[styles.progressFill, { width: `${upload.progress}%` }]} />
          </View>
          <Text style={styles.progressText}>{upload.progress}%</Text>
        </View>
      )}

      {upload.error && <Text style={styles.error}>{upload.error}</Text>}

      <View style={styles.actions}>
        {upload.status === 'uploading' && onCancel && (
          <TouchableOpacity onPress={onCancel}>
            <Text style={styles.actionText}>Cancel</Text>
          </TouchableOpacity>
        )}
        {upload.status === 'failed' && onRetry && (
          <TouchableOpacity onPress={onRetry}>
            <Text style={[styles.actionText, styles.retryText]}>Retry</Text>
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 8,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  filename: { flex: 1, fontSize: 14, fontWeight: '500', color: '#1f2937' },
  statusBadge: { width: 24, height: 24, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  statusIcon: { color: '#fff', fontSize: 14, fontWeight: 'bold' },
  progressContainer: { flexDirection: 'row', alignItems: 'center', marginTop: 8, gap: 8 },
  progressBar: { flex: 1, height: 6, backgroundColor: '#e5e7eb', borderRadius: 3, overflow: 'hidden' },
  progressFill: { height: '100%', backgroundColor: '#3b82f6' },
  progressText: { fontSize: 12, color: '#6b7280', width: 40, textAlign: 'right' },
  error: { fontSize: 12, color: '#ef4444', marginTop: 4 },
  actions: { flexDirection: 'row', gap: 12, marginTop: 8 },
  actionText: { fontSize: 12, color: '#6b7280', fontWeight: '500' },
  retryText: { color: '#3b82f6' },
});
