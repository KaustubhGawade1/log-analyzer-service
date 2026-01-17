import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { fetchIncident, analyzeIncident } from '../services/api';

function IncidentDetail() {
    const { id } = useParams();
    const [incident, setIncident] = useState(null);
    const [analysis, setAnalysis] = useState(null);
    const [loading, setLoading] = useState(true);
    const [analyzing, setAnalyzing] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        async function loadIncident() {
            try {
                setLoading(true);
                const data = await fetchIncident(id);
                setIncident(data);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        }
        loadIncident();
    }, [id]);

    const handleAnalyze = async () => {
        try {
            setAnalyzing(true);
            const result = await analyzeIncident(id);
            setAnalysis(result);
        } catch (err) {
            alert('Failed to analyze incident. Make sure OPENAI_API_KEY is set.');
        } finally {
            setAnalyzing(false);
        }
    };

    if (loading) {
        return (
            <div className="loading">
                <div className="spinner"></div>
            </div>
        );
    }

    if (error || !incident) {
        return (
            <div className="empty-state">
                <h3>Incident not found</h3>
                <Link to="/incidents" className="btn btn-primary" style={{ marginTop: '16px' }}>
                    Back to Incidents
                </Link>
            </div>
        );
    }

    return (
        <div className="incident-detail">
            <Link to="/incidents" style={{ color: '#94a3b8', marginBottom: '24px', display: 'inline-block' }}>
                ← Back to Incidents
            </Link>

            <header className="page-header">
                <div className="incident-header">
                    <h1>Incident #{incident.id}</h1>
                    <span className={`badge ${incident.status.toLowerCase()}`}>
                        {incident.status}
                    </span>
                </div>
                <p>{incident.description}</p>
            </header>

            <div className="incident-meta">
                <div className="meta-item">
                    <div className="meta-label">Service</div>
                    <div className="meta-value">{incident.serviceName}</div>
                </div>
                <div className="meta-item">
                    <div className="meta-label">Type</div>
                    <div className="meta-value">{incident.type.replace('_', ' ')}</div>
                </div>
                <div className="meta-item">
                    <div className="meta-label">Start Time</div>
                    <div className="meta-value">{new Date(incident.startTime).toLocaleString()}</div>
                </div>
                <div className="meta-item">
                    <div className="meta-label">End Time</div>
                    <div className="meta-value">
                        {incident.endTime ? new Date(incident.endTime).toLocaleString() : 'Ongoing'}
                    </div>
                </div>
            </div>

            {!analysis ? (
                <button
                    className="btn btn-primary"
                    onClick={handleAnalyze}
                    disabled={analyzing}
                    style={{ marginBottom: '24px' }}
                >
                    {analyzing ? (
                        <>
                            <div className="spinner" style={{ width: '16px', height: '16px', borderWidth: '2px' }}></div>
                            Analyzing...
                        </>
                    ) : (
                        <>
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M12 2a10 10 0 1 0 10 10H12V2z" />
                                <path d="M21.18 8.02c-1-2.3-2.85-4.17-5.16-5.18" />
                            </svg>
                            Generate AI Root Cause Analysis
                        </>
                    )}
                </button>
            ) : (
                <div className="rca-report">
                    <div className="rca-header">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#a855f7" strokeWidth="2">
                            <circle cx="12" cy="12" r="10" />
                            <path d="M12 16v-4" />
                            <path d="M12 8h.01" />
                        </svg>
                        <h3>AI Root Cause Analysis</h3>
                    </div>

                    <div className="rca-section">
                        <h4>Summary</h4>
                        <p>{analysis.summary}</p>
                    </div>

                    <div className="rca-section">
                        <h4>Probable Root Cause</h4>
                        <p style={{
                            padding: '16px',
                            background: 'var(--bg-tertiary)',
                            borderRadius: '8px',
                            borderLeft: '4px solid #ef4444'
                        }}>
                            {analysis.probableRootCause}
                        </p>
                    </div>

                    <div className="rca-section">
                        <h4>Recommended Actions</h4>
                        <ul className="rca-actions">
                            {analysis.recommendedActions.map((action, index) => (
                                <li key={index}>{action}</li>
                            ))}
                        </ul>
                    </div>

                    <div className="rca-section">
                        <h4>Confidence Score</h4>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                            <span style={{ fontWeight: 700, fontSize: '1.25rem', color: '#a855f7' }}>
                                {Math.round(analysis.confidenceScore * 100)}%
                            </span>
                            <div className="confidence-bar" style={{ flex: 1 }}>
                                <div
                                    className="confidence-fill"
                                    style={{ width: `${analysis.confidenceScore * 100}%` }}
                                ></div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default IncidentDetail;
