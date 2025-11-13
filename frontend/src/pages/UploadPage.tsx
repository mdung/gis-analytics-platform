import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import apiClient from '../lib/apiClient';
import { Layer } from '../types';

interface UploadResponse {
  id: string;
  fileName: string;
  fileSize: number;
  status: string;
  message?: string;
  stats?: {
    totalFeatures?: number;
    successCount?: number;
    failedCount?: number;
  };
  createdAt: string;
}

export default function UploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [selectedLayerId, setSelectedLayerId] = useState<string>('');
  const [latColumn, setLatColumn] = useState('');
  const [lngColumn, setLngColumn] = useState('');
  const [uploadId, setUploadId] = useState<string | null>(null);

  const { data: layers = [] } = useQuery<Layer[]>({
    queryKey: ['layers'],
    queryFn: async () => {
      const response = await apiClient.get('/api/layers?size=100');
      return response.data.content;
    },
  });

  const { data: upload, refetch: refetchUpload } = useQuery<UploadResponse>({
    queryKey: ['upload', uploadId],
    queryFn: async () => {
      const response = await apiClient.get(`/api/uploads/${uploadId}`);
      return response.data;
    },
    enabled: !!uploadId,
    refetchInterval: (query) => {
      const data = query.state.data;
      if (data?.status === 'PROCESSING' || data?.status === 'UPLOADED') {
        return 2000; // Poll every 2 seconds while processing
      }
      return false;
    },
  });

  const uploadMutation = useMutation({
    mutationFn: async (formData: FormData) => {
      const response = await apiClient.post('/api/uploads', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      return response.data;
    },
    onSuccess: (data) => {
      setUploadId(data.id);
      refetchUpload();
    },
  });

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
      setUploadId(null);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) {
      alert('Please select a file');
      return;
    }

    const formData = new FormData();
    formData.append('file', file);
    if (selectedLayerId) {
      formData.append('layerId', selectedLayerId);
    }
    if (latColumn) {
      formData.append('latColumn', latColumn);
    }
    if (lngColumn) {
      formData.append('lngColumn', lngColumn);
    }

    uploadMutation.mutate(formData);
  };

  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'PROCESSED':
        return 'text-green-600 bg-green-50';
      case 'PROCESSING':
        return 'text-blue-600 bg-blue-50';
      case 'FAILED':
        return 'text-red-600 bg-red-50';
      default:
        return 'text-gray-600 bg-gray-50';
    }
  };

  const isCSV = file?.name.toLowerCase().endsWith('.csv');

  return (
    <div className="p-4 max-w-4xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Upload Data</h1>
        <Link to="/" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
          Back to Map
        </Link>
      </div>

      <div className="bg-white rounded-lg shadow p-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Select File
            </label>
            <input
              type="file"
              accept=".geojson,.json,.csv,.zip,.shp"
              onChange={handleFileChange}
              className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
              required
            />
            <p className="mt-1 text-xs text-gray-500">
              Supported formats: GeoJSON (.geojson, .json), CSV (.csv), Shapefile ZIP (.zip)
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Target Layer (Optional - will auto-create if not specified)
            </label>
            <select
              value={selectedLayerId}
              onChange={(e) => setSelectedLayerId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">-- Create New Layer --</option>
              {layers.map((layer) => (
                <option key={layer.id} value={layer.id}>
                  {layer.name} ({layer.geomType})
                </option>
              ))}
            </select>
          </div>

          {isCSV && (
            <>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Latitude Column Name
                </label>
                <input
                  type="text"
                  value={latColumn}
                  onChange={(e) => setLatColumn(e.target.value)}
                  placeholder="e.g., lat, latitude, y"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required={isCSV}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Longitude Column Name
                </label>
                <input
                  type="text"
                  value={lngColumn}
                  onChange={(e) => setLngColumn(e.target.value)}
                  placeholder="e.g., lng, lon, longitude, x"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required={isCSV}
                />
              </div>
            </>
          )}

          <button
            type="submit"
            disabled={uploadMutation.isPending || !file}
            className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {uploadMutation.isPending ? 'Uploading...' : 'Upload & Process'}
          </button>
        </form>

        {uploadMutation.isError && (
          <div className="mt-4 p-3 bg-red-50 text-red-700 rounded">
            Upload failed: {uploadMutation.error instanceof Error ? uploadMutation.error.message : 'Unknown error'}
          </div>
        )}

        {upload && (
          <div className="mt-6 p-4 border rounded-lg">
            <div className="flex items-center justify-between mb-2">
              <h3 className="font-semibold">Upload Status</h3>
              <span className={`px-3 py-1 rounded text-sm font-medium ${getStatusColor(upload.status)}`}>
                {upload.status}
              </span>
            </div>

            <div className="space-y-2 text-sm">
              <div>
                <span className="font-medium">File:</span> {upload.fileName}
              </div>
              <div>
                <span className="font-medium">Size:</span> {(upload.fileSize / 1024).toFixed(2)} KB
              </div>
              {upload.message && (
                <div>
                  <span className="font-medium">Message:</span> {upload.message}
                </div>
              )}
              {upload.stats && (
                <div className="mt-3 p-2 bg-gray-50 rounded">
                  <div className="font-medium mb-1">Processing Stats:</div>
                  <div className="grid grid-cols-3 gap-2 text-xs">
                    <div>
                      <span className="text-gray-600">Total:</span> {upload.stats.totalFeatures || 0}
                    </div>
                    <div>
                      <span className="text-green-600">Success:</span> {upload.stats.successCount || 0}
                    </div>
                    <div>
                      <span className="text-red-600">Failed:</span> {upload.stats.failedCount || 0}
                    </div>
                  </div>
                </div>
              )}
              <div className="text-xs text-gray-500 mt-2">
                Uploaded: {new Date(upload.createdAt).toLocaleString()}
              </div>
            </div>

            {upload.status === 'PROCESSING' && (
              <div className="mt-3">
                <div className="animate-pulse bg-blue-100 h-2 rounded">
                  <div className="bg-blue-500 h-2 rounded" style={{ width: '60%' }}></div>
                </div>
                <p className="text-xs text-gray-600 mt-1">Processing in background...</p>
              </div>
            )}
          </div>
        )}
      </div>

      <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h3 className="font-semibold text-blue-900 mb-2">Upload Instructions</h3>
        <ul className="text-sm text-blue-800 space-y-1 list-disc list-inside">
          <li>GeoJSON files: Must contain FeatureCollection or Feature objects</li>
          <li>CSV files: Must have latitude and longitude columns (specify column names above)</li>
          <li>Shapefile: Upload as ZIP containing .shp, .dbf, and optionally .prj files</li>
          <li>All geometries will be transformed to EPSG:4326 (WGS84)</li>
          <li>If no layer is selected, a new layer will be created automatically</li>
        </ul>
      </div>
    </div>
  );
}
