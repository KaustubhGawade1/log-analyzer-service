const API_BASE = 'http://localhost:8080/api';

// Existing API functions
export async function fetchIncidents(filters = {}) {
    const params = new URLSearchParams();
    if (filters.status) params.append('status', filters.status);
    if (filters.serviceName) params.append('serviceName', filters.serviceName);
    if (filters.type) params.append('type', filters.type);

    const response = await fetch(`${API_BASE}/incidents?${params}`);
    return response.json();
}

export async function fetchIncident(id) {
    const response = await fetch(`${API_BASE}/incidents/${id}`);
    return response.json();
}

export async function fetchIncidentStats() {
    const response = await fetch(`${API_BASE}/incidents/stats`);
    return response.json();
}

export async function fetchLogs(filters = {}) {
    const params = new URLSearchParams();
    if (filters.level) params.append('level', filters.level);
    if (filters.serviceName) params.append('serviceName', filters.serviceName);
    if (filters.search) params.append('search', filters.search);
    params.append('limit', filters.limit || 100);

    const response = await fetch(`${API_BASE}/logs?${params}`);
    return response.json();
}

export async function fetchLogClusters() {
    const response = await fetch(`${API_BASE}/logs/clusters`);
    return response.json();
}

// Alias for backward compatibility with Clusters.jsx
export const fetchClusters = fetchLogClusters;

export async function fetchLogTimeline(minutes = 60) {
    const response = await fetch(`${API_BASE}/logs/timeline?minutes=${minutes}`);
    return response.json();
}

// Alias for backward compatibility with Dashboard
export const fetchTimeline = fetchLogTimeline;

export async function fetchServices() {
    const response = await fetch(`${API_BASE}/services`);
    return response.json();
}

export async function analyzeIncident(id) {
    const response = await fetch(`${API_BASE}/incidents/${id}/analyze`, {
        method: 'POST',
    });
    return response.json();
}

// ========== NEW: Flow API Functions ==========

/**
 * Fetch all services from Zipkin
 */
export async function fetchFlowServices() {
    const response = await fetch(`${API_BASE}/flows/services`);
    return response.json();
}

/**
 * Fetch service dependency graph
 */
export async function fetchDependencies(lookbackMs = 3600000) {
    const response = await fetch(`${API_BASE}/flows/dependencies?lookbackMs=${lookbackMs}`);
    return response.json();
}

/**
 * Fetch recent traces with optional filtering
 */
export async function fetchTraces(options = {}) {
    const params = new URLSearchParams();
    if (options.serviceName) params.append('serviceName', options.serviceName);
    params.append('limit', options.limit || 50);
    params.append('lookbackMs', options.lookbackMs || 3600000);

    const response = await fetch(`${API_BASE}/flows/traces?${params}`);
    return response.json();
}

/**
 * Fetch a specific flow by trace ID
 */
export async function fetchFlow(traceId) {
    const response = await fetch(`${API_BASE}/flows/${traceId}`);
    if (!response.ok) {
        throw new Error('Flow not found');
    }
    return response.json();
}

/**
 * Fetch flows with detected bottlenecks
 */
export async function fetchBottleneckFlows(limit = 20, lookbackMs = 3600000) {
    const response = await fetch(
        `${API_BASE}/flows/bottlenecks?limit=${limit}&lookbackMs=${lookbackMs}`
    );
    return response.json();
}

/**
 * Fetch bottleneck details for a specific flow
 */
export async function fetchFlowBottlenecks(traceId) {
    const response = await fetch(`${API_BASE}/flows/${traceId}/bottlenecks`);
    return response.json();
}

/**
 * Trigger AI explanation for a flow
 */
export async function explainFlow(traceId) {
    const response = await fetch(`${API_BASE}/flows/${traceId}/explain`, {
        method: 'POST',
    });
    return response.json();
}

/**
 * Fetch flow statistics
 */
export async function fetchFlowStats(lookbackMs = 3600000) {
    const response = await fetch(`${API_BASE}/flows/stats?lookbackMs=${lookbackMs}`);
    return response.json();
}

/**
 * Fetch complete topology for visualization
 */
export async function fetchTopology(lookbackMs = 3600000) {
    const response = await fetch(`${API_BASE}/flows/topology?lookbackMs=${lookbackMs}`);
    return response.json();
}
