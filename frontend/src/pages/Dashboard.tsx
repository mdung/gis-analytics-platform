import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../lib/auth';
import MapView from '../components/map/MapView';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../lib/apiClient';
import { Layer, Feature } from '../types';

export default function Dashboard() {
  const { user, logout } = useAuthStore();
  const [selectedLayer, setSelectedLayer] = useState<string | null>(null);

  const { data: layers = [] } = useQuery<Layer[]>({
    queryKey: ['layers'],
    queryFn: async () => {
      const response = await apiClient.get('/api/layers?size=100');
      return response.data.content;
    },
  });

  const { data: features = [] } = useQuery<Feature[]>({
    queryKey: ['features', selectedLayer],
    queryFn: async () => {
      if (!selectedLayer) return [];
      const response = await apiClient.get(`/api/features?layerId=${selectedLayer}&size=1000`);
      return response.data.content;
    },
    enabled: !!selectedLayer,
  });

  const handleLogout = () => {
    authApi.logout();
    logout();
    window.location.href = '/login';
  };

  return (
    <div className="h-screen flex flex-col">
      <header className="bg-white shadow-sm border-b">
        <div className="px-4 py-3 flex justify-between items-center">
          <h1 className="text-xl font-bold">GIS Analytics Platform</h1>
          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-600">
              {user?.username} ({user?.role})
            </span>
            <nav className="flex gap-2">
              <Link to="/layers" className="px-3 py-1 text-sm bg-gray-100 rounded hover:bg-gray-200">
                Layers
              </Link>
              <Link to="/query" className="px-3 py-1 text-sm bg-gray-100 rounded hover:bg-gray-200">
                Query
              </Link>
              <Link to="/geofence" className="px-3 py-1 text-sm bg-gray-100 rounded hover:bg-gray-200">
                Geofence
              </Link>
              <Link to="/upload" className="px-3 py-1 text-sm bg-gray-100 rounded hover:bg-gray-200">
                Upload
              </Link>
            </nav>
            <button
              onClick={handleLogout}
              className="px-3 py-1 text-sm bg-red-600 text-white rounded hover:bg-red-700"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <div className="flex-1 flex">
        <aside className="w-64 bg-gray-50 border-r p-4">
          <h2 className="font-semibold mb-3">Layers</h2>
          <div className="space-y-2">
            {layers.map((layer) => (
              <label key={layer.id} className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={selectedLayer === layer.id}
                  onChange={(e) => setSelectedLayer(e.target.checked ? layer.id : null)}
                  className="rounded"
                />
                <span className="text-sm">{layer.name}</span>
              </label>
            ))}
          </div>
        </aside>

        <main className="flex-1">
          <MapView features={features} />
        </main>
      </div>
    </div>
  );
}

