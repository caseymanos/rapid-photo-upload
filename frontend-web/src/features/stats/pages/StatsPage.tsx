import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { statsApi } from '@/shared/api/endpoints';
import { UploadStatsResponse } from '@/shared/types';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/Table';
import { formatBytes, formatDuration, formatDate } from '@/lib/utils';

export const StatsPage = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState<UploadStatsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setLoading(true);
        const response = await statsApi.getUploadStats(20);
        setStats(response.data);
      } catch (err: any) {
        setError(err.message || 'Failed to load statistics');
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-primary-600 border-r-transparent"></div>
          <p className="mt-4 text-gray-600">Loading statistics...</p>
        </div>
      </div>
    );
  }

  if (error || !stats) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <p className="text-red-600">{error || 'Failed to load statistics'}</p>
          <button
            onClick={() => window.location.reload()}
            className="mt-4 px-4 py-2 bg-primary-600 text-white rounded-md hover:bg-primary-700"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  const { aggregate, recentSessions } = stats;

  // Show friendly empty state when no sessions exist
  if (aggregate.totalSessions === 0 || recentSessions.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {/* Header */}
          <div className="mb-8">
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-3xl font-bold text-gray-900">Upload Statistics</h1>
                <p className="mt-2 text-sm text-gray-600">
                  View your upload performance metrics and historical data
                </p>
              </div>
            </div>
          </div>

          {/* Empty State */}
          <div className="bg-white shadow rounded-lg overflow-hidden">
            <div className="px-6 py-16 text-center">
              <svg
                className="mx-auto h-24 w-24 text-gray-400"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                aria-hidden="true"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
                />
              </svg>
              <h3 className="mt-6 text-xl font-semibold text-gray-900">
                No Upload Sessions Yet
              </h3>
              <p className="mt-3 text-base text-gray-600 max-w-md mx-auto">
                Upload some photos to see your performance statistics, upload speeds, and
                historical data here.
              </p>
              <div className="mt-8">
                <button
                  onClick={() => navigate('/upload')}
                  className="inline-flex items-center px-6 py-3 border border-transparent text-base font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                >
                  <svg
                    className="-ml-1 mr-3 h-5 w-5"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
                    />
                  </svg>
                  Start Uploading
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Upload Statistics</h1>
              <p className="mt-2 text-sm text-gray-600">
                View your upload performance metrics and historical data
              </p>
            </div>
            <button
              onClick={() => navigate('/upload')}
              className="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
            >
              Upload Photos
            </button>
          </div>
        </div>

        {/* Aggregate Statistics Cards */}
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4 mb-8">
          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-500 truncate">Total Photos</p>
                  <p className="mt-1 text-3xl font-semibold text-gray-900">
                    {aggregate.totalPhotos.toLocaleString()}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-500 truncate">Total Data</p>
                  <p className="mt-1 text-3xl font-semibold text-gray-900">
                    {formatBytes(aggregate.totalBytesUploaded)}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-500 truncate">Avg Speed</p>
                  <p className="mt-1 text-3xl font-semibold text-gray-900">
                    {aggregate.avgThroughputMbps?.toFixed(2) || 'N/A'}{' '}
                    {aggregate.avgThroughputMbps && <span className="text-base">Mbps</span>}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-500 truncate">Success Rate</p>
                  <p className="mt-1 text-3xl font-semibold text-green-600">
                    {aggregate.successRate.toFixed(1)}%
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Additional Stats */}
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-3 mb-8">
          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <p className="text-sm font-medium text-gray-500">Total Sessions</p>
              <p className="mt-2 text-2xl font-semibold text-gray-900">
                {aggregate.totalSessions}
              </p>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <p className="text-sm font-medium text-gray-500">Avg Upload Time</p>
              <p className="mt-2 text-2xl font-semibold text-gray-900">
                {aggregate.avgUploadDurationMs
                  ? formatDuration(aggregate.avgUploadDurationMs)
                  : 'N/A'}
              </p>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <p className="text-sm font-medium text-gray-500">Completed Sessions</p>
              <p className="mt-2 text-2xl font-semibold text-gray-900">
                {aggregate.completedSessions}
                {aggregate.failedSessions > 0 && (
                  <span className="text-sm text-red-600 ml-2">
                    ({aggregate.failedSessions} failed)
                  </span>
                )}
              </p>
            </div>
          </div>
        </div>

        {/* Recent Sessions Table */}
        <div className="bg-white shadow rounded-lg overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-medium text-gray-900">Recent Upload Sessions</h2>
          </div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Started</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Photos</TableHead>
                <TableHead className="text-right">Data</TableHead>
                <TableHead className="text-right">Avg Speed</TableHead>
                <TableHead className="text-right">Avg Time</TableHead>
                <TableHead className="text-right">Success Rate</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {recentSessions.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} className="text-center text-gray-500 py-8">
                    No upload sessions yet. Start uploading to see your statistics!
                  </TableCell>
                </TableRow>
              ) : (
                recentSessions.map((session) => {
                  const successRate =
                    session.totalPhotos > 0
                      ? (session.completedPhotos / session.totalPhotos) * 100
                      : 0;
                  const statusColor =
                    session.status === 'COMPLETED'
                      ? 'text-green-600 bg-green-50'
                      : session.status === 'FAILED'
                      ? 'text-red-600 bg-red-50'
                      : 'text-yellow-600 bg-yellow-50';

                  return (
                    <TableRow key={session.sessionId}>
                      <TableCell className="font-medium">
                        {formatDate(session.startedAt)}
                      </TableCell>
                      <TableCell>
                        <span
                          className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${statusColor}`}
                        >
                          {session.status}
                        </span>
                      </TableCell>
                      <TableCell className="text-right">
                        {session.completedPhotos}/{session.totalPhotos}
                      </TableCell>
                      <TableCell className="text-right">
                        {session.totalBytesUploaded
                          ? formatBytes(session.totalBytesUploaded)
                          : 'N/A'}
                      </TableCell>
                      <TableCell className="text-right">
                        {session.avgThroughputMbps
                          ? `${session.avgThroughputMbps.toFixed(2)} Mbps`
                          : 'N/A'}
                      </TableCell>
                      <TableCell className="text-right">
                        {session.avgUploadDurationMs
                          ? formatDuration(session.avgUploadDurationMs)
                          : 'N/A'}
                      </TableCell>
                      <TableCell className="text-right">
                        <span
                          className={
                            successRate === 100
                              ? 'text-green-600 font-semibold'
                              : successRate > 50
                              ? 'text-yellow-600'
                              : 'text-red-600'
                          }
                        >
                          {successRate.toFixed(0)}%
                        </span>
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </div>
      </div>
    </div>
  );
};
