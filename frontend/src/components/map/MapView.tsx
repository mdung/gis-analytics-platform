import { useEffect, useRef, useState } from 'react';
import maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { mapConfig } from '../../lib/mapConfig';
import { Layer, Feature } from '../../types';

interface MapViewProps {
  layers?: Layer[];
  features?: Feature[];
  onMapClick?: (lng: number, lat: number) => void;
  onFeatureClick?: (feature: Feature) => void;
}

export default function MapView({ layers = [], features = [], onMapClick, onFeatureClick }: MapViewProps) {
  const mapContainer = useRef<HTMLDivElement>(null);
  const map = useRef<maplibregl.Map | null>(null);
  const [mapLoaded, setMapLoaded] = useState(false);

  useEffect(() => {
    if (!mapContainer.current || map.current) return;

    map.current = new maplibregl.Map({
      container: mapContainer.current,
      style: {
        version: 8,
        sources: {
          'osm-tiles': {
            type: 'raster',
            tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
            tileSize: 256,
            attribution: '© OpenStreetMap contributors',
          },
        },
        layers: [
          {
            id: 'osm-layer',
            type: 'raster',
            source: 'osm-tiles',
          },
        ],
      },
      center: mapConfig.defaultCenter,
      zoom: mapConfig.defaultZoom,
    });

    map.current.on('load', () => {
      setMapLoaded(true);
    });

    map.current.on('click', (e) => {
      if (onMapClick) {
        onMapClick(e.lngLat.lng, e.lngLat.lat);
      }
    });

    return () => {
      if (map.current) {
        map.current.remove();
        map.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (!map.current || !mapLoaded) return;

    // Add features as sources and layers
    features.forEach((feature, index) => {
      const sourceId = `feature-${feature.id || index}`;
      const layerId = `feature-layer-${feature.id || index}`;

      if (map.current!.getSource(sourceId)) {
        (map.current!.getSource(sourceId) as maplibregl.GeoJSONSource).setData({
          type: 'FeatureCollection',
          features: [feature as any],
        });
      } else {
        map.current!.addSource(sourceId, {
          type: 'geojson',
          data: {
            type: 'FeatureCollection',
            features: [feature as any],
          },
        });

        const layerType = feature.geometry.type === 'Point' ? 'circle' :
                         feature.geometry.type === 'LineString' ? 'line' : 'fill';

        map.current!.addLayer({
          id: layerId,
          type: layerType,
          source: sourceId,
          paint: layerType === 'circle' ? {
            'circle-radius': 8,
            'circle-color': '#3b82f6',
          } : layerType === 'line' ? {
            'line-color': '#ef4444',
            'line-width': 3,
          } : {
            'fill-color': '#10b981',
            'fill-opacity': 0.3,
          },
        });

        if (onFeatureClick) {
          map.current!.on('click', layerId, (e) => {
            onFeatureClick(feature);
          });
        }
      }
    });
  }, [features, mapLoaded, onFeatureClick]);

  return (
    <div ref={mapContainer} className="w-full h-full" />
  );
}

