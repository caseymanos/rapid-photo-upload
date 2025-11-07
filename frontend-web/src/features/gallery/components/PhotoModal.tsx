import { useState, useEffect } from 'react';
import { PhotoResponse } from '@/shared/types';

interface PhotoModalProps {
  photo: PhotoResponse;
  onClose: () => void;
  onUpdate: (tags: string[], metadata: any) => Promise<void>;
}

export const PhotoModal = ({ photo, onClose, onUpdate }: PhotoModalProps) => {
  const [tags, setTags] = useState<string[]>(photo.tags || []);
  const [newTag, setNewTag] = useState('');
  const [description, setDescription] = useState(photo.metadata?.description || '');
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    // Prevent body scroll when modal is open
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, []);

  const getPhotoUrl = (photo: PhotoResponse) => {
    return `https://${photo.s3Bucket}.s3.amazonaws.com/${photo.s3Key}`;
  };

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
              <img
                src={getPhotoUrl(photo)}
                alt={photo.originalFilename}
                className="w-full h-auto rounded-lg"
              />
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
        <div className="flex items-center justify-end gap-3 p-4 border-t border-gray-200">
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
  );
};
