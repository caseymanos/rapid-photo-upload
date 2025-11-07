import { PhotoResponse } from '@/shared/types';

interface PhotoCardProps {
  photo: PhotoResponse;
  onClick: () => void;
}

export const PhotoCard = ({ photo, onClick }: PhotoCardProps) => {
  const getPhotoUrl = (photo: PhotoResponse) => {
    // Use thumbnail if available, otherwise construct S3 URL
    if (photo.thumbnailUrl) {
      return photo.thumbnailUrl;
    }
    // Construct CloudFront/S3 URL
    return `https://${photo.s3Bucket}.s3.amazonaws.com/${photo.s3Key}`;
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  };

  return (
    <div
      onClick={onClick}
      className="group relative bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden cursor-pointer hover:shadow-md transition-shadow"
    >
      {/* Image */}
      <div className="aspect-square bg-gray-100">
        <img
          src={getPhotoUrl(photo)}
          alt={photo.originalFilename}
          className="w-full h-full object-cover"
          loading="lazy"
        />
      </div>

      {/* Overlay on hover */}
      <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-40 transition-opacity flex items-center justify-center">
        <svg
          className="w-12 h-12 text-white opacity-0 group-hover:opacity-100 transition-opacity"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
          />
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
          />
        </svg>
      </div>

      {/* Info */}
      <div className="p-3">
        <p className="text-sm font-medium text-gray-900 truncate">
          {photo.originalFilename}
        </p>
        <div className="flex items-center justify-between mt-1">
          <span className="text-xs text-gray-500">{formatFileSize(photo.fileSizeBytes)}</span>
          <span className="text-xs text-gray-500">{formatDate(photo.createdAt)}</span>
        </div>

        {/* Tags */}
        {photo.tags && photo.tags.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-1">
            {photo.tags.slice(0, 2).map((tag, index) => (
              <span
                key={index}
                className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-primary-100 text-primary-800"
              >
                {tag}
              </span>
            ))}
            {photo.tags.length > 2 && (
              <span className="text-xs text-gray-500">+{photo.tags.length - 2} more</span>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
