import { uploadApi } from '@/shared/api/endpoints';
import { InitiateUploadRequest, CompletedPart, PresignedUrl } from '@/shared/types';

const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB chunks

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
   */
  async uploadFile(file: File, options: UploadOptions = {}): Promise<UploadResult> {
    try {
      // Step 1: Initiate upload with backend
      const initRequest: InitiateUploadRequest = {
        filename: file.name,
        fileSizeBytes: file.size,
        mimeType: file.type,
      };

      const { data: initResponse } = await uploadApi.initiateUpload(initRequest);
      const { photoId, uploadId, s3Key, presignedUrls } = initResponse;

      // Step 2: Upload parts directly to S3 in parallel
      const uploadedParts = await this.uploadParts(file, presignedUrls, options);

      // Step 3: Complete the upload with backend
      await uploadApi.completeUpload(photoId, {
        uploadId,
        parts: uploadedParts,
      });

      return { photoId, uploadId, s3Key };
    } catch (error) {
      console.error('Upload failed:', error);
      throw error;
    }
  }

  /**
   * Upload file parts in parallel to S3 using presigned URLs
   */
  private async uploadParts(
    file: File,
    presignedUrls: PresignedUrl[],
    options: UploadOptions
  ): Promise<CompletedPart[]> {
    const chunks = this.splitFile(file, CHUNK_SIZE);
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
