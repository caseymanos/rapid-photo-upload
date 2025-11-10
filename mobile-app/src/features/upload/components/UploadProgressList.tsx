import React from 'react';
import { ScrollView, StyleSheet } from 'react-native';
import { UploadItem } from '../../../shared/types';
import { UploadProgressItem } from './UploadProgressItem';

interface Props {
  uploads: UploadItem[];
  onCancel: (id: string) => void;
  onRetry: (id: string) => void;
}

export const UploadProgressList: React.FC<Props> = ({ uploads, onCancel, onRetry }) => {
  return (
    <ScrollView style={styles.container}>
      {uploads.map((upload) => (
        <UploadProgressItem
          key={upload.id}
          upload={upload}
          onCancel={() => onCancel(upload.id)}
          onRetry={() => onRetry(upload.id)}
        />
      ))}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
