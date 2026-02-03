import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Suspense, lazy } from 'react';
import Sidebar from './components/Sidebar';
import Dashboard from './pages/Dashboard';
import Incidents from './pages/Incidents';
import IncidentDetail from './pages/IncidentDetail';
import LogExplorer from './pages/LogExplorer';
import Clusters from './pages/Clusters';
import './App.css';

// Lazy load ApiFlows to avoid blocking the app with dagre ESM issues
const ApiFlows = lazy(() => import('./pages/ApiFlows'));

function App() {
  return (
    <Router>
      <div className="app-layout">
        <Sidebar />
        <main className="main-content">
          <Suspense fallback={<div className="loading">Loading...</div>}>
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/incidents" element={<Incidents />} />
              <Route path="/incidents/:id" element={<IncidentDetail />} />
              <Route path="/logs" element={<LogExplorer />} />
              <Route path="/clusters" element={<Clusters />} />
              <Route path="/flows" element={<ApiFlows />} />
            </Routes>
          </Suspense>
        </main>
      </div>
    </Router>
  );
}

export default App;
