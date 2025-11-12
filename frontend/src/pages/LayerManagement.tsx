import { useQuery } from '@tanstack/react-query';
import apiClient from '../lib/apiClient';
import { Layer } from '../types';
import { Link } from 'react-router-dom';

export default function LayerManagement() {
  const { data: layers = [], isLoading } = useQuery<Layer[]>({
    queryKey: ['layers'],
    queryFn: async () => {
      const response = await apiClient.get('/api/layers?size=100');
      return response.data.content;
    },
  });

  if (isLoading) {
    return <div className="p-4">Loading...</div>;
  }

  return (
    <div className="p-4">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold">Layer Management</h1>
        <Link to="/" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
          Back to Map
        </Link>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {layers.map((layer) => (
          <div key={layer.id} className="bg-white p-4 rounded-lg shadow">
            <h3 className="font-semibold">{layer.name}</h3>
            <p className="text-sm text-gray-600">Code: {layer.code}</p>
            <p className="text-sm text-gray-600">Type: {layer.geomType}</p>
            <p className="text-sm text-gray-600">SRID: {layer.srid}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

