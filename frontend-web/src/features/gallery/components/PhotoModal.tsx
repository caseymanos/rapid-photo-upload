import { useState, useEffect } from 'react';
import { PhotoResponse } from '@/shared/types';
import { usePhotoDownloadUrl } from '../hooks/usePhotoDownloadUrl';

interface PhotoModalProps {
  photo: PhotoResponse;
  onClose: () => void;
  onUpdate: (tags: string[], metadata: any) => Promise<void>;
  onDelete?: (photoId: string) => Promise<void>;
}

export const PhotoModal = ({ photo, onClose, onUpdate, onDelete }: PhotoModalProps) => {
  const [tags, setTags] = useState<string[]>(photo.tags || []);
  const [newTag, setNewTag] = useState('');
  const [description, setDescription] = useState(photo.metadata?.description || '');
  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const { url, fetchUrl, isLoading } = usePhotoDownloadUrl(photo.id, photo.downloadUrl);

  useEffect(() => {
    // Prevent body scroll when modal is open
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, []);

  useEffect(() => {
    if (!url) {
      fetchUrl();
    }
  }, [url, fetchUrl]);

  const handleAddTag = () => {
    const trimmedTag = newTag.trim();
    if (trimmedTag && !tags.includes(trimmedTag)) {
      setTags([...tags, trimmedTag]);
      setNewTag('');
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter((tag) => tag !== tagToRemove));
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      await onUpdate(tags, { ...photo.metadata, description });
      onClose();
    } catch (error) {
      alert('Failed to update photo');
    } finally {
      setIsSaving(false);
    }
  };

  const handleBackgroundClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  const handleDelete = async () => {
    if (!onDelete) return;
    setIsDeleting(true);
    try {
      await onDelete(photo.id);
      onClose();
    } catch (error) {
      alert('Failed to delete photo');
    } finally {
      setIsDeleting(false);
      setShowDeleteConfirm(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-75 flex items-center justify-center p-4"
      onClick={handleBackgroundClick}
    >
      <div className="bg-white rounded-lg max-w-6xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900">{photo.originalFilename}</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-500"
          >
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-4">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Image */}
            <div className="lg:col-span-2">
              {url ? (
                <img
                  src={url}
                  alt={photo.originalFilename}
                  className="w-full h-auto rounded-lg"
                  onError={(e) => {
                    e.currentTarget.src = '/placeholder-image.png';
                  }}
                />
              ) : (
                <div className="w-full aspect-video rounded-lg bg-gray-200 animate-pulse">
                  {isLoading && (
                    <div className="h-full w-full flex items-center justify-center text-gray-500 text-sm">
                      Loading photo...
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Metadata */}
            <div className="space-y-6">
              {/* Info */}
              <div>
                <h4 className="text-sm font-semibold text-gray-900 mb-2">Information</h4>
                <dl className="space-y-2 text-sm">
                  <div>
                    <dt className="text-gray-500">File Size</dt>
                    <dd className="text-gray-900">
                      {(photo.fileSizeBytes / 1024 / 1024).toFixed(2)} MB
                    </dd>
                  </div>
                  <div>
                    <dt className="text-gray-500">Type</dt>
                    <dd className="text-gray-900">{photo.mimeType}</dd>
                  </div>
                  <div>
                    <dt className="text-gray-500">Uploaded</dt>
                    <dd className="text-gray-900">
                      {new Date(photo.createdAt).toLocaleString()}
                    </dd>
                  </div>
                  {photo.metadata?.width && photo.metadata?.height && (
                    <div>
                      <dt className="text-gray-500">Dimensions</dt>
                      <dd className="text-gray-900">
                        {photo.metadata.width} × {photo.metadata.height}
                      </dd>
                    </div>
                  )}
                </dl>
              </div>

              {/* Description */}
              <div>
                <label className="block text-sm font-semibold text-gray-900 mb-2">
                  Description
                </label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  rows={3}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  placeholder="Add a description..."
                />
              </div>

              {/* Tags */}
              <div>
                <label className="block text-sm font-semibold text-gray-900 mb-2">Tags</label>
                <div className="flex gap-2 mb-2">
                  <input
                    type="text"
                    value={newTag}
                    onChange={(e) => setNewTag(e.target.value)}
                    onKeyPress={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleAddTag();
                      }
                    }}
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-sm"
                    placeholder="Add tag..."
                  />
                  <button
                    onClick={handleAddTag}
                    className="px-4 py-2 bg-primary-600 text-white rounded-md hover:bg-primary-700 text-sm font-medium"
                  >
                    Add
                  </button>
                </div>
                <div className="flex flex-wrap gap-2">
                  {tags.map((tag, index) => (
                    <span
                      key={index}
                      className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800"
                    >
                      {tag}
                      <button
                        onClick={() => handleRemoveTag(tag)}
                        className="hover:text-primary-900"
                      >
                        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                          <path
                            fillRule="evenodd"
                            d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                            clipRule="evenodd"
                          />
                        </svg>
                      </button>
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex flex-wrap items-center gap-3 p-4 border-t border-gray-200">
          {onDelete && (
            <div className="flex items-center gap-3 mr-auto">
              {!showDeleteConfirm ? (
                <button
                  onClick={() => setShowDeleteConfirm(true)}
                  className="px-4 py-2 border border-red-300 rounded-md text-red-700 hover:bg-red-50 font-medium flex items-center gap-2"
                >
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                    />
                  </svg>
                  Delete Photo
                </button>
              ) : (
                <div className="flex items-center gap-2 text-sm">
                  <span className="text-gray-700">Delete this photo?</span>
                  <button
                    onClick={handleDelete}
                    disabled={isDeleting}
                    className="px-3 py-1 bg-red-600 text-white rounded-md hover:bg-red-700 font-medium disabled:opacity-50"
                  >
                    {isDeleting ? 'Deleting...' : 'Confirm'}
                  </button>
                  <button
                    onClick={() => setShowDeleteConfirm(false)}
                    disabled={isDeleting}
                    className="px-3 py-1 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 font-medium"
                  >
                    Cancel
                  </button>
                </div>
              )}
            </div>
          )}
          <div className="ml-auto flex items-center gap-3">
            <button
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 font-medium"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              disabled={isSaving}
              className="px-4 py-2 bg-primary-600 text-white rounded-md hover:bg-primary-700 font-medium disabled:opacity-50"
            >
              {isSaving ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
