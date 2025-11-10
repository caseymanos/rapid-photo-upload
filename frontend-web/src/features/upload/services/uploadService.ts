import { uploadApi } from '@/shared/api/endpoints';
import { InitiateUploadRequest, CompletedPart, PresignedUrl } from '@/shared/types';

const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024; // 5MB fallback

export interface UploadOptions {
  onProgress?: (progress: number) => void;
  signal?: AbortSignal;
}

export interface UploadResult {
  photoId: string;
  uploadId: string;
  s3Key: string;
}

class UploadService {
  /**
   * Main upload function that coordinates the entire multipart upload process
   * Includes performance instrumentation
   */
  async uploadFile(file: File, options: UploadOptions = {}): Promise<UploadResult> {
    const overallStart = performance.now();
    const perfMetrics = {
      initiate: 0,
      upload: 0,
      complete: 0,
    };

    try {
      // Step 1: Initiate upload with backend
      const initStart = performance.now();
      const initRequest: InitiateUploadRequest = {
        originalFilename: file.name,
        fileSizeBytes: file.size,
        mimeType: file.type,
      };

      const { data: initResponse } = await uploadApi.initiateUpload(initRequest);
      const { photoId, multipartUploadId, s3Key, presignedUrls } = initResponse;
      const chunkSize = initResponse.chunkSizeBytes ?? DEFAULT_CHUNK_SIZE;
      const uploadMode = initResponse.singlePartUpload ? 'single-part' : 'multipart';
      perfMetrics.initiate = performance.now() - initStart;

      // Step 2: Upload parts directly to S3 in parallel
      const uploadStart = performance.now();
      const uploadedParts = await this.uploadParts(file, presignedUrls, chunkSize, options);
      perfMetrics.upload = performance.now() - uploadStart;

      // Step 3: Complete the upload with backend
      const completeStart = performance.now();
      await uploadApi.completeUpload(photoId, {
        parts: uploadedParts,
      });
      perfMetrics.complete = performance.now() - completeStart;

      const totalDuration = performance.now() - overallStart;
      const fileSizeMB = (file.size / 1024 / 1024).toFixed(2);
      const throughputMbps = ((file.size * 8) / (perfMetrics.upload / 1000) / 1000000).toFixed(2);

      console.log(
        `[Performance] Upload complete for ${file.name} (${fileSizeMB}MB, ${uploadMode}):\n` +
        `  Total: ${totalDuration.toFixed(0)}ms\n` +
        `  Initiate: ${perfMetrics.initiate.toFixed(0)}ms\n` +
        `  Upload: ${perfMetrics.upload.toFixed(0)}ms (${throughputMbps} Mbps)\n` +
        `  Complete: ${perfMetrics.complete.toFixed(0)}ms`
      );

      // Keep return shape backward-compatible for any callers
      const uploadId = multipartUploadId;
      return { photoId, uploadId, s3Key };
    } catch (error) {
      const totalDuration = performance.now() - overallStart;
      console.error(`Upload failed after ${totalDuration.toFixed(0)}ms:`, error);
      throw error;
    }
  }

  /**
   * Upload file parts in parallel to S3 using presigned URLs
   */
  private async uploadParts(
    file: File,
    presignedUrls: PresignedUrl[],
    chunkSize: number,
    options: UploadOptions
  ): Promise<CompletedPart[]> {
    const chunks = this.splitFile(file, chunkSize);
    const totalParts = presignedUrls.length;
    let completedParts = 0;

    // Upload all parts in parallel
    const uploadPromises = presignedUrls.map(async (presigned, index) => {
      const chunk = chunks[index];

      // Upload chunk to S3
      const response = await fetch(presigned.url, {
        method: 'PUT',
        body: chunk,
        headers: {
          'Content-Type': file.type,
        },
        signal: options.signal,
      });

      if (!response.ok) {
        throw new Error(
          `Part ${presigned.partNumber} upload failed: ${response.statusText}`
        );
      }

      // Extract ETag from response headers
      const etag = response.headers.get('ETag')?.replace(/"/g, '');
      if (!etag) {
        throw new Error(`Part ${presigned.partNumber} missing ETag`);
      }

      // Update progress
      completedParts++;
      if (options.onProgress) {
        const progress = (completedParts / totalParts) * 100;
        options.onProgress(Math.round(progress));
      }

      return {
        partNumber: presigned.partNumber,
        etag,
      };
    });

    // Wait for all parts to complete
    const results = await Promise.all(uploadPromises);

    // Sort by part number to ensure correct order
    return results.sort((a, b) => a.partNumber - b.partNumber);
  }

  /**
   * Split file into chunks for multipart upload
   */
  private splitFile(file: File, chunkSize: number): Blob[] {
    const chunks: Blob[] = [];
    let offset = 0;

    while (offset < file.size) {
      const chunk = file.slice(offset, offset + chunkSize);
      chunks.push(chunk);
      offset += chunkSize;
    }

    return chunks;
  }

  /**
   * Validate file before upload
   */
  validateFile(file: File): { valid: boolean; error?: string } {
    const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    const ALLOWED_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];

    if (file.size > MAX_FILE_SIZE) {
      return {
        valid: false,
        error: `File size exceeds 50MB limit (${(file.size / 1024 / 1024).toFixed(2)}MB)`,
      };
    }

    if (!ALLOWED_TYPES.includes(file.type)) {
      return {
        valid: false,
        error: `File type not supported. Allowed types: JPEG, PNG, GIF, WebP`,
      };
    }

    return { valid: true };
  }
}

export const uploadService = new UploadService();
