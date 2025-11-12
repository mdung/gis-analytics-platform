export const mapConfig = {
  defaultCenter: [106.6297, 10.8231] as [number, number], // Ho Chi Minh City
  defaultZoom: 12,
  minZoom: 3,
  maxZoom: 18,
  style: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png', // OSM tiles
  // Alternative: Mapbox style (requires API key)
  // style: 'mapbox://styles/mapbox/streets-v12',
};

export const getTileUrl = (z: number, x: number, y: number): string => {
  return mapConfig.style
    .replace('{z}', z.toString())
    .replace('{x}', x.toString())
    .replace('{y}', y.toString());
};

