import { useState, useEffect } from 'react';
import { fetchClusters } from '../services/api';

function Clusters() {
    const [clusters, setClusters] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function loadClusters() {
            try {
                setLoading(true);
                const data = await fetchClusters();
                setClusters(data);
            } catch (err) {
                console.error('Failed to load clusters:', err);
            } finally {
                setLoading(false);
            }
        }
        loadClusters();
    }, []);

    return (
        <div>
            <header className="page-header">
                <h1>Error Clusters</h1>
                <p>Grouped error patterns by stack trace signature</p>
            </header>

            {loading ? (
                <div className="loading">
                    <div className="spinner"></div>
                </div>
            ) : clusters.length === 0 ? (
                <div className="empty-state">
                    <h3>No error clusters found</h3>
                    <p>Error logs with similar patterns will be grouped here</p>
                </div>
            ) : (
                <div>
                    <p style={{ marginBottom: '24px', color: '#94a3b8' }}>
                        Found {clusters.length} unique error patterns
                    </p>

                    {clusters.map((cluster, index) => (
                        <div key={cluster.clusterId} className="cluster-card">
                            <div className="cluster-header">
                                <div>
                                    <span style={{ color: '#94a3b8', fontSize: '0.75rem' }}>Cluster #{index + 1}</span>
                                    <div style={{ fontFamily: 'monospace', fontSize: '0.8rem', marginTop: '4px', color: '#64748b' }}>
                                        {cluster.clusterId}
                                    </div>
                                </div>
                                <div className="cluster-count">
                                    {cluster.count} <span style={{ fontSize: '0.875rem', color: '#94a3b8' }}>occurrences</span>
                                </div>
                            </div>

                            <div className="cluster-message">
                                {cluster.sample?.message || 'No sample message'}
                            </div>

                            <div style={{ marginTop: '12px', display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                                {cluster.services?.map(service => (
                                    <span
                                        key={service}
                                        style={{
                                            background: 'var(--bg-tertiary)',
                                            padding: '4px 12px',
                                            borderRadius: '16px',
                                            fontSize: '0.75rem',
                                            color: '#94a3b8'
                                        }}
                                    >
                                        {service}
                                    </span>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

export default Clusters;
