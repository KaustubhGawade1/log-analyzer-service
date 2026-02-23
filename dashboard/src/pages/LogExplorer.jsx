import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { fetchLogs, fetchServices } from '../services/api';

function LogExplorer() {
    const [logs, setLogs] = useState([]);
    const [services, setServices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filters, setFilters] = useState({
        level: '',
        serviceName: '',
        search: '',
        limit: 100
    });

    useEffect(() => {
        fetchServices().then(setServices).catch(console.error);
    }, []);

    useEffect(() => {
        async function loadLogs() {
            try {
                setLoading(true);
                const data = await fetchLogs(filters);
                setLogs(data);
            } catch (err) {
                console.error('Failed to load logs:', err);
            } finally {
                setLoading(false);
            }
        }
        loadLogs();
    }, [filters]);

    const handleSearch = (e) => {
        e.preventDefault();
    };

    return (
        <div>
            <header className="page-header">
                <h1>Log Explorer</h1>
                <p>Search and filter through application logs ingested via Kafka</p>
            </header>

            <form onSubmit={handleSearch} className="input-group">
                <input
                    type="text"
                    className="search-input"
                    placeholder="Search logs..."
                    value={filters.search}
                    onChange={(e) => setFilters({ ...filters, search: e.target.value })}
                />

                <select
                    value={filters.level}
                    onChange={(e) => setFilters({ ...filters, level: e.target.value })}
                >
                    <option value="">All Levels</option>
                    <option value="ERROR">ERROR</option>
                    <option value="WARN">WARN</option>
                    <option value="INFO">INFO</option>
                    <option value="DEBUG">DEBUG</option>
                </select>

                <select
                    value={filters.serviceName}
                    onChange={(e) => setFilters({ ...filters, serviceName: e.target.value })}
                >
                    <option value="">All Services</option>
                    {services.map(service => (
                        <option key={service} value={service}>{service}</option>
                    ))}
                </select>

                <select
                    value={filters.limit}
                    onChange={(e) => setFilters({ ...filters, limit: parseInt(e.target.value) })}
                >
                    <option value="50">50 logs</option>
                    <option value="100">100 logs</option>
                    <option value="200">200 logs</option>
                    <option value="500">500 logs</option>
                </select>
            </form>

            {loading ? (
                <div className="loading">
                    <div className="spinner"></div>
                </div>
            ) : logs.length === 0 ? (
                <div className="empty-state">
                    <h3>No logs found</h3>
                    <p>Try adjusting your filters or run the log producer</p>
                </div>
            ) : (
                <div className="card">
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th style={{ width: '180px' }}>Timestamp</th>
                                    <th style={{ width: '80px' }}>Level</th>
                                    <th style={{ width: '140px' }}>Service</th>
                                    <th>Message</th>
                                    <th style={{ width: '120px' }}>Trace ID</th>
                                    <th style={{ width: '90px' }}>Source</th>
                                </tr>
                            </thead>
                            <tbody>
                                {logs.map((log, index) => (
                                    <tr key={log.id || index}>
                                        <td style={{ color: '#94a3b8', fontFamily: 'monospace', fontSize: '0.75rem' }}>
                                            {new Date(log.timestamp).toLocaleString()}
                                        </td>
                                        <td>
                                            <span className={`badge ${log.level?.toLowerCase()}`}>
                                                {log.level}
                                            </span>
                                        </td>
                                        <td>{log.serviceName}</td>
                                        <td style={{
                                            fontFamily: 'monospace',
                                            fontSize: '0.8rem',
                                            maxWidth: '400px',
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                            whiteSpace: 'nowrap'
                                        }}>
                                            {log.message}
                                        </td>
                                        <td>
                                            {log.traceId ? (
                                                <Link
                                                    to="/flows"
                                                    className="trace-link"
                                                    title={`View trace: ${log.traceId}`}
                                                >
                                                    🔗 {log.traceId.substring(0, 8)}…
                                                </Link>
                                            ) : (
                                                <span style={{ color: '#4a4a6a', fontSize: '0.75rem' }}>—</span>
                                            )}
                                        </td>
                                        <td>
                                            {log.source === 'microservices-demo' ? (
                                                <span className="source-badge demo">🔀 Demo</span>
                                            ) : (
                                                <span className="source-badge sim">📨 Sim</span>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    <div style={{ marginTop: '16px', color: '#94a3b8', fontSize: '0.875rem', display: 'flex', justifyContent: 'space-between' }}>
                        <span>Showing {logs.length} logs</span>
                        <span>
                            {logs.filter(l => l.traceId).length} with trace context
                        </span>
                    </div>
                </div>
            )}
        </div>
    );
}

export default LogExplorer;
