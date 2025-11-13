import { useEffect, useState, useRef } from 'react';
import { Link } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import MapView from '../components/map/MapView';

interface DevicePosition {
  deviceId: string;
  deviceCode: string;
  deviceName: string;
  longitude: number;
  latitude: number;
  timestamp: string;
}

interface GeofenceEvent {
  deviceId: string;
  deviceCode: string;
  deviceName: string;
  geofenceId: string;
  geofenceName: string;
  eventType: 'ENTER' | 'EXIT';
  longitude: number;
  latitude: number;
  timestamp: string;
}

export default function LiveTrackingPage() {
  const [positions, setPositions] = useState<Map<string, DevicePosition>>(new Map());
  const [events, setEvents] = useState<GeofenceEvent[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    const client = new Client({
      brokerURL: `ws://${window.location.hostname}:8081/ws/positions`,
      onConnect: () => {
        console.log('WebSocket connected');
        setIsConnected(true);

        // Subscribe to all device positions
        client.subscribe('/topic/devices', (message) => {
          const position: DevicePosition = JSON.parse(message.body);
          setPositions((prev) => {
            const newMap = new Map(prev);
            newMap.set(position.deviceId, position);
            return newMap;
          });
        });

        // Subscribe to geofence events
        client.subscribe('/topic/geofences', (message) => {
          const event: GeofenceEvent = JSON.parse(message.body);
          setEvents((prev) => [event, ...prev].slice(0, 50)); // Keep last 50 events
        });
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected');
        setIsConnected(false);
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, []);

  return (
    <div className="h-screen flex flex-col">
      <div className="bg-white shadow p-4 flex justify-between items-center">
        <div className="flex items-center gap-4">
          <h1 className="text-xl font-bold">Live Device Tracking</h1>
          <div className="flex items-center gap-2">
            <div className={`w-3 h-3 rounded-full ${isConnected ? 'bg-green-500' : 'bg-red-500'}`}></div>
            <span className="text-sm text-gray-600">{isConnected ? 'Connected' : 'Disconnected'}</span>
          </div>
        </div>
        <Link to="/" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
          Back to Dashboard
        </Link>
      </div>

      <div className="flex-1 flex">
        <div className="flex-1">
          <MapView
            features={Array.from(positions.values()).map((pos) => ({
              id: pos.deviceId,
              layerId: '',
              properties: { name: pos.deviceName, code: pos.deviceCode },
              geometry: {
                type: 'Point',
                coordinates: [pos.longitude, pos.latitude],
              },
            }))}
          />
        </div>

        <div className="w-80 bg-white shadow-lg overflow-y-auto">
          <div className="p-4 border-b">
            <h2 className="font-semibold">Active Devices ({positions.size})</h2>
          </div>
          <div className="divide-y">
            {Array.from(positions.values()).map((pos) => (
              <div key={pos.deviceId} className="p-3 hover:bg-gray-50">
                <div className="font-medium text-sm">{pos.deviceName}</div>
                <div className="text-xs text-gray-500">{pos.deviceCode}</div>
                <div className="text-xs text-gray-400 mt-1">
                  {new Date(pos.timestamp).toLocaleTimeString()}
                </div>
              </div>
            ))}
          </div>

          <div className="p-4 border-t mt-4">
            <h2 className="font-semibold mb-2">Recent Events ({events.length})</h2>
            <div className="space-y-2 max-h-96 overflow-y-auto">
              {events.map((event, idx) => (
                <div key={idx} className="p-2 bg-gray-50 rounded text-xs">
                  <div className="font-medium">
                    {event.deviceName} - {event.eventType}
                  </div>
                  <div className="text-gray-600">{event.geofenceName}</div>
                  <div className="text-gray-400">{new Date(event.timestamp).toLocaleTimeString()}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

