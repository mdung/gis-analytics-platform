import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../lib/apiClient';
import { useAuthStore } from '../store/authStore';

interface AuditLog {
  id: string;
  userId?: string;
  username?: string;
  action: string;
  entityType?: string;
  entityId?: string;
  ipAddress?: string;
  requestMethod?: string;
  requestPath?: string;
  executionTimeMs?: number;
  createdAt: string;
}

export default function AuditLogsPage() {
  const { user } = useAuthStore();
  const [page, setPage] = useState(0);
  const [actionFilter, setActionFilter] = useState('');

  const { data, isLoading } = useQuery<{ content: AuditLog[]; totalElements: number }>({
    queryKey: ['audit-logs', page, actionFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ page: page.toString(), size: '20' });
      if (actionFilter) params.append('action', actionFilter);
      const response = await apiClient.get(`/api/audit?${params}`);
      return response.data;
    },
    enabled: user?.role === 'ADMIN',
  });

  if (user?.role !== 'ADMIN') {
    return (
      <div className="p-4">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-800">Access denied. Admin role required.</p>
          <Link to="/" className="mt-2 inline-block text-blue-600 hover:underline">
            Back to Dashboard
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 max-w-7xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Audit Logs</h1>
        <Link to="/" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
          Back to Dashboard
        </Link>
      </div>

      <div className="mb-4">
        <select
          value={actionFilter}
          onChange={(e) => {
            setActionFilter(e.target.value);
            setPage(0);
          }}
          className="px-3 py-2 border rounded-md"
        >
          <option value="">All Actions</option>
          <option value="CREATE">CREATE</option>
          <option value="UPDATE">UPDATE</option>
          <option value="DELETE">DELETE</option>
          <option value="VIEW">VIEW</option>
          <option value="QUERY">QUERY</option>
        </select>
      </div>

      {isLoading ? (
        <div className="text-center py-8">Loading...</div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Timestamp</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">User</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Action</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Entity</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Path</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Time (ms)</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">IP</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {data?.content.map((log) => (
                <tr key={log.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {new Date(log.createdAt).toLocaleString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">{log.username || 'N/A'}</td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className="px-2 py-1 text-xs rounded bg-blue-100 text-blue-800">{log.action}</span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {log.entityType} {log.entityId ? `(${log.entityId.substring(0, 8)}...)` : ''}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500 max-w-xs truncate">{log.requestPath}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">{log.executionTimeMs || '-'}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{log.ipAddress || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="px-6 py-4 flex justify-between items-center border-t">
            <div className="text-sm text-gray-600">
              Showing {data?.content.length || 0} of {data?.totalElements || 0} logs
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => setPage(Math.max(0, page - 1))}
                disabled={page === 0}
                className="px-3 py-1 border rounded disabled:opacity-50"
              >
                Previous
              </button>
              <button
                onClick={() => setPage(page + 1)}
                disabled={!data || data.content.length < 20}
                className="px-3 py-1 border rounded disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

