import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { usePhotos } from '../hooks/usePhotos';
import { PhotoCard } from '../components/PhotoCard';
import { PhotoModal } from '../components/PhotoModal';
import { PhotoResponse, UploadStatus } from '@/shared/types';
import { Masonry } from 'masonic';

export const GalleryPage = () => {
  const navigate = useNavigate();
  const {
    photos,
    totalPhotos,
    isLoading,
    isLoadingMore,
    error,
    hasMore,
    loadMore,
    updatePhotoMetadata,
    deletePhoto,
    deleteAllPhotos,
    deleteSelectedPhotos,
  } = usePhotos();

  const [selectedPhoto, setSelectedPhoto] = useState<PhotoResponse | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | UploadStatus>('ALL');
  const [sortDirection, setSortDirection] = useState<'desc' | 'asc'>('desc');
  const [selectMode, setSelectMode] = useState(false);
  const [selectedPhotoIds, setSelectedPhotoIds] = useState<Set<string>>(new Set());
  const [showDeleteAllConfirm, setShowDeleteAllConfirm] = useState(false);
  const [showDeleteSelectedConfirm, setShowDeleteSelectedConfirm] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const masonryContainerRef = useRef<HTMLDivElement | null>(null);
  const [observerRoot, setObserverRoot] = useState<Element | null>(null);

  const handleMasonryContainerRef = useCallback((node: HTMLDivElement | null) => {
    masonryContainerRef.current = node;
    setObserverRoot((prev) => (prev === node ? prev : node));
  }, []);

  const handlePhotoClick = (photo: PhotoResponse) => {
    if (selectMode) {
      togglePhotoSelection(photo.id);
      return;
    }
    setSelectedPhoto(photo);
  };

  const handleCloseModal = () => {
    setSelectedPhoto(null);
  };

  const handleUpdatePhoto = async (tags: string[], metadata: any) => {
    if (selectedPhoto) {
      await updatePhotoMetadata(selectedPhoto.id, tags, metadata);
    }
  };

  const handleDeletePhoto = async (photoId: string) => {
    await deletePhoto(photoId);
    setSelectedPhoto(null);
    setSelectedPhotoIds((prev) => {
      if (!prev.has(photoId)) return prev;
      const next = new Set(prev);
      next.delete(photoId);
      return next;
    });
  };

  const toggleSelectMode = () => {
    setSelectMode((prev) => !prev);
    setSelectedPhotoIds(new Set());
    setShowDeleteSelectedConfirm(false);
  };

  const togglePhotoSelection = (photoId: string) => {
    setSelectedPhotoIds((prev) => {
      const next = new Set(prev);
      if (next.has(photoId)) {
        next.delete(photoId);
      } else {
        next.add(photoId);
      }
      return next;
    });
  };

  const selectAll = () => {
    setSelectedPhotoIds(new Set(filteredAndSortedPhotos.map((photo) => photo.id)));
  };

  const deselectAll = () => {
    setSelectedPhotoIds(new Set());
  };

  const handleDeleteAll = async () => {
    setIsDeleting(true);
    try {
      await deleteAllPhotos();
      setShowDeleteAllConfirm(false);
      setSelectMode(false);
      setSelectedPhotoIds(new Set());
      setSelectedPhoto(null);
    } catch (err: any) {
      alert(err.message || 'Failed to delete photos');
    } finally {
      setIsDeleting(false);
    }
  };

  const handleDeleteSelected = async () => {
    if (selectedPhotoIds.size === 0) return;
    setIsDeleting(true);
    try {
      await deleteSelectedPhotos(Array.from(selectedPhotoIds));
      setSelectedPhotoIds(new Set());
      setSelectMode(false);
      setShowDeleteSelectedConfirm(false);
    } catch (err: any) {
      alert(err.message || 'Failed to delete selected photos');
    } finally {
      setIsDeleting(false);
    }
  };

  useEffect(() => {
    setSelectedPhotoIds((prev) => {
      const next = new Set<string>();
      photos.forEach((photo) => {
        if (prev.has(photo.id)) {
          next.add(photo.id);
        }
      });
      return next;
    });
  }, [photos]);

  const filteredAndSortedPhotos = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase();

    let result = photos;

    if (statusFilter !== 'ALL') {
      result = result.filter((photo) => photo.uploadStatus === statusFilter);
    }

    if (normalizedQuery) {
      result = result.filter((photo) => {
        const filenameMatch = photo.originalFilename.toLowerCase().includes(normalizedQuery);
        const tagMatch = photo.tags?.some((tag) => tag.toLowerCase().includes(normalizedQuery));
        return filenameMatch || tagMatch;
      });
    }

    const sorted = [...result].sort((a, b) => {
      const dateA = new Date(a.createdAt).getTime();
      const dateB = new Date(b.createdAt).getTime();
      return sortDirection === 'desc' ? dateB - dateA : dateA - dateB;
    });

    return sorted;
  }, [photos, statusFilter, searchQuery, sortDirection]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-4 border-primary-600 border-t-transparent"></div>
          <p className="mt-4 text-gray-600">Loading photos...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <svg
            className="mx-auto h-12 w-12 text-red-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          <p className="mt-4 text-red-600">{error}</p>
        </div>
      </div>
    );
  }

  const noPhotosAvailable = totalPhotos === 0;
  const noMatches = !noPhotosAvailable && filteredAndSortedPhotos.length === 0;

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex flex-wrap gap-4 items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Photo Gallery</h1>
              <p className="mt-2 text-sm text-gray-600">
                Showing {filteredAndSortedPhotos.length} of {totalPhotos}{' '}
                {totalPhotos === 1 ? 'photo' : 'photos'}
              </p>
              {photos.length < totalPhotos && (
                <p className="text-xs text-gray-500">Loaded {photos.length} so far</p>
              )}
            </div>
            <div className="flex flex-wrap gap-3 items-center">
              {!selectMode && (
                <>
                  <button
                    onClick={toggleSelectMode}
                    disabled={totalPhotos === 0}
                    className="inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                  >
                    Select
                  </button>
                  <button
                    onClick={() => setShowDeleteAllConfirm(true)}
                    disabled={totalPhotos === 0}
                    className="inline-flex items-center px-4 py-2 border border-red-300 text-sm font-medium rounded-md text-red-700 bg-white hover:bg-red-50 disabled:opacity-50"
                  >
                    Delete All
                  </button>
                </>
              )}
              {selectMode && (
                <>
                  <button
                    onClick={selectedPhotoIds.size === filteredAndSortedPhotos.length ? deselectAll : selectAll}
                    className="inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
                  >
                    {selectedPhotoIds.size === filteredAndSortedPhotos.length ? 'Deselect All' : 'Select All'}
                  </button>
                  {selectedPhotoIds.size > 0 && (
                    <button
                      onClick={() => setShowDeleteSelectedConfirm(true)}
                      className="inline-flex items-center px-4 py-2 border border-red-300 text-sm font-medium rounded-md text-red-700 bg-white hover:bg-red-50"
                    >
                      Delete Selected ({selectedPhotoIds.size})
                    </button>
                  )}
                  <button
                    onClick={toggleSelectMode}
                    className="inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                </>
              )}
              <button
                onClick={() => navigate('/')}
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700"
              >
                Upload Photos
              </button>
            </div>
          </div>
          <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Search</label>
              <input
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search by filename or tag"
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 sm:text-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as 'ALL' | UploadStatus)}
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 sm:text-sm"
              >
                <option value="ALL">All</option>
                <option value={UploadStatus.COMPLETED}>Completed</option>
                <option value={UploadStatus.UPLOADING}>Uploading</option>
                <option value={UploadStatus.INITIATED}>Initiated</option>
                <option value={UploadStatus.FAILED}>Failed</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Sort</label>
              <select
                value={sortDirection}
                onChange={(e) => setSortDirection(e.target.value as 'asc' | 'desc')}
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 sm:text-sm"
              >
                <option value="desc">Newest first</option>
                <option value="asc">Oldest first</option>
              </select>
            </div>
          </div>
        </div>

        {/* Photos Grid */}
        {noPhotosAvailable ? (
          <div className="text-center py-12">
            <svg
              className="mx-auto h-12 w-12 text-gray-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
            </svg>
            <h3 className="mt-2 text-sm font-medium text-gray-900">No photos</h3>
            <p className="mt-1 text-sm text-gray-500">Get started by uploading some photos.</p>
            <div className="mt-6">
              <button
                onClick={() => navigate('/')}
                className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700"
              >
                Upload Photos
              </button>
            </div>
          </div>
        ) : noMatches ? (
          <div className="text-center py-12 text-sm text-gray-600">
            No photos match your filters.
          </div>
        ) : (
          <div ref={handleMasonryContainerRef}>
            <Masonry<PhotoResponse>
              items={filteredAndSortedPhotos}
              itemKey={(item) => item.id}
              columnWidth={280}
              columnGutter={24}
              overscanBy={4}
              render={({ data, width }) => (
                <PhotoCard
                  photo={data}
                  onClick={() => handlePhotoClick(data)}
                  selectable={selectMode}
                  selected={selectedPhotoIds.has(data.id)}
                  onSelect={() => togglePhotoSelection(data.id)}
                  observerRoot={observerRoot}
                  style={{ width, maxWidth: '100%' }}
                />
              )}
            />
          </div>
        )}

        {/* Load more */}
        {hasMore && !noPhotosAvailable && (
          <div className="mt-8 flex justify-center">
            <button
              onClick={loadMore}
              disabled={isLoadingMore}
              className="px-6 py-2 text-sm font-medium rounded-md border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 disabled:opacity-50"
            >
              {isLoadingMore ? 'Loading more...' : 'Load More Photos'}
            </button>
          </div>
        )}
      </div>

      {/* Photo Modal */}
      {selectedPhoto && (
        <PhotoModal
          photo={selectedPhoto}
          onClose={handleCloseModal}
          onUpdate={handleUpdatePhoto}
          onDelete={handleDeletePhoto}
        />
      )}

      {/* Delete All Confirmation */}
      {showDeleteAllConfirm && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg max-w-md w-full p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">Delete All Photos?</h3>
            <p className="text-sm text-gray-600 mb-6">
              Are you sure you want to delete all {totalPhotos} {totalPhotos === 1 ? 'photo' : 'photos'}? This action cannot be undone.
            </p>
            <div className="flex items-center justify-end gap-3">
              <button
                onClick={() => setShowDeleteAllConfirm(false)}
                disabled={isDeleting}
                className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 font-medium"
              >
                Cancel
              </button>
              <button
                onClick={handleDeleteAll}
                disabled={isDeleting}
                className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 font-medium disabled:opacity-50"
              >
                {isDeleting ? 'Deleting...' : 'Delete All'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Selected Confirmation */}
      {showDeleteSelectedConfirm && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg max-w-md w-full p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">Delete Selected Photos?</h3>
            <p className="text-sm text-gray-600 mb-6">
              Are you sure you want to delete {selectedPhotoIds.size} selected {selectedPhotoIds.size === 1 ? 'photo' : 'photos'}?
            </p>
            <div className="flex items-center justify-end gap-3">
              <button
                onClick={() => setShowDeleteSelectedConfirm(false)}
                disabled={isDeleting}
                className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 font-medium"
              >
                Cancel
              </button>
              <button
                onClick={handleDeleteSelected}
                disabled={isDeleting}
                className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 font-medium disabled:opacity-50"
              >
                {isDeleting ? 'Deleting...' : `Delete ${selectedPhotoIds.size}`}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
