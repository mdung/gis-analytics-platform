import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import LoginPage from './pages/LoginPage';
import Dashboard from './pages/Dashboard';
import LayerManagement from './pages/LayerManagement';
import GeoQueryBuilder from './pages/GeoQueryBuilder';
import GeofencePage from './pages/GeofencePage';
import UploadPage from './pages/UploadPage';
import UserManagement from './pages/UserManagement';
import ExportPage from './pages/ExportPage';
import LiveTrackingPage from './pages/LiveTrackingPage';
import AuditLogsPage from './pages/AuditLogsPage';

function App() {
  const { isAuthenticated } = useAuthStore();

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={isAuthenticated ? <Dashboard /> : <Navigate to="/login" />}
      />
      <Route
        path="/layers"
        element={isAuthenticated ? <LayerManagement /> : <Navigate to="/login" />}
      />
      <Route
        path="/query"
        element={isAuthenticated ? <GeoQueryBuilder /> : <Navigate to="/login" />}
      />
      <Route
        path="/geofence"
        element={isAuthenticated ? <GeofencePage /> : <Navigate to="/login" />}
      />
      <Route
        path="/upload"
        element={isAuthenticated ? <UploadPage /> : <Navigate to="/login" />}
      />
      <Route
        path="/export"
        element={isAuthenticated ? <ExportPage /> : <Navigate to="/login" />}
      />
      <Route
        path="/tracking"
        element={isAuthenticated ? <LiveTrackingPage /> : <Navigate to="/login" />}
      />
      <Route
        path="/users"
        element={isAuthenticated ? <UserManagement /> : <Navigate to="/login" />}
      />
      <Route
        path="/audit"
        element={isAuthenticated ? <AuditLogsPage /> : <Navigate to="/login" />}
      />
    </Routes>
  );
}

export default App;

