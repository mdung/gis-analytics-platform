import { useState } from 'react';
import { Link } from 'react-router-dom';

export default function GeoQueryBuilder() {
  return (
    <div className="p-4">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold">Spatial Query Builder</h1>
        <Link to="/" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
          Back to Map
        </Link>
      </div>
      <div className="bg-white p-4 rounded-lg shadow">
        <p className="text-gray-600">Spatial query builder interface (to be implemented)</p>
      </div>
    </div>
  );
}

