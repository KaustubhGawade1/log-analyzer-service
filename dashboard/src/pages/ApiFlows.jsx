import { useState, useEffect, useCallback } from 'react';
import FlowGraph from '../components/graph/FlowGraph';
import {
    fetchTraces,
    fetchFlow,
    fetchFlowServices,
    fetchFlowStats,
    explainFlow
} from '../services/api';
import './ApiFlows.css';

export default function ApiFlows() {
    const [traces, setTraces] = useState([]);
    const [selectedTrace, setSelectedTrace] = useState(null);
    const [flowGraph, setFlowGraph] = useState(null);
    const [services, setServices] = useState([]);
    const [stats, setStats] = useState(null);
    const [explanation, setExplanation] = useState(null);
    const [loading, setLoading] = useState(true);
    const [loadingFlow, setLoadingFlow] = useState(false);
    const [loadingExplanation, setLoadingExplanation] = useState(false);

    const [filters, setFilters] = useState({
        serviceName: '',
        timeRange: '1h',
    });

    const timeRangeToMs = {
        '15m': 15 * 60 * 1000,
        '1h': 60 * 60 * 1000,
        '6h': 6 * 60 * 60 * 1000,
        '24h': 24 * 60 * 60 * 1000,
    };

    useEffect(() => {
        loadData();
    }, [filters]);

    const loadData = async () => {
        setLoading(true);
        try {
            const [tracesData, servicesData, statsData] = await Promise.all([
                fetchTraces({
                    serviceName: filters.serviceName,
                    limit: 50,
                    lookbackMs: timeRangeToMs[filters.timeRange],
                }),
                fetchFlowServices(),
                fetchFlowStats(timeRangeToMs[filters.timeRange]),
            ]);
            setTraces(tracesData);
            setServices(servicesData);
            setStats(statsData);
        } catch (error) {
            console.error('Failed to load flow data:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleTraceSelect = async (trace) => {
        setSelectedTrace(trace);
        setExplanation(null);
        setLoadingFlow(true);
        try {
            const graph = await fetchFlow(trace.traceId);
            setFlowGraph(graph);
        } catch (error) {
            console.error('Failed to load flow:', error);
        } finally {
            setLoadingFlow(false);
        }
    };

    const handleExplain = async () => {
        if (!selectedTrace) return;
        setLoadingExplanation(true);
        try {
            const result = await explainFlow(selectedTrace.traceId);
            setExplanation(result.explanation);
        } catch (error) {
            console.error('Failed to get explanation:', error);
        } finally {
            setLoadingExplanation(false);
        }
    };

    const getStatusColor = (status) => {
        switch (status) {
            case 'FAILURE': return 'status-failure';
            case 'PARTIAL_FAILURE': return 'status-partial';
            default: return 'status-success';
        }
    };

    return (
        <div className="api-flows-page">
            <header className="page-header">
                <div className="header-left">
                    <h1>🔀 API Flow Explorer</h1>
                    <p className="subtitle">Visualize API call graphs from distributed traces</p>
                </div>
                {stats && (
                    <div className="header-stats">
                        <div className="stat-item">
                            <span className="stat-value">{stats.totalFlows}</span>
                            <span className="stat-label">Traces</span>
                        </div>
                        <div className="stat-item">
                            <span className="stat-value success">{stats.successfulFlows}</span>
                            <span className="stat-label">Successful</span>
                        </div>
                        <div className="stat-item">
                            <span className="stat-value error">{stats.failedFlows}</span>
                            <span className="stat-label">Failed</span>
                        </div>
                        <div className="stat-item">
                            <span className="stat-value">{stats.serviceCount}</span>
                            <span className="stat-label">Services</span>
                        </div>
                    </div>
                )}
            </header>

            <div className="filters-bar">
                <div className="filter-group">
                    <label>Service</label>
                    <select
                        value={filters.serviceName}
                        onChange={(e) => setFilters({ ...filters, serviceName: e.target.value })}
                    >
                        <option value="">All Services</option>
                        {services.map((service) => (
                            <option key={service} value={service}>{service}</option>
                        ))}
                    </select>
                </div>
                <div className="filter-group">
                    <label>Time Range</label>
                    <select
                        value={filters.timeRange}
                        onChange={(e) => setFilters({ ...filters, timeRange: e.target.value })}
                    >
                        <option value="15m">Last 15 min</option>
                        <option value="1h">Last 1 hour</option>
                        <option value="6h">Last 6 hours</option>
                        <option value="24h">Last 24 hours</option>
                    </select>
                </div>
                <button className="refresh-btn" onClick={loadData}>
                    🔄 Refresh
                </button>
            </div>

            <div className="flows-layout">
                {/* Trace List */}
                <aside className="trace-list-panel">
                    <h3>Recent Traces</h3>
                    {loading ? (
                        <div className="loading">Loading traces...</div>
                    ) : traces.length === 0 ? (
                        <div className="empty-state">No traces found</div>
                    ) : (
                        <ul className="trace-list">
                            {traces.map((trace) => (
                                <li
                                    key={trace.traceId}
                                    className={`trace-item ${selectedTrace?.traceId === trace.traceId ? 'selected' : ''}`}
                                    onClick={() => handleTraceSelect(trace)}
                                >
                                    <div className="trace-header">
                                        <span className={`status-dot ${getStatusColor(trace.status)}`}></span>
                                        <span className="trace-service">{trace.rootService}</span>
                                        {trace.hasBottleneck && <span className="bottleneck-badge">🔥</span>}
                                    </div>
                                    <div className="trace-endpoint">{trace.rootEndpoint || 'N/A'}</div>
                                    <div className="trace-meta">
                                        <span>⏱ {trace.durationMs}ms</span>
                                        <span>📦 {trace.nodeCount} nodes</span>
                                    </div>
                                    <div className="trace-time">
                                        {new Date(trace.startTime).toLocaleTimeString()}
                                    </div>
                                </li>
                            ))}
                        </ul>
                    )}
                </aside>

                {/* Graph View */}
                <main className="graph-panel">
                    {loadingFlow ? (
                        <div className="loading-overlay">
                            <div className="spinner"></div>
                            <span>Loading flow graph...</span>
                        </div>
                    ) : flowGraph ? (
                        <FlowGraph
                            graphData={flowGraph}
                            onNodeClick={(node) => console.log('Node clicked:', node)}
                            onEdgeClick={(edge) => console.log('Edge clicked:', edge)}
                        />
                    ) : (
                        <div className="empty-graph">
                            <span className="icon">🔀</span>
                            <h3>Select a trace to visualize</h3>
                            <p>Choose a trace from the list to see the API call flow</p>
                        </div>
                    )}
                </main>

                {/* Details Panel */}
                <aside className="details-panel">
                    {selectedTrace && (
                        <>
                            <div className="detail-section">
                                <h3>Flow Details</h3>
                                <dl className="detail-list">
                                    <dt>Trace ID</dt>
                                    <dd className="mono">{selectedTrace.traceId}</dd>
                                    <dt>Root Service</dt>
                                    <dd>{selectedTrace.rootService}</dd>
                                    <dt>Duration</dt>
                                    <dd>{selectedTrace.durationMs}ms</dd>
                                    <dt>Status</dt>
                                    <dd className={getStatusColor(selectedTrace.status)}>
                                        {selectedTrace.status}
                                    </dd>
                                    <dt>Nodes</dt>
                                    <dd>{selectedTrace.nodeCount}</dd>
                                </dl>
                            </div>

                            <div className="detail-section">
                                <h3>AI Analysis</h3>
                                <button
                                    className="analyze-btn"
                                    onClick={handleExplain}
                                    disabled={loadingExplanation}
                                >
                                    {loadingExplanation ? '🔄 Analyzing...' : '🧠 Generate Explanation'}
                                </button>

                                {explanation && (
                                    <div className="ai-explanation">
                                        <div className="explanation-summary">
                                            <h4>Summary</h4>
                                            <p>{explanation.summary}</p>
                                        </div>

                                        {explanation.bottleneckService && (
                                            <div className="explanation-bottleneck">
                                                <h4>Bottleneck</h4>
                                                <span className="bottleneck-service">{explanation.bottleneckService}</span>
                                            </div>
                                        )}

                                        {explanation.rootCause && (
                                            <div className="explanation-cause">
                                                <h4>Root Cause</h4>
                                                <p>{explanation.rootCause}</p>
                                            </div>
                                        )}

                                        {explanation.recommendations?.length > 0 && (
                                            <div className="explanation-recommendations">
                                                <h4>Recommendations</h4>
                                                <ul>
                                                    {explanation.recommendations.map((rec, i) => (
                                                        <li key={i}>{rec}</li>
                                                    ))}
                                                </ul>
                                            </div>
                                        )}

                                        {explanation.estimatedImpact && (
                                            <div className="explanation-impact">
                                                <span className={`impact-badge impact-${explanation.estimatedImpact}`}>
                                                    Impact: {explanation.estimatedImpact}
                                                </span>
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>
                        </>
                    )}

                    {!selectedTrace && (
                        <div className="empty-details">
                            <p>Select a trace to see details</p>
                        </div>
                    )}
                </aside>
            </div>
        </div>
    );
}
