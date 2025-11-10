import { File, getInfoAsync } from 'expo-file-system';
import * as ImageManipulator from 'expo-image-manipulator';
import { uploadApi } from '../../../shared/api/endpoints';
import { InitiateUploadRequest, CompletedPart, PresignedUrl } from '../../../shared/types';

const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB chunks

export interface UploadOptions {
  onProgress?: (progress: number) => void;
  signal?: AbortSignal;
  sessionId?: string;
}

export interface UploadResult {
  photoId: string;
  uploadId: string;
  s3Key: string;
  uploadDurationMs: number;
}

class UploadService {
  /**
   * Main upload function that coordinates the entire multipart upload process
   * Includes performance instrumentation
   */
  async uploadFile(
    uri: string,
    filename: string,
    fileSize: number,
    mimeType: string,
    options: UploadOptions = {}
  ): Promise<UploadResult> {
    const prepared = await this.prepareFileForUpload(uri, filename, fileSize, mimeType);
    const uploadUri = prepared.uri;
    const uploadFilename = prepared.filename;
    const uploadSize = prepared.fileSize;
    const uploadMimeType = prepared.mimeType;

    const overallStart = Date.now();
    const perfMetrics = {
      initiate: 0,
      upload: 0,
      complete: 0,
    };

    try {
      // Step 1: Initiate upload with backend
      const initStart = Date.now();
      const initRequest: InitiateUploadRequest = {
        originalFilename: uploadFilename,
        fileSizeBytes: uploadSize,
        mimeType: uploadMimeType,
        uploadSessionId: options.sessionId,
      };

      const { data: initResponse } = await uploadApi.initiateUpload(initRequest);
      const { photoId, multipartUploadId, s3Key, presignedUrls } = initResponse;
      perfMetrics.initiate = Date.now() - initStart;

      // Step 2: Upload parts directly to S3 in parallel
      const uploadStart = Date.now();
      const uploadedParts = await this.uploadParts(
        uploadUri,
        uploadSize,
        uploadMimeType,
        presignedUrls,
        options
      );
      perfMetrics.upload = Date.now() - uploadStart;

      // Step 3: Complete the upload with backend
      const completeStart = Date.now();
      await uploadApi.completeUpload(photoId, {
        parts: uploadedParts,
      });
      perfMetrics.complete = Date.now() - completeStart;

      const totalDuration = Date.now() - overallStart;
      const fileSizeMB = (uploadSize / 1024 / 1024).toFixed(2);
      const throughputMbps = ((uploadSize * 8) / (perfMetrics.upload / 1000) / 1000000).toFixed(2);

      console.log(
        `[Performance] Upload complete for ${filename} (${fileSizeMB}MB):\n` +
        `  Total: ${totalDuration}ms\n` +
        `  Initiate: ${perfMetrics.initiate}ms\n` +
        `  Upload: ${perfMetrics.upload}ms (${throughputMbps} Mbps)\n` +
        `  Complete: ${perfMetrics.complete}ms`
      );

      const uploadId = multipartUploadId;
      return { photoId, uploadId, s3Key, uploadDurationMs: totalDuration };
    } catch (error) {
      const totalDuration = Date.now() - overallStart;
      console.error(`Upload failed after ${totalDuration}ms:`, error);
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
      try {
        // Calculate chunk offset and size
        const partIndex = presigned.partNumber - 1;
        const offset = partIndex * CHUNK_SIZE;
        const length = Math.min(CHUNK_SIZE, fileSize - offset);

        console.log(`[Upload] Part ${presigned.partNumber}: offset=${offset}, length=${length}`);

        // Read chunk from file using File API
        let bytes: Uint8Array;
        try {
          const file = new File(uri);
          if (!file.exists) {
            throw new Error(`File does not exist: ${uri}`);
          }
          const handle = file.open();
          handle.offset = offset;
          bytes = handle.readBytes(length);
          handle.close();
        } catch (fileError) {
          console.error(`[Upload] File API error for part ${presigned.partNumber}:`, fileError);
          throw new Error(`Failed to read file chunk: ${fileError}`);
        }

        if (!bytes || bytes.length === 0) {
          throw new Error(`Part ${presigned.partNumber}: Read 0 bytes from file`);
        }

        console.log(`[Upload] Part ${presigned.partNumber}: Uploading ${bytes.length} bytes`);

        // Upload chunk to S3 using fetch with raw binary data
        // IMPORTANT: Use Uint8Array directly, not string conversion which corrupts binary data
        const response = await fetch(presigned.url, {
          method: 'PUT',
          body: bytes,
          headers: {
            'Content-Type': mimeType,
            'Content-Length': length.toString(),
          },
          signal: options.signal,
        });

        if (!response.ok) {
          const responseText = await response.text().catch(() => 'Unable to read response');
          throw new Error(
            `Part ${presigned.partNumber} upload failed: ${response.status} ${response.statusText}\n${responseText}`
          );
        }

        // Extract ETag from response headers
        const etag = response.headers.get('ETag')?.replace(/"/g, '');
        if (!etag) {
          throw new Error(`Part ${presigned.partNumber} missing ETag in response`);
        }

        console.log(`[Upload] Part ${presigned.partNumber} success: etag=${etag}`);

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
      } catch (error) {
        console.error(`[Upload] Part ${presigned.partNumber} failed:`, error);
        throw error;
      }
    });

    // Wait for all parts to complete
    const results = await Promise.all(uploadPromises);

    // Sort by part number to ensure correct order
    return results.sort((a, b) => a.partNumber - b.partNumber);
  }

  /**
   * Convert Uint8Array bytes to base64 string
   */
  private bytesToBase64(bytes: Uint8Array): string {
    let binary = '';
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary);
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
      'image/heic',
      'image/heif',
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
   * Convert HEIC/HEIF images to JPEG before upload so the backend and gallery can render them.
   */
  private async prepareFileForUpload(
    uri: string,
    filename: string,
    fileSize: number,
    mimeType: string
  ): Promise<{ uri: string; filename: string; fileSize: number; mimeType: string }> {
    console.log(`[UploadService] Preparing file: ${filename}, type: ${mimeType}, size: ${fileSize}`);

    const lowerName = (filename || '').toLowerCase();
    const normalizedMime = mimeType?.toLowerCase() || '';
    const looksLikeHeic =
      normalizedMime.includes('heic') ||
      normalizedMime.includes('heif') ||
      lowerName.endsWith('.heic') ||
      lowerName.endsWith('.heif');

    if (!looksLikeHeic) {
      console.log(`[UploadService] Not HEIC, uploading as-is`);
      return { uri, filename, fileSize, mimeType };
    }

    try {
      console.log(`[UploadService] Detected HEIC, converting to JPEG...`);
      const converted = await ImageManipulator.manipulateAsync(
        uri,
        [],
        {
          compress: 1,
          format: ImageManipulator.SaveFormat.JPEG,
        }
      );

      console.log(`[UploadService] HEIC conversion successful, new URI: ${converted.uri}`);

      const info = await getInfoAsync(converted.uri);
      const derivedSize =
        'size' in info && typeof (info as { size?: number }).size === 'number'
          ? (info as { size?: number }).size!
          : fileSize;
      const normalizedName =
        filename?.replace(/\.heic$/i, '.jpg').replace(/\.heif$/i, '.jpg') ||
        `photo-${Date.now()}.jpg`;

      console.log(`[UploadService] Converted file: ${normalizedName}, new size: ${derivedSize}`);

      return {
        uri: converted.uri,
        filename: normalizedName,
        fileSize: derivedSize,
        mimeType: 'image/jpeg',
      };
    } catch (error) {
      console.error('[UploadService] Failed to convert HEIC, will upload original file:', error);
      return {
        uri,
        filename,
        fileSize,
        mimeType,
      };
    }
  }

  /**
   * Get file info from URI
   */
  async getFileInfo(uri: string): Promise<{ exists: boolean; size?: number; uri: string }> {
    const file = new File(uri);
    const exists = file.exists;
    const size = exists ? file.size : undefined;
    return { exists, size, uri };
  }
}

export const uploadService = new UploadService();
