/**
 * Retry utility for handling transient network failures (React Native)
 * Uses exponential backoff with a maximum cap
 */

export interface RetryOptions {
  maxRetries?: number;
  initialDelayMs?: number;
  maxDelayMs?: number;
  backoffMultiplier?: number;
  onRetry?: (attempt: number, error: Error) => void;
}

const DEFAULT_OPTIONS: Required<RetryOptions> = {
  maxRetries: 3,
  initialDelayMs: 1000,
  maxDelayMs: 8000,
  backoffMultiplier: 2,
  onRetry: () => {},
};

/**
 * Executes an async function with exponential backoff retry logic
 * 
 * @param fn - The async function to execute
 * @param options - Retry configuration options
 * @returns Promise that resolves with the function result or rejects after all retries
 */
export async function withRetry<T>(
  fn: () => Promise<T>,
  options: RetryOptions = {}
): Promise<T> {
  const opts = { ...DEFAULT_OPTIONS, ...options };
  let lastError: Error;

  for (let attempt = 1; attempt <= opts.maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error));
      
      if (attempt < opts.maxRetries) {
        const delay = Math.min(
          opts.initialDelayMs * Math.pow(opts.backoffMultiplier, attempt - 1),
          opts.maxDelayMs
        );
        
        opts.onRetry(attempt, lastError);
        
        if (__DEV__) {
          console.warn(
            `[Retry] Attempt ${attempt}/${opts.maxRetries} failed. Retrying in ${delay}ms...`,
            lastError.message
          );
        }
        
        await sleep(delay);
      }
    }
  }

  if (__DEV__) {
    console.error(
      `[Retry] All ${opts.maxRetries} attempts failed.`,
      lastError!.message
    );
  }
  
  throw lastError!;
}

/**
 * Sleep for a specified duration
 */
function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}
