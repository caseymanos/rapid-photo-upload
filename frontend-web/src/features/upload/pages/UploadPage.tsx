import { useUploadManager } from '../hooks/useUploadManager';
import { UploadZone } from '../components/UploadZone';
import { UploadProgressList } from '../components/UploadProgressList';
import { useNavigate } from 'react-router-dom';

export const UploadPage = () => {
  const navigate = useNavigate();
  const {
    uploads,
    addFiles,
    cancelUpload,
    retryUpload,
    clearCompleted,
    stats,
  } = useUploadManager({
    concurrency: 10,
    onAllComplete: () => {
      // Optionally navigate to gallery when all uploads complete
      // navigate('/gallery');
    },
  });

  const handleFilesSelected = (files: File[]) => {
    addFiles(files);
  };

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
