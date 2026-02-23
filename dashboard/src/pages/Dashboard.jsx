import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, AreaChart, Area } from 'recharts';
import { fetchIncidentStats, fetchIncidents, fetchTimeline, fetchFlowStats } from '../services/api';

function Dashboard() {
    const [stats, setStats] = useState(null);
    const [recentIncidents, setRecentIncidents] = useState([]);
    const [timeline, setTimeline] = useState([]);
    const [flowStats, setFlowStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        async function loadData() {
            try {
                setLoading(true);
                const [statsData, incidentsData, timelineData] = await Promise.all([
                    fetchIncidentStats(),
                    fetchIncidents(),
                    fetchTimeline(60)
                ]);
                setStats(statsData);
                setRecentIncidents(incidentsData.slice(0, 5));
                setTimeline(timelineData);

                // Load flow stats (non-blocking — Zipkin may not be running)
                try {
                    const flowData = await fetchFlowStats();
                    setFlowStats(flowData);
                } catch (_) {
                    // Zipkin not available
                }
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        }
        loadData();
    }, []);

    if (loading) {
        return (
            <div className="loading">
                <div className="spinner"></div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="empty-state">
                <h3>Unable to load data</h3>
                <p>{error}</p>
                <p style={{ marginTop: '16px', color: '#94a3b8' }}>
                    Make sure the backend is running on <code>localhost:8080</code>
                </p>
            </div>
        );
    }

    return (
        <div>
            <header className="page-header">
                <h1>Dashboard</h1>
                <p>Real-time monitoring and anomaly detection overview</p>
            </header>

            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-label">Total Incidents</div>
                    <div className="stat-value">{stats?.total || 0}</div>
                </div>
                <div className="stat-card open">
                    <div className="stat-label">Open Incidents</div>
                    <div className="stat-value">{stats?.open || 0}</div>
                </div>
                <div className="stat-card resolved">
                    <div className="stat-label">Resolved</div>
                    <div className="stat-value">{stats?.resolved || 0}</div>
                </div>
                <div className="stat-card">
                    <div className="stat-label">Error Bursts</div>
                    <div className="stat-value">{stats?.byType?.ERROR_BURST || 0}</div>
                </div>
            </div>

            {/* Data Pipelines Section */}
            <div className="pipelines-section">
                <h3 style={{ marginBottom: '16px', fontSize: '1.125rem' }}>Data Pipelines</h3>
                <div className="pipeline-cards">
                    <div className="pipeline-card kafka">
                        <div className="pipeline-icon">📨</div>
                        <div className="pipeline-info">
                            <div className="pipeline-name">Apache Kafka</div>
                            <div className="pipeline-desc">Log Ingestion Pipeline</div>
                            <div className="pipeline-detail">Topic: <code>app-logs</code> → Elasticsearch</div>
                        </div>
                        <div className="pipeline-stats">
                            <span className="pipeline-badge active">● Active</span>
                            <span className="pipeline-count">{stats?.total || 0} incidents detected</span>
                        </div>
                    </div>
                    <div className="pipeline-card zipkin">
                        <div className="pipeline-icon">🔀</div>
                        <div className="pipeline-info">
                            <div className="pipeline-name">Zipkin Tracing</div>
                            <div className="pipeline-desc">Distributed Trace Pipeline</div>
                            <div className="pipeline-detail">Micrometer → Zipkin → Flow Graphs</div>
                        </div>
                        <div className="pipeline-stats">
                            <span className={`pipeline-badge ${flowStats ? 'active' : 'inactive'}`}>
                                ● {flowStats ? 'Active' : 'Unavailable'}
                            </span>
                            {flowStats && (
                                <span className="pipeline-count">
                                    {flowStats.totalFlows} traces · {flowStats.serviceCount} services
                                </span>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            <div className="chart-container">
                <h3>Log Activity Timeline (Last 60 minutes)</h3>
                <ResponsiveContainer width="100%" height={300}>
                    <AreaChart data={timeline}>
                        <defs>
                            <linearGradient id="colorError" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#ef4444" stopOpacity={0.3} />
                                <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
                            </linearGradient>
                            <linearGradient id="colorWarn" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#f59e0b" stopOpacity={0.3} />
                                <stop offset="95%" stopColor="#f59e0b" stopOpacity={0} />
                            </linearGradient>
                            <linearGradient id="colorInfo" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3} />
                                <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                            </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" stroke="#252542" />
                        <XAxis
                            dataKey="time"
                            stroke="#64748b"
                            tickFormatter={(val) => {
                                const date = new Date(val);
                                return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
                            }}
                        />
                        <YAxis stroke="#64748b" />
                        <Tooltip
                            contentStyle={{
                                background: '#1a1a2e',
                                border: '1px solid #252542',
                                borderRadius: '8px'
                            }}
                            labelFormatter={(val) => new Date(val).toLocaleString()}
                        />
                        <Area type="monotone" dataKey="ERROR" stroke="#ef4444" fillOpacity={1} fill="url(#colorError)" />
                        <Area type="monotone" dataKey="WARN" stroke="#f59e0b" fillOpacity={1} fill="url(#colorWarn)" />
                        <Area type="monotone" dataKey="INFO" stroke="#3b82f6" fillOpacity={1} fill="url(#colorInfo)" />
                    </AreaChart>
                </ResponsiveContainer>
            </div>

            <div className="card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                    <h3>Recent Incidents</h3>
                    <Link to="/incidents" className="btn btn-secondary">View All</Link>
                </div>

                {recentIncidents.length === 0 ? (
                    <div className="empty-state">
                        <h3>No incidents yet</h3>
                        <p>Run the log producer to generate some data</p>
                    </div>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Service</th>
                                    <th>Type</th>
                                    <th>Status</th>
                                    <th>Time</th>
                                </tr>
                            </thead>
                            <tbody>
                                {recentIncidents.map(incident => (
                                    <tr key={incident.id}>
                                        <td>
                                            <Link to={`/incidents/${incident.id}`} style={{ color: '#a855f7' }}>
                                                #{incident.id}
                                            </Link>
                                        </td>
                                        <td>{incident.serviceName}</td>
                                        <td><span className="badge error">{incident.type}</span></td>
                                        <td>
                                            <span className={`badge ${incident.status.toLowerCase()}`}>
                                                {incident.status}
                                            </span>
                                        </td>
                                        <td style={{ color: '#94a3b8' }}>
                                            {new Date(incident.startTime).toLocaleString()}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}

export default Dashboard;
