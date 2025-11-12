export interface User {
  id: string;
  username: string;
  email?: string;
  role: 'ADMIN' | 'EDITOR' | 'VIEWER';
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: User;
}

export interface Layer {
  id: string;
  code: string;
  name: string;
  geomType: 'POINT' | 'LINE' | 'POLYGON';
  srid: number;
  style: LayerStyle;
  metadata: LayerMetadata;
  createdAt?: string;
  updatedAt?: string;
}

export interface LayerStyle {
  color?: string;
  size?: number;
  icon?: string;
  iconColor?: string;
  lineWidth?: number;
  fillOpacity?: number;
  strokeWidth?: number;
}

export interface LayerMetadata {
  fields?: Array<{
    name: string;
    type: string;
  }>;
}

export interface Feature {
  id?: string;
  layerId: string;
  properties: Record<string, any>;
  geometry: GeoJSON.Geometry;
  createdAt?: string;
  updatedAt?: string;
  distanceMeters?: number;
}

export interface Geofence {
  id: string;
  name: string;
  description?: string;
  geometry: GeoJSON.Polygon;
  active: boolean;
}

export interface Device {
  id: string;
  code: string;
  name: string;
  lastPosition?: GeoJSON.Point;
  updatedAt?: string;
}

export interface SpatialQueryRequest {
  layerId: string;
  center?: [number, number];
  radiusMeters?: number;
  polygonGeoJson?: GeoJSON.Polygon;
  targetLayerId?: string;
  predicate?: 'INTERSECTS' | 'WITHIN' | 'CONTAINS';
  limit?: number;
  offset?: number;
}

