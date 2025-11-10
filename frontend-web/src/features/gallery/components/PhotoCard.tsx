import { useEffect, useMemo, useRef, useState } from 'react';
import type { CSSProperties } from 'react';
import { PhotoResponse } from '@/shared/types';
import { usePhotoDownloadUrl } from '../hooks/usePhotoDownloadUrl';

interface PhotoCardProps {
  photo: PhotoResponse;
  onClick: () => void;
  selectable?: boolean;
  selected?: boolean;
  onSelect?: () => void;
  observerRoot?: Element | null;
  style?: CSSProperties;
}

export const PhotoCard = ({
  photo,
  onClick,
  selectable = false,
  selected = false,
  onSelect,
  observerRoot = null,
  style,
}: PhotoCardProps) => {
  const [isVisible, setIsVisible] = useState(false);
  const [isImageLoaded, setIsImageLoaded] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const hasThumbnail = Boolean(photo.thumbnailUrl || photo.thumbnailFallbackUrl);
  const shouldFetchFullImage = !hasThumbnail;
  const { url, fetchUrl } = usePhotoDownloadUrl(photo.id, photo.downloadUrl);

  useEffect(() => {
    setIsImageLoaded(false);
  }, [photo.id]);

  useEffect(() => {
    const element = containerRef.current;
    if (!element) return;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setIsVisible(true);
            observer.disconnect();
          }
        });
      },
      { root: observerRoot, rootMargin: '200px 0px' }
    );

    observer.observe(element);

    return () => {
      observer.disconnect();
    };
  }, [observerRoot]);

  useEffect(() => {
    if (isVisible && shouldFetchFullImage && !url) {
      fetchUrl();
    }
  }, [fetchUrl, isVisible, shouldFetchFullImage, url]);

  const variantEntries = useMemo(() => {
    if (!photo.thumbnailVariants) return [];
    return Object.entries(photo.thumbnailVariants)
      .filter(([descriptor]) => /^\d+w$/.test(descriptor))
      .sort((a, b) => parseInt(a[0], 10) - parseInt(b[0], 10));
  }, [photo.thumbnailVariants]);

  const toFallbackFormat = (src?: string) => {
    if (!src) return undefined;
    if (!/\.webp(\?.*)?$/i.test(src)) return undefined;
    return src.replace(/\.webp(\?.*)?$/i, '.jpg$1');
  };

  const fallbackVariantEntries = useMemo(() => {
    return variantEntries
      .map(([descriptor, src]) => {
        const fallbackSrc = toFallbackFormat(src);
        return fallbackSrc ? ([descriptor, fallbackSrc] as [string, string]) : null;
      })
      .filter((entry): entry is [string, string] => entry !== null);
  }, [variantEntries]);

  const webpSrcSet =
    variantEntries.length > 0
      ? variantEntries.map(([descriptor, src]) => `${src} ${descriptor}`).join(', ')
      : undefined;

  const fallbackSrcSet =
    fallbackVariantEntries.length > 0
      ? fallbackVariantEntries.map(([descriptor, src]) => `${src} ${descriptor}`).join(', ')
      : undefined;

  const responsiveSizes = '(max-width: 640px) 48vw, (max-width: 1024px) 30vw, 280px';

  const defaultVariant =
    variantEntries.find(([descriptor]) => descriptor === '640w') ??
    variantEntries[Math.floor(variantEntries.length / 2)] ??
    variantEntries[0];

  const defaultVariantUrl = defaultVariant?.[1];

  const placeholderSrc = photo.placeholderBase64 || photo.placeholderUrl || photo.placeholderFallbackUrl;
  const primaryThumbnailSrc = photo.thumbnailUrl || defaultVariantUrl;
  const fallbackThumbnailSrc = photo.thumbnailFallbackUrl || toFallbackFormat(defaultVariantUrl) || url;
  const imageSrc = fallbackThumbnailSrc || primaryThumbnailSrc || '/placeholder-image.png';
  const shouldApplyResponsive = Boolean(webpSrcSet || fallbackSrcSet);

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

  const handleClick = (event: React.MouseEvent<HTMLDivElement>) => {
    if (selectable) {
      event.stopPropagation();
      onSelect?.();
    } else {
      onClick();
    }
  };

  return (
    <div
      ref={containerRef}
      onClick={handleClick}
      data-selected={selected}
      style={style}
      className={`group relative bg-white rounded-lg shadow-sm overflow-hidden cursor-pointer transition-shadow ${
        selected ? 'border-2 border-primary-500 shadow-lg' : 'border border-gray-200 hover:shadow-md'
      }`}
    >
      {/* Selection checkbox */}
      {selectable && (
        <div className="absolute top-2 left-2 z-10">
          <button
            type="button"
            onClick={(event) => {
              event.stopPropagation();
              onSelect?.();
            }}
            className={`w-6 h-6 rounded-full border-2 flex items-center justify-center transition-colors ${
              selected ? 'bg-primary-600 border-primary-600' : 'bg-white border-gray-300 group-hover:border-gray-400'
            }`}
            aria-pressed={selected}
          >
            {selected && (
              <svg className="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                <path
                  fillRule="evenodd"
                  d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                  clipRule="evenodd"
                />
              </svg>
            )}
          </button>
        </div>
      )}

      {/* Image */}
      <div className="relative aspect-square bg-gray-100 overflow-hidden">
        {placeholderSrc && (
          <img
            src={placeholderSrc}
            alt=""
            aria-hidden="true"
            className={`absolute inset-0 w-full h-full object-cover blur-2xl scale-110 transition-opacity duration-500 ${
              isImageLoaded ? 'opacity-0' : 'opacity-100'
            }`}
          />
        )}

        {primaryThumbnailSrc || fallbackThumbnailSrc ? (
          <picture>
            {webpSrcSet && <source srcSet={webpSrcSet} sizes={responsiveSizes} type="image/webp" />}
            {(fallbackSrcSet || fallbackThumbnailSrc) && (
              <source
                srcSet={fallbackSrcSet ?? fallbackThumbnailSrc}
                sizes={responsiveSizes}
                type="image/jpeg"
              />
            )}
            <img
              src={imageSrc}
              alt={photo.originalFilename}
              className={`relative w-full h-full object-cover transition-opacity duration-500 ${
                isImageLoaded ? 'opacity-100' : 'opacity-0'
              }`}
              loading="lazy"
              decoding="async"
              srcSet={fallbackSrcSet}
              sizes={shouldApplyResponsive ? responsiveSizes : undefined}
              onLoad={() => setIsImageLoaded(true)}
              onError={(event) => {
                if (event.currentTarget.src !== '/placeholder-image.png') {
                  event.currentTarget.src = '/placeholder-image.png';
                }
              }}
            />
          </picture>
        ) : (
          <div className="w-full h-full animate-pulse bg-gray-200" />
        )}
      </div>

      {/* Overlay on hover */}
      {!selectable && (
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
      )}

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
