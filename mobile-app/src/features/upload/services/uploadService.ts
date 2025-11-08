import * as FileSystem from 'expo-file-system';
import { uploadApi } from '../../../shared/api/endpoints';
import { InitiateUploadRequest, CompletedPart, PresignedUrl } from '../../../shared/types';

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
  async uploadFile(
    uri: string,
    filename: string,
    fileSize: number,
    mimeType: string,
    options: UploadOptions = {}
  ): Promise<UploadResult> {
    try {
      // Step 1: Initiate upload with backend
      const initRequest: InitiateUploadRequest = {
        filename,
        fileSizeBytes: fileSize,
        mimeType,
      };

      const { data: initResponse } = await uploadApi.initiateUpload(initRequest);
      const { photoId, uploadId, s3Key, presignedUrls } = initResponse;

      // Step 2: Upload parts directly to S3 in parallel
      const uploadedParts = await this.uploadParts(
        uri,
        fileSize,
        mimeType,
        presignedUrls,
        options
      );

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
    uri: string,
    fileSize: number,
    mimeType: string,
    presignedUrls: PresignedUrl[],
    options: UploadOptions
  ): Promise<CompletedPart[]> {
    const totalParts = presignedUrls.length;
    let completedParts = 0;

    // Upload all parts in parallel
    const uploadPromises = presignedUrls.map(async (presigned) => {
      // Calculate chunk offset and size
      const partIndex = presigned.partNumber - 1;
      const offset = partIndex * CHUNK_SIZE;
      const length = Math.min(CHUNK_SIZE, fileSize - offset);

      // Read chunk from file
      const chunkBase64 = await FileSystem.readAsStringAsync(uri, {
        encoding: FileSystem.EncodingType.Base64,
        position: offset,
        length: length,
      });

      // Convert base64 to blob
      const chunkBlob = this.base64ToBlob(chunkBase64, mimeType);

      // Upload chunk to S3 using fetch
      const response = await fetch(presigned.url, {
        method: 'PUT',
        body: chunkBlob,
        headers: {
          'Content-Type': mimeType,
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
   * Convert base64 string to Blob
   */
  private base64ToBlob(base64: string, mimeType: string): Blob {
    const byteCharacters = atob(base64);
    const byteArrays = [];

    for (let offset = 0; offset < byteCharacters.length; offset += 512) {
      const slice = byteCharacters.slice(offset, offset + 512);
      const byteNumbers = new Array(slice.length);

      for (let i = 0; i < slice.length; i++) {
        byteNumbers[i] = slice.charCodeAt(i);
      }

      const byteArray = new Uint8Array(byteNumbers);
      byteArrays.push(byteArray);
    }

    return new Blob(byteArrays, { type: mimeType });
  }

  /**
   * Validate file before upload
   */
  validateFile(fileSize: number, mimeType: string): { valid: boolean; error?: string } {
    const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    const ALLOWED_TYPES = [
      'image/jpeg',
      'image/jpg',
      'image/png',
      'image/gif',
      'image/webp',
    ];

    if (fileSize > MAX_FILE_SIZE) {
      return {
        valid: false,
        error: `File size exceeds 50MB limit (${(fileSize / 1024 / 1024).toFixed(2)}MB)`,
      };
    }

    if (!ALLOWED_TYPES.includes(mimeType)) {
      return {
        valid: false,
        error: `File type not supported. Allowed types: JPEG, PNG, GIF, WebP`,
      };
    }

    return { valid: true };
  }

  /**
   * Get file info from URI
   */
  async getFileInfo(uri: string): Promise<FileSystem.FileInfo> {
    return await FileSystem.getInfoAsync(uri);
  }
}

export const uploadService = new UploadService();
