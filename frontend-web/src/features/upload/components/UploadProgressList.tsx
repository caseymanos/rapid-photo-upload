import { UploadItem } from '@/shared/types';
import { UploadProgressItem } from './UploadProgressItem';

interface UploadProgressListProps {
  uploads: UploadItem[];
  onCancel: (id: string) => void;
  onRetry: (id: string) => void;
  onClearCompleted: () => void;
  stats: {
    total: number;
    completed: number;
    failed: number;
    uploading: number;
    pending: number;
  };
}

export const UploadProgressList = ({
  uploads,
  onCancel,
  onRetry,
  onClearCompleted,
  stats,
}: UploadProgressListProps) => {
  if (uploads.length === 0) {
    return null;
  }

  return (
    <div className="bg-gray-50 rounded-lg p-6">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">Upload Progress</h3>
          <div className="flex items-center space-x-4 mt-1 text-sm text-gray-600">
            <span>
              {stats.completed} / {stats.total} completed
            </span>
            {stats.uploading > 0 && (
              <span className="text-primary-600">{stats.uploading} uploading</span>
            )}
            {stats.failed > 0 && <span className="text-red-600">{stats.failed} failed</span>}
          </div>
        </div>

        {stats.completed > 0 && (
          <button
            onClick={onClearCompleted}
            className="text-sm text-gray-600 hover:text-gray-900 font-medium"
          >
            Clear completed
          </button>
        )}
      </div>

      <div className="space-y-3 max-h-96 overflow-y-auto">
        {uploads.map((upload) => (
          <UploadProgressItem
            key={upload.id}
            upload={upload}
            onCancel={onCancel}
            onRetry={onRetry}
          />
        ))}
      </div>
    </div>
  );
};
