import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import apiClient from '../lib/apiClient';
import { Layer } from '../types';

interface ExportRequest {
  layerId: string;
  bbox?: {
    minLng: number;
    minLat: number;
    maxLng: number;
    maxLat: number;
  };
  format: 'geojson' | 'csv' | 'png';
}

export default function ExportPage() {
  const [exportRequest, setExportRequest] = useState<ExportRequest>({
    layerId: '',
    format: 'geojson',
  });

  const { data: layers = [] } = useQuery<Layer[]>({
    queryKey: ['layers'],
    queryFn: async () => {
      const response = await apiClient.get('/api/layers?size=100');
      return response.data.content;
    },
  });

  const exportMutation = useMutation({
    mutationFn: async (request: ExportRequest) => {
      const response = await apiClient.post(`/api/export/${request.format}`, request, {
        responseType: request.format === 'png' ? 'blob' : 'blob',
      });
      return response.data;
    },
    onSuccess: (data, variables) => {
      // Create download link
      const blob = new Blob([data], {
        type: variables.format === 'png' ? 'image/png' : variables.format === 'csv' ? 'text/csv' : 'application/json',
      });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `export.${variables.format === 'png' ? 'png' : variables.format === 'csv' ? 'csv' : 'geojson'}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    },
  });

  const handleExport = (e: React.FormEvent) => {
    e.preventDefault();
    if (!exportRequest.layerId) {
      alert('Please select a layer');
      return;
    }
    exportMutation.mutate(exportRequest);
  };

  return (
    <div className="p-4 max-w-4xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Export Data</h1>
        <Link to="/" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
          Back to Map
        </Link>
      </div>

      <div className="bg-white rounded-lg shadow p-6">
        <form onSubmit={handleExport} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Layer</label>
            <select
              value={exportRequest.layerId}
              onChange={(e) => setExportRequest({ ...exportRequest, layerId: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            >
              <option value="">-- Select Layer --</option>
              {layers.map((layer) => (
                <option key={layer.id} value={layer.id}>
                  {layer.name} ({layer.geomType})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Export Format</label>
            <select
              value={exportRequest.format}
              onChange={(e) => setExportRequest({ ...exportRequest, format: e.target.value as any })}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="geojson">GeoJSON</option>
              <option value="csv">CSV</option>
              <option value="png">PNG (Map Snapshot)</option>
            </select>
          </div>

          <div className="bg-gray-50 p-4 rounded">
            <label className="block text-sm font-medium text-gray-700 mb-2">Bounding Box (Optional)</label>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs text-gray-600 mb-1">Min Longitude</label>
                <input
                  type="number"
                  step="any"
                  value={exportRequest.bbox?.minLng || ''}
                  onChange={(e) =>
                    setExportRequest({
                      ...exportRequest,
                      bbox: { ...exportRequest.bbox, minLng: parseFloat(e.target.value) } as any,
                    })
                  }
                  className="w-full px-2 py-1 border rounded text-sm"
                  placeholder="-180"
                />
              </div>
              <div>
                <label className="block text-xs text-gray-600 mb-1">Min Latitude</label>
                <input
                  type="number"
                  step="any"
                  value={exportRequest.bbox?.minLat || ''}
                  onChange={(e) =>
                    setExportRequest({
                      ...exportRequest,
                      bbox: { ...exportRequest.bbox, minLat: parseFloat(e.target.value) } as any,
                    })
                  }
                  className="w-full px-2 py-1 border rounded text-sm"
                  placeholder="-90"
                />
              </div>
              <div>
                <label className="block text-xs text-gray-600 mb-1">Max Longitude</label>
                <input
                  type="number"
                  step="any"
                  value={exportRequest.bbox?.maxLng || ''}
                  onChange={(e) =>
                    setExportRequest({
                      ...exportRequest,
                      bbox: { ...exportRequest.bbox, maxLng: parseFloat(e.target.value) } as any,
                    })
                  }
                  className="w-full px-2 py-1 border rounded text-sm"
                  placeholder="180"
                />
              </div>
              <div>
                <label className="block text-xs text-gray-600 mb-1">Max Latitude</label>
                <input
                  type="number"
                  step="any"
                  value={exportRequest.bbox?.maxLat || ''}
                  onChange={(e) =>
                    setExportRequest({
                      ...exportRequest,
                      bbox: { ...exportRequest.bbox, maxLat: parseFloat(e.target.value) } as any,
                    })
                  }
                  className="w-full px-2 py-1 border rounded text-sm"
                  placeholder="90"
                />
              </div>
            </div>
            <p className="text-xs text-gray-500 mt-2">
              Leave empty to export all features. Bounding box limits export to a specific area.
            </p>
          </div>

          <button
            type="submit"
            disabled={exportMutation.isPending || !exportRequest.layerId}
            className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {exportMutation.isPending ? 'Exporting...' : 'Export'}
          </button>

          {exportMutation.isError && (
            <div className="p-3 bg-red-50 text-red-700 rounded">
              Export failed: {exportMutation.error instanceof Error ? exportMutation.error.message : 'Unknown error'}
            </div>
          )}
        </form>
      </div>
    </div>
  );
}

