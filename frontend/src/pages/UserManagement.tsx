import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../lib/apiClient';
import { useAuthStore } from '../store/authStore';

interface User {
  id: string;
  username: string;
  email: string;
  role: 'ADMIN' | 'EDITOR' | 'VIEWER';
  lastLoginAt?: string;
  createdAt: string;
  updatedAt: string;
}

interface CreateUserRequest {
  username: string;
  password: string;
  email: string;
  role: 'ADMIN' | 'EDITOR' | 'VIEWER';
}

export default function UserManagement() {
  const { user: currentUser } = useAuthStore();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createForm, setCreateForm] = useState<CreateUserRequest>({
    username: '',
    password: '',
    email: '',
    role: 'VIEWER',
  });

  const { data, isLoading } = useQuery<{ content: User[]; totalElements: number }>({
    queryKey: ['users', page],
    queryFn: async () => {
      const response = await apiClient.get(`/api/users?page=${page}&size=20`);
      return response.data;
    },
    enabled: currentUser?.role === 'ADMIN',
  });

  const createMutation = useMutation({
    mutationFn: async (userData: CreateUserRequest) => {
      const response = await apiClient.post('/api/users', userData);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setShowCreateModal(false);
      setCreateForm({ username: '', password: '', email: '', role: 'VIEWER' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async (userId: string) => {
      await apiClient.delete(`/api/users/${userId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });

  const updateRoleMutation = useMutation({
    mutationFn: async ({ userId, role }: { userId: string; role: 'ADMIN' | 'EDITOR' | 'VIEWER' }) => {
      await apiClient.put(`/api/users/${userId}/role?role=${role}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });

  if (currentUser?.role !== 'ADMIN') {
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
        <h1 className="text-2xl font-bold">User Management</h1>
        <div className="flex gap-2">
          <button
            onClick={() => setShowCreateModal(true)}
            className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
          >
            Create User
          </button>
          <Link to="/" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
            Back to Dashboard
          </Link>
        </div>
      </div>

      {isLoading ? (
        <div className="text-center py-8">Loading...</div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Username</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Role</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Last Login</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {data?.content.map((user) => (
                <tr key={user.id}>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">{user.username}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{user.email}</td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <select
                      value={user.role}
                      onChange={(e) => {
                        if (user.id !== currentUser?.id) {
                          updateRoleMutation.mutate({
                            userId: user.id,
                            role: e.target.value as 'ADMIN' | 'EDITOR' | 'VIEWER',
                          });
                        }
                      }}
                      disabled={user.id === currentUser?.id}
                      className="text-sm border rounded px-2 py-1"
                    >
                      <option value="ADMIN">ADMIN</option>
                      <option value="EDITOR">EDITOR</option>
                      <option value="VIEWER">VIEWER</option>
                    </select>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : 'Never'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {user.id !== currentUser?.id && (
                      <button
                        onClick={() => {
                          if (confirm(`Delete user ${user.username}?`)) {
                            deleteMutation.mutate(user.id);
                          }
                        }}
                        className="text-red-600 hover:text-red-900"
                      >
                        Delete
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full">
            <h2 className="text-xl font-bold mb-4">Create New User</h2>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                createMutation.mutate(createForm);
              }}
              className="space-y-4"
            >
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Username</label>
                <input
                  type="text"
                  required
                  value={createForm.username}
                  onChange={(e) => setCreateForm({ ...createForm, username: e.target.value })}
                  className="w-full px-3 py-2 border rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
                <input
                  type="password"
                  required
                  value={createForm.password}
                  onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                  className="w-full px-3 py-2 border rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                <input
                  type="email"
                  required
                  value={createForm.email}
                  onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                  className="w-full px-3 py-2 border rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Role</label>
                <select
                  value={createForm.role}
                  onChange={(e) => setCreateForm({ ...createForm, role: e.target.value as any })}
                  className="w-full px-3 py-2 border rounded-md"
                >
                  <option value="VIEWER">VIEWER</option>
                  <option value="EDITOR">EDITOR</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
              </div>
              <div className="flex gap-2 justify-end">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 border rounded hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
                >
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

